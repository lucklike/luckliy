package com.luckyframework.idea.spel.symbol

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.luckyframework.idea.spel.documentation.docCommentOf
import com.luckyframework.idea.spel.documentation.summaryOf
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * `@SpELImport` 展开结果。
 *
 * [symbols] 是正常可用符号；[disabled] 是静态 `false` 条件导入未生效的类
 * 提供的符号，只用于诊断（报「类未被导入」错误），不参与补全与解析。
 */
data class ImportedSymbols(
    val symbols: List<SpelSymbol> = emptyList(),
    val disabled: List<SpelSymbol> = emptyList()
) {
    operator fun plus(other: ImportedSymbols): ImportedSymbols =
        ImportedSymbols(symbols + other.symbols, disabled + other.disabled)
}

/**
 * 把 `@SpELImport` 导入的类展开为函数与命名空间符号。
 *
 * 复刻 `ClassStaticElement` 的公开规则：只取 public static 方法，跳过
 * `@FunctionFilter`，按 `@FunctionAlias` 改名；类上有 `@Namespace` 时，
 * 其函数归入该命名空间（`#ns.func`）。
 *
 * 来自 `ifs` 条件导入且 condition 无法静态判定的类，符号标记为
 * [SpelSymbol.conditional]，检查器据此给出警告而不是未解析错误；
 * condition 静态为 `false` 的类不参与解析，其符号只放进
 * [ImportedSymbols.disabled] 供诊断使用。
 */
class ImportedClassSymbols(private val importParser: SpelImportParser = SpelImportParser()) {

    fun collect(owner: PsiModifierListOwner, origin: SpelSymbolOrigin): ImportedSymbols {
        val available = LinkedHashMap<String, AvailableClass>()
        val disabled = LinkedHashMap<String, PsiClass>()
        for (annotation in importParser.findImportAnnotations(owner)) {
            for (psiClass in importParser.parseClasses(annotation)) {
                addAvailable(available, psiClass, conditionUnknown = false)
            }
            for (imported in importParser.parseConditionalClasses(annotation)) {
                when (imported.condition) {
                    false -> disabled.putIfAbsent(keyOf(imported.psiClass), imported.psiClass)
                    else -> addAvailable(available, imported.psiClass, imported.condition == null)
                }
            }
        }

        var result = ImportedSymbols()
        for (entry in available.values) {
            result += ImportedSymbols(classSymbols(entry.psiClass, origin, entry.conditionUnknown))
        }
        for ((key, psiClass) in disabled) {
            // 同一个类只要有确定导入来源，就不再视为未导入
            if (available.containsKey(key)) {
                continue
            }
            result += ImportedSymbols(disabled = classSymbols(psiClass, origin))
        }
        return result
    }

    private fun addAvailable(
        target: MutableMap<String, AvailableClass>,
        psiClass: PsiClass,
        conditionUnknown: Boolean
    ) {
        val key = keyOf(psiClass)
        val existing = target[key]
        target[key] = if (existing == null) {
            AvailableClass(psiClass, conditionUnknown)
        } else {
            AvailableClass(existing.psiClass, existing.conditionUnknown && conditionUnknown)
        }
    }

    private fun keyOf(psiClass: PsiClass): String =
        psiClass.qualifiedName ?: psiClass.name ?: psiClass.text

    private data class AvailableClass(val psiClass: PsiClass, val conditionUnknown: Boolean)

    /** 展开单个已解析类的函数与命名空间符号（YAML 配置侧复用）。 */
    internal fun symbolsOf(psiClass: PsiClass, origin: SpelSymbolOrigin): List<SpelSymbol> =
        classSymbols(psiClass, origin)

