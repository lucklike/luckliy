package com.luckyframework.idea.spel.symbol

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 把 `@SpELImport` 导入的类展开为函数与命名空间符号。
 *
 * 复刻 `ClassStaticElement` 的公开规则：只取 public static 方法，跳过
 * `@FunctionFilter`，按 `@FunctionAlias` 改名；类上有 `@Namespace` 时，
 * 其函数归入该命名空间（`#ns.func`）。
 */
class ImportedClassSymbols(private val importParser: SpelImportParser = SpelImportParser()) {

    fun collect(owner: PsiModifierListOwner, origin: SpelSymbolOrigin): List<SpelSymbol> {
        val result = ArrayList<SpelSymbol>()
        val visited = HashSet<String>()
        for (annotation in importParser.findImportAnnotations(owner)) {
            for (psiClass in importParser.parseClasses(annotation)) {
                if (visited.add(psiClass.qualifiedName ?: psiClass.name ?: continue)) {
                    result += classSymbols(psiClass, origin)
                }
            }
            for (psiClass in importParser.parseConditionalClasses(annotation)) {
                if (visited.add(psiClass.qualifiedName ?: psiClass.name ?: continue)) {
                    result += classSymbols(psiClass, origin)
                }
            }
        }
        return result
    }

    /** 展开单个已解析类的函数与命名空间符号（YAML 配置侧复用）。 */
    internal fun symbolsOf(psiClass: PsiClass, origin: SpelSymbolOrigin): List<SpelSymbol> =
        classSymbols(psiClass, origin)

    private fun classSymbols(psiClass: PsiClass, origin: SpelSymbolOrigin): List<SpelSymbol> {
        val namespace = namespaceOf(psiClass)
        val result = ArrayList<SpelSymbol>()
        if (namespace != null) {
            result += SpelSymbol(
                name = namespace,
                kind = SpelSymbolKind.NAMESPACE,
                type = psiClass.qualifiedName,
                origin = origin,
                declaration = psiClass,
                owner = psiClass.qualifiedName
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
            )
        }
        return result
    }

    private fun namespaceOf(psiClass: PsiClass): String? {
        readStringAttribute(psiClass.getAnnotation(NAMESPACE), "value")?.let { return it }
        // @Namespace 支持出现在元注解上
        for (annotation in psiClass.modifierList?.annotations ?: return null) {
            val merged = annotation.resolveAnnotationType()?.getAnnotation(NAMESPACE) ?: continue
            readStringAttribute(merged, "value")?.let { return it }
        }
        return null
    }

    private fun functionAliasOf(method: PsiMethod): String? {
        readStringAttribute(method.getAnnotation(FUNCTION_ALIAS), "value")?.let { return it }
        for (annotation in method.modifierList?.annotations ?: return null) {
            val merged = annotation.resolveAnnotationType()?.getAnnotation(FUNCTION_ALIAS) ?: continue
            readStringAttribute(merged, "value")?.let { return it }
        }
        return null
    }

    private fun isFiltered(method: PsiMethod): Boolean {
        if (method.getAnnotation(FUNCTION_FILTER) != null) {
            return true
        }
        return method.modifierList?.annotations?.any { annotation ->
            annotation.resolveAnnotationType()?.getAnnotation(FUNCTION_FILTER) != null
        } == true
    }

    private fun readStringAttribute(annotation: PsiAnnotation?, name: String): String? {
        if (annotation == null) {
            return null
        }
        val value = (annotation.findAttributeValue(name) as? PsiLiteralExpression)?.value as? String
        return value?.takeIf { it.isNotBlank() }
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
    }
}
