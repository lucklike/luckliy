package com.luckyframework.idea.spel.metadata

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.search.GlobalSearchScope

/**
 * 注解上的一个 SpEL 属性解析结果。
 *
 * [declaredAnnotationFqName]/[declaredAttribute] 是源码中实际书写的注解与属性，
 * [effectiveAnnotationFqName]/[effectiveAttribute] 是元数据表中登记的目标属性；
 * 组合注解与 `@AliasFor` 会让两者不同（例如 `@Get#value` → `@HttpRequest#url`）。
 */
data class ResolvedSpelAttribute(
    val declaredAnnotationFqName: String,
    val declaredAttribute: String,
    val effectiveAnnotationFqName: String,
    val effectiveAttribute: String,
    val metadata: SpelAttributeMetadata
) {
    val kind: SpelValueKind get() = metadata.kind
    val lifecycle: SpelLifecycle get() = metadata.lifecycle

    /** 使用处生效的字面量分隔符；空表示按 `=` 处理。 */
    val separator: String get() = metadata.separator

    val isAlias: Boolean
        get() = declaredAnnotationFqName != effectiveAnnotationFqName || declaredAttribute != effectiveAttribute
}

/**
 * 把 PSI 注解解析到注解元数据。
 *
 * 元数据全部来自注解声明上的 [SpelMetaAnnotationReader]（`@SpelAttribute` 等元注解）
 * 实时读取：框架注解与用户自定义注解一视同仁，无需登记到任何离线清单，
 * 也不存在任何 JSON 兜底。
 *
 * 规则：
 * - 注解本身（或通过元注解）不属于 lucky-httpclient 时返回 null，调用方应完全跳过；
 * - 声明上有元注解但未标注 `@SpelAttribute` 的属性降级为 [SpelValueKind.UNKNOWN]，
 *   不参与未解析检查；
 * - 组合注解递归解析元注解，并按 Spring 的合并语义展开 `@AliasFor`：
 *   注解内别名（`@AliasFor("result")`）、跨注解别名
 *   （`@AliasFor(annotation = X.class, attribute = "y")`）以及多级别名链
 *   （如 `@RespConvert#metaType` → `@ResultConvertMeta#metaType` → `@ConvertMetaType#value`）；
 * - 递归带 visited 集合做循环检测，深度上限 [MAX_DEPTH]。
 */