    private fun classSymbols(
        psiClass: PsiClass,
        origin: SpelSymbolOrigin,
        conditional: Boolean = false
    ): List<SpelSymbol> {
        val namespace = namespaceOf(psiClass)
        val result = ArrayList<SpelSymbol>()
        if (namespace != null) {
            result += SpelSymbol(
                name = namespace,
                kind = SpelSymbolKind.NAMESPACE,
                type = psiClass.qualifiedName,
                origin = origin,
                declaration = psiClass,
                owner = psiClass.qualifiedName,
                conditional = conditional
            )
        }
        for (method in psiClass.allMethods) {
            if (method.isConstructor) {
                continue
            }
            if (!method.hasModifierProperty(PsiModifier.PUBLIC) ||
                !method.hasModifierProperty(PsiModifier.STATIC)
            ) {
                continue
            }
            if (isFiltered(method)) {
                continue
            }
            val alias = functionAliasOf(method)
            val name = alias ?: method.name
            result += SpelSymbol(
                name = name,
                kind = SpelSymbolKind.FUNCTION,
                type = method.returnType?.presentableText,
                origin = origin,
                declaration = method,
                lifecycle = SpelLifecycle.REQUEST_INIT,
                signature = signatureOf(method, name),
                namespace = namespace,
                owner = psiClass.qualifiedName,
                description = alias?.let { "别名：$it（原方法 ${method.name}）" }
                    ?: summaryOf(docCommentOf(method), DESCRIPTION_LIMIT),
                conditional = conditional
            )
        }
        return result
    }

    private fun namespaceOf(psiClass: PsiClass): String? {
        // @Namespace 带 @Inherited，运行期 findMergedAnnotation 会沿父类/接口查找
        for (owner in classHierarchy(psiClass)) {
            mergedNamespace(owner)?.let { return it }
        }
        return null
    }

    private fun mergedNamespace(owner: PsiModifierListOwner): String? {
        val annotations = owner.modifierList?.annotations ?: return null
        annotations.firstOrNull { it.qualifiedName == NAMESPACE }?.let { annotation ->
            readStringAttribute(annotation, "value")?.let { return it }
        }
        for (annotation in annotations) {
            mergedNamespaceValue(annotation, HashSet(), 0)?.let { return it }
        }
        return null
    }

    /**
     * 计算注解（含元注解）合并后的 `@Namespace#value`：
     * `@AliasFor` 别名链与元注解同名属性的隐式别名都视为有效来源。
     */
    private fun mergedNamespaceValue(
        annotation: PsiAnnotation,
        visited: MutableSet<String>,
        depth: Int
    ): String? {
        val typeName = annotation.qualifiedName ?: return null
        val declaration = annotation.resolveAnnotationType() ?: return null
        if (depth > MAX_DEPTH || !visited.add(typeName)) {
            return null
        }
        if (typeName == NAMESPACE) {
            return readStringAttribute(annotation, "value")
        }
        for (pair in annotation.parameterList.attributes) {
            val attribute = pair.name ?: DEFAULT_ATTRIBUTE
            if (resolveNamespaceAttribute(declaration, attribute, HashSet(), 0) != "value") {
                continue
            }
            readStringAttribute(annotation, attribute)?.let { return it }
        }
        for (meta in declaration.modifierList?.annotations ?: return null) {
            mergedNamespaceValue(meta, visited, depth + 1)?.let { return it }
        }
        return null
    }

    /**
     * 沿别名图找到 `@Namespace#value`，找不到返回 null。
     * 显式 `@AliasFor` 优先，否则按元注解同名属性继续向上查找。
     */
    private fun resolveNamespaceAttribute(
        declaration: PsiClass,
        attribute: String,
        visited: MutableSet<String>,
        depth: Int
    ): String? {
        if (depth > MAX_DEPTH) {
            return null
        }
        val key = "${declaration.qualifiedName}#$attribute"
        if (!visited.add(key)) {
            return null
        }
        val method = declaration.findMethodsByName(attribute, false).firstOrNull() ?: return null
        if (declaration.qualifiedName == NAMESPACE) {
            return attribute
        }
        findAliasFor(method)?.let { alias ->
            val targetAnnotation = readClassAttribute(alias, "annotation")
                ?.takeUnless { it.qualifiedName == JAVA_LANG_ANNOTATION }
            val targetAttribute = readStringAttribute(alias, "attribute")?.takeIf { it.isNotBlank() }
                ?: readStringAttribute(alias, "value")?.takeIf { it.isNotBlank() }
                ?: attribute
            return if (targetAnnotation == null) {
                resolveNamespaceAttribute(declaration, targetAttribute, visited, depth + 1)
            } else {
                resolveNamespaceAttribute(targetAnnotation, targetAttribute, visited, depth + 1)
            }
        }
        for (meta in declaration.modifierList?.annotations ?: return null) {
            val metaFqName = meta.qualifiedName ?: continue
            if (metaFqName.startsWith("java.lang.annotation.")) {
                continue
            }
            val metaDeclaration = meta.resolveAnnotationType() ?: continue
            resolveNamespaceAttribute(metaDeclaration, attribute, visited, depth + 1)?.let { return it }
        }
        return null
    }

