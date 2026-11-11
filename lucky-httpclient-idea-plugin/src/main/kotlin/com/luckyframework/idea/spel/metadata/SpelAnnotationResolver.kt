package com.luckyframework.idea.spel.metadata

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiNameValuePair

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
 * - 组合注解递归解析元注解，并展开 `@AliasFor(annotation = X.class, attribute = "y")`；
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
        if (!accept(fqName, visited, depth)) {
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
        if (!accept(fqName, visited, depth)) {
            return null
        }

        table.attribute(fqName, attribute)?.let { metadata ->
            return ResolvedSpelAttribute(fqName, attribute, fqName, attribute, metadata)
        }

        aliasTarget(declaration, attribute)?.let { (targetAnnotation, targetAttribute) ->
            table.attribute(targetAnnotation, targetAttribute)?.let { metadata ->
                return ResolvedSpelAttribute(fqName, attribute, targetAnnotation, targetAttribute, metadata)
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
    private fun accept(fqName: String, visited: MutableSet<String>, depth: Int): Boolean {
        if (depth > MAX_DEPTH) {
            return false
        }
        if (fqName.startsWith("java.lang.annotation.")) {
            return false
        }
        return visited.add(fqName)
    }

    /** 读取属性方法上的 `@AliasFor(annotation = X.class, attribute = "y")`。 */
    private fun aliasTarget(declaration: PsiClass, attribute: String): Pair<String, String>? {
        val method = declaration.findMethodsByName(attribute, false).firstOrNull() ?: return null
        val alias = findAliasFor(method) ?: return null
        val targetAnnotation = readClassAttribute(alias, "annotation") ?: return null
        val targetAttribute = (alias.findAttributeValue("attribute") as? PsiLiteralExpression)?.value as? String
            ?: return null
        return targetAnnotation to targetAttribute
    }

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

    companion object {
        private const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"
        private const val MAX_DEPTH = 16
    }
}
