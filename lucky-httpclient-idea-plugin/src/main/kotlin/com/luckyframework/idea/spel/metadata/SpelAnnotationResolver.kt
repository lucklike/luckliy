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

    val isAlias: Boolean
        get() = declaredAnnotationFqName != effectiveAnnotationFqName || declaredAttribute != effectiveAttribute
}

/**
 * 把 PSI 注解解析到注解元数据表。
 *
 * 规则：
 * - 注解本身（或通过元注解）不属于 lucky-httpclient 时返回 null，调用方应完全跳过；
 * - 已登记注解的未登记属性降级为 [SpelValueKind.UNKNOWN]，不参与未解析检查；
 * - 组合注解递归解析元注解，并按 Spring 的合并语义展开 `@AliasFor`：
 *   注解内别名（`@AliasFor("result")`）、跨注解别名
 *   （`@AliasFor(annotation = X.class, attribute = "y")`）以及多级别名链
 *   （如 `@RespConvert#metaType` → `@ResultConvertMeta#metaType` → `@ConvertMetaType#value`）；
 * - 递归带 visited 集合做循环检测，深度上限 [MAX_DEPTH]。
 */
class SpelAnnotationResolver(
    private val table: AnnotationTable = AnnotationMetadataLoader.load()
) {

    /**
     * 解析注解 [annotation] 上的属性 [attribute]。
     *
     * @return 不属于 lucky-httpclient 上下文时返回 null；否则返回（可能是 UNKNOWN 的）结果
     */
    fun resolve(annotation: PsiAnnotation, attribute: String): ResolvedSpelAttribute? {
        val fqName = annotation.qualifiedName ?: return null
        val declaration = annotation.resolveAnnotationType() ?: return null
        findRegistered(fqName, declaration, attribute, HashSet(), 0)?.let { return it }
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
     * 解析注解上所有显式书写的属性。
     */
    fun collect(annotation: PsiAnnotation): List<ResolvedSpelAttribute> =
        annotation.parameterList.attributes.mapNotNull { pair ->
            resolve(annotation, pair.name ?: "value")
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
     * 注解本身或任意层元注解是否登记在元数据表中。
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
        if (table.metadataFor(fqName) != null) {
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

        table.attribute(fqName, attribute)?.let { metadata ->
            return ResolvedSpelAttribute(fqName, attribute, fqName, attribute, metadata)
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
        private const val MAX_DEPTH = 16
    }
}