    /** `@FunctionAlias` 的 `@Target` 只有 METHOD，不存在元注解场景。 */
    private fun functionAliasOf(method: PsiMethod): String? {
        readStringAttribute(method.getAnnotation(FUNCTION_ALIAS), "value")?.let { return it }
        for (annotation in method.modifierList?.annotations ?: return null) {
            val merged = annotation.resolveAnnotationType()?.getAnnotation(FUNCTION_ALIAS) ?: continue
            readStringAttribute(merged, "value")?.let { return it }
        }
        return null
    }

    private fun isFiltered(method: PsiMethod): Boolean =
        isAnnotatedWith(method, FUNCTION_FILTER, HashSet(), 0)

    /** 对齐 `AnnotationUtils.isAnnotated`：沿元注解链递归检查注解存在性。 */
    private fun isAnnotatedWith(
        owner: PsiModifierListOwner,
        fqName: String,
        visited: MutableSet<String>,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) {
            return false
        }
        val annotations = owner.modifierList?.annotations ?: return false
        if (annotations.any { it.qualifiedName == fqName }) {
            return true
        }
        for (annotation in annotations) {
            val declaration = annotation.resolveAnnotationType() ?: continue
            val declarationName = declaration.qualifiedName ?: continue
            if (!visited.add(declarationName)) {
                continue
            }
            if (isAnnotatedWith(declaration, fqName, visited, depth + 1)) {
                return true
            }
        }
        return false
    }

    private fun readStringAttribute(annotation: PsiAnnotation?, name: String): String? {
        if (annotation == null) {
            return null
        }
        val value = (annotation.findAttributeValue(name) as? PsiLiteralExpression)?.value as? String
        return value?.takeIf { it.isNotBlank() }
    }

    private fun readClassAttribute(annotation: PsiAnnotation, name: String): PsiClass? {
        val value = annotation.findAttributeValue(name) as? PsiClassObjectAccessExpression ?: return null
        val classType = value.operand.type as? PsiClassType ?: return null
        return classType.resolve()
    }

    private fun findAliasFor(owner: PsiModifierListOwner): PsiAnnotation? {
        owner.getAnnotation(ALIAS_FOR)?.let { return it }
        // spring-core 不在 classpath 时 FQ 名无法解析，退化为按简单名匹配
        return owner.modifierList?.annotations?.firstOrNull {
            it.qualifiedName == ALIAS_FOR || it.nameReferenceElement?.referenceName == "AliasFor"
        }
    }

    private fun classHierarchy(psiClass: PsiClass): List<PsiClass> {
        val result = ArrayList<PsiClass>()
        val visited = HashSet<String>()
        fun visit(current: PsiClass?) {
            if (current == null) {
                return
            }
            val key = current.qualifiedName ?: current.name ?: return
            if (!visited.add(key)) {
                return
            }
            result += current
            visit(current.superClass)
            current.interfaces.forEach { visit(it) }
        }
        visit(psiClass)
        return result
    }

    /** 签名用可输入的名字（有别名时用别名），与列表展示、前缀过滤保持一致。 */
    private fun signatureOf(method: PsiMethod, name: String): String {
        val parameters = method.parameterList.parameters.joinToString(", ") { it.type.presentableText }
        return "$name($parameters)"
    }

    companion object {
        private const val NAMESPACE = "com.luckyframework.httpclient.proxy.spel.Namespace"
        private const val FUNCTION_ALIAS = "com.luckyframework.httpclient.proxy.spel.FunctionAlias"
        private const val FUNCTION_FILTER = "com.luckyframework.httpclient.proxy.spel.FunctionFilter"
        private const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"
        private const val JAVA_LANG_ANNOTATION = "java.lang.annotation.Annotation"
        private const val DEFAULT_ATTRIBUTE = "value"
        private const val MAX_DEPTH = 16
        private const val DESCRIPTION_LIMIT = 80
    }
}