class SpelAnnotationResolver(
    private val metaReader: SpelMetaAnnotationReader = SpelMetaAnnotationReader()
) {

    /**
     * 解析注解 [annotation] 上的属性 [attribute]。
     *
     * @return 不属于 lucky-httpclient 上下文时返回 null；否则返回（可能是 UNKNOWN 的）结果
     */
    fun resolve(annotation: PsiAnnotation, attribute: String): ResolvedSpelAttribute? {
        val fqName = annotation.qualifiedName ?: return null
        val declaration = annotation.resolveAnnotationType() ?: return null
        findRegistered(fqName, declaration, attribute, HashSet(), 0)?.let { return applyDynamic(annotation, it) }
        if (!isKnown(fqName, declaration, HashSet(), 0)) {
            return null
        }
        return ResolvedSpelAttribute(
            declaredAnnotationFqName = fqName,
            declaredAttribute = attribute,
            effectiveAnnotationFqName = fqName,
            effectiveAttribute = attribute,
            metadata = SpelAttributeMetadata(SpelValueKind.UNKNOWN, SpelLifecycle.METHOD)
        )
    }

    /**
     * 应用「使用处」的动态信息：
     *
     * - 属性没有显式声明 `stage` 时，读取 `@SpelStage` 标记元素的实际值
     *   （例如 `@Hook(lifecycle = Lifecycle.REQUEST_INIT)`）；
     * - 属性没有显式声明 `separator` 时，读取 `@SpelSeparator` 标记元素的实际值。
     *
     * 只在直接书写该注解时读取；组合注解经元注解解析出的属性沿用表内默认值。
     */
    private fun applyDynamic(annotation: PsiAnnotation, resolved: ResolvedSpelAttribute): ResolvedSpelAttribute {
        if (resolved.declaredAnnotationFqName != resolved.effectiveAnnotationFqName) {
            return resolved
        }
        val annotationMetadata = metadataOf(annotation.resolveAnnotationType()) ?: return resolved
        var metadata = resolved.metadata
        if (!metadata.explicit.contains(STAGE_ATTRIBUTE)) {
            readLifecycle(annotation, annotationMetadata.stageElement)?.let { stage ->
                metadata = metadata.copy(lifecycle = stage)
            }
        }
        if (metadata.separator.isEmpty()) {
            readString(annotation, annotationMetadata.separatorElement)
                ?.takeIf { it.isNotEmpty() }
                ?.let { separator -> metadata = metadata.copy(separator = separator) }
        }
        return if (metadata === resolved.metadata) resolved else resolved.copy(metadata = metadata)
    }

    /** 读取使用处 `@SpelStage` 元素的枚举值；未书写或无法识别时返回 null。 */
    private fun readLifecycle(annotation: PsiAnnotation, element: String?): SpelLifecycle? {
        if (element == null) {
            return null
        }
        val value = annotation.findAttributeValue(element) ?: return null
        val raw = (value as? PsiLiteralExpression)?.value as? String ?: value.text
        val name = raw.substringAfterLast('.').trim('"')
        return SpelLifecycle.values().firstOrNull { it.name == name }
    }

    /** 读取使用处 `@SpelSeparator` 元素的字符串值；未书写时返回 null。 */
    private fun readString(annotation: PsiAnnotation, element: String?): String? {
        if (element == null) {
            return null
        }
        return (annotation.findAttributeValue(element) as? PsiLiteralExpression)?.value as? String
    }

    /**
     * 解析注解上所有显式书写的属性。
     */
    fun collect(annotation: PsiAnnotation): List<ResolvedSpelAttribute> =
        annotation.parameterList.attributes.mapNotNull { pair ->
            resolve(annotation, pair.name ?: "value")
        }

    /**
     * 字面量是否属于 SpEL 求值属性：显式标注 `@SpelAttribute` 且 kind 参与求值。
     *
     * 未标注（如自定义注解 `@DemoLiteral#value`）、LITERAL、NONE、UNKNOWN
     * 都返回 false：运行期框架不会求值，补全/引用/高亮/检查器必须完全跳过。
     * 高亮、补全、引用与所有检查器共用本判定，避免各处规则漂移。
     */
    fun isSpelLiteral(literal: PsiLiteralExpression): Boolean {
        val attribute = resolveLiteral(literal) ?: return false
        return attribute.kind !in NON_SPEL_KINDS
    }

    /**
     * 函数名字面量：`kind = FUNCTION` 且整段文本就是函数名（不含 `#{}`）。
     *
     * `SpelKind#FUNCTION` 允许两种写法：函数名字面量，或求值结果为函数名的
     * SpEL 表达式。运行期 `Context#autoExecuteSpELOrFunc` 先用模板语义解析属性值，
     * 不含 `#{}` 的文本原样作为函数名，再从变量表按名字取 `Method`
     * （`getVar(name, Method.class)` 等价于 `#name`）。
     *
     * 先做廉价的文本判定再解析注解：引用收集会对每个字符串字面量调用本方法，
     * URL、中文等非法标识符必须在解析注解前就被排除。
     */
    fun isFunctionNameLiteral(literal: PsiLiteralExpression): Boolean {
        if (literal.text.contains("#{")) {
            return false
        }
        if (functionNameOf(literal) == null) {
            return false
        }
        return isFunctionNameAttribute(literal)
    }

    /**
     * 属性是否声明为 `kind = FUNCTION`（不看当前值）。
     *
     * 补全与自动弹出需要本判定：补全副本会在光标处插入 dummy identifier
     * （`IntellijIdeaRulezzz `），此时属性值不是合法标识符，但补全仍须弹出。
     */
    fun isFunctionNameAttribute(literal: PsiLiteralExpression): Boolean =
        resolveLiteral(literal)?.kind == SpelValueKind.FUNCTION

    /**
     * 函数名字面量的函数名：空串表示尚未输入（供补全使用）。
     *
     * 非标识符内容返回 null：`"http://x"` 这类值运行期一定找不到函数，
     * 交给未解析诊断报错，不参与高亮、跳转、文档与补全。
     */
    fun functionNameOf(literal: PsiLiteralExpression): String? {
        val value = runCatching { literal.value as? String }.getOrNull() ?: return null
        if (value.isEmpty()) {
            return value
        }
        if (!Character.isJavaIdentifierStart(value[0])) {
            return null
        }
        return value.takeIf { name -> name.all { Character.isJavaIdentifierPart(it) } }
    }

    /**
     * 解析字符串字面量所在的注解属性；字面量不在注解属性中时返回 null。
     */
    fun resolveLiteral(literal: PsiLiteralExpression): ResolvedSpelAttribute? {
        var current: PsiElement? = literal.parent
        while (current != null && current !is PsiNameValuePair) {
            if (current is PsiAnnotation) {
                return null
            }
            current = current.parent
        }
        val pair = current as? PsiNameValuePair ?: return null
        val annotation = pair.parent?.parent as? PsiAnnotation ?: return null
        return resolve(annotation, pair.name ?: "value")
    }

    /**
     * 注解本身或任意层元注解是否携带 SpEL 元注解。
     */
    fun isKnown(annotation: PsiAnnotation): Boolean {
        val fqName = annotation.qualifiedName ?: return false
        val declaration = annotation.resolveAnnotationType() ?: return false
        return isKnown(fqName, declaration, HashSet(), 0)
    }

    private fun isKnown(
        fqName: String,
        declaration: PsiClass,
        visited: MutableSet<String>,
        depth: Int
    ): Boolean {
        if (!accept(fqName, null, visited, depth)) {
            return false
        }
        if (metadataOf(declaration) != null) {
            return true
        }
        return metaAnnotations(declaration).any { meta ->
            val metaFqName = meta.qualifiedName ?: return@any false
            val metaDeclaration = meta.resolveAnnotationType() ?: return@any false
            isKnown(metaFqName, metaDeclaration, visited, depth + 1)
        }
    }

    private fun findRegistered(
        fqName: String,
        declaration: PsiClass,
        attribute: String,
        visited: MutableSet<String>,
        depth: Int
    ): ResolvedSpelAttribute? {
        if (!accept(fqName, attribute, visited, depth)) {
            return null
        }

        val runtime = metaReader.read(declaration)
        if (runtime != null) {
            runtime.attributes[attribute]?.let { metadata ->
                return ResolvedSpelAttribute(fqName, attribute, fqName, attribute, metadata)
            }
            // 声明上有元注解但该元素未标注：按源码语义不参与 SpEL，
            // 继续沿别名链与元注解链查找
        }

        // 跟随 @AliasFor：注解内别名留在当前注解，跨注解别名切到目标注解继续，
        // 多级链（组合注解 → 元注解 → 元元注解）由此递归展开
        aliasTarget(declaration, attribute)?.let { target ->
            val targetFqName = target.annotationFqName ?: fqName
            val targetDeclaration = if (targetFqName == fqName) declaration
            else resolveAnnotationClass(declaration, targetFqName)
            if (targetDeclaration != null) {
                findRegistered(targetFqName, targetDeclaration, target.attribute, visited, depth + 1)?.let { found ->
                    return found.copy(declaredAnnotationFqName = fqName, declaredAttribute = attribute)
                }
            }
        }

        for (meta in metaAnnotations(declaration)) {
            val metaFqName = meta.qualifiedName ?: continue
            val metaDeclaration = meta.resolveAnnotationType() ?: continue
            val found = findRegistered(metaFqName, metaDeclaration, attribute, visited, depth + 1) ?: continue
            return found.copy(declaredAnnotationFqName = fqName, declaredAttribute = attribute)
        }
        return null
    }

    /** 注解元数据：直接读取声明上的元注解。 */
    private fun metadataOf(declaration: PsiClass?): SpelAnnotationMetadata? =
        declaration?.let { metaReader.read(it) }

    private fun metaAnnotations(declaration: PsiClass): Array<PsiAnnotation> =
        declaration.modifierList?.annotations ?: emptyArray<PsiAnnotation>()

    /** 循环检测与深度上限；java.lang.annotation 下的元注解不参与解析。 */
    private fun accept(
        fqName: String,
        attribute: String?,
        visited: MutableSet<String>,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) {
            return false
        }
        if (fqName.startsWith("java.lang.annotation.")) {
            return false
        }
        // 属性级 visited：注解内别名会回到同一注解的另一个属性，不能按注解去重
        return visited.add(if (attribute == null) fqName else "$fqName#$attribute")
    }

    /**
     * 读取属性方法上的 `@AliasFor`，按 Spring 的默认值规则补全：
     *
     * - `annotation` 未指定（默认 `Annotation.class`）表示当前注解；
     * - `attribute` 未指定时取 `value`（单值写法 `@AliasFor("result")`），再缺省为当前属性名。
     */
    private fun aliasTarget(declaration: PsiClass, attribute: String): AliasTarget? {
        val method = declaration.findMethodsByName(attribute, false).firstOrNull() ?: return null
        val alias = findAliasFor(method) ?: return null
        val targetAnnotation = readClassAttribute(alias, "annotation")
            ?.takeUnless { it == JAVA_LANG_ANNOTATION }
        val targetAttribute = readStringAttribute(alias, "attribute")?.takeIf { it.isNotBlank() }
            ?: readStringAttribute(alias, "value")?.takeIf { it.isNotBlank() }
            ?: attribute
        return AliasTarget(targetAnnotation, targetAttribute)
    }

    /** 按全限定名解析注解类；无法解析时返回 null，调用方继续按元注解递归。 */
    private fun resolveAnnotationClass(context: PsiClass, fqName: String): PsiClass? =
        JavaPsiFacade.getInstance(context.project)
            .findClass(fqName, GlobalSearchScope.allScope(context.project))

    private data class AliasTarget(val annotationFqName: String?, val attribute: String)

    private fun findAliasFor(owner: PsiModifierListOwner): PsiAnnotation? {
        owner.getAnnotation(ALIAS_FOR)?.let { return it }
        // spring-core 不在 classpath 时 FQ 名无法解析，退化为按简单名匹配
        return owner.modifierList?.annotations?.firstOrNull {
            it.qualifiedName == ALIAS_FOR || it.nameReferenceElement?.referenceName == "AliasFor"
        }
    }

    private fun readClassAttribute(annotation: PsiAnnotation, name: String): String? {
        val value = annotation.findAttributeValue(name) as? PsiClassObjectAccessExpression ?: return null
        val classType = value.operand.type as? PsiClassType ?: return null
        return classType.resolve()?.qualifiedName
    }

    private fun readStringAttribute(annotation: PsiAnnotation, name: String): String? =
        runCatching { (annotation.findAttributeValue(name) as? PsiLiteralExpression)?.value as? String }
            .getOrNull()

    companion object {
        private const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"
        private const val JAVA_LANG_ANNOTATION = "java.lang.annotation.Annotation"
        private const val STAGE_ATTRIBUTE = "stage"
        private const val MAX_DEPTH = 16

        /**
         * 运行期不求值的属性类型：与「未标注 @SpelAttribute」一致，完全不解析。
         *
         * `UNKNOWN` 产生于「注解声明上有 SpEL 元注解，但当前属性未标注」：
         * 源码语义下该属性不是 SpEL，不能因为同一注解的其它属性是 SpEL 而连带解析。
         */
        val NON_SPEL_KINDS = setOf(
            SpelValueKind.LITERAL,
            SpelValueKind.NONE,
            SpelValueKind.UNKNOWN
        )
    }
}
