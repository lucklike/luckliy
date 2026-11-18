package com.luckyframework.idea.std

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.GlobalSearchScope

/**
 * `@StdHttpClient` / `@Api` 属性的合并注解读取。
 *
 * 运行期使用 Spring 的合并注解语义：组合注解上的 `@AliasFor` 会把属性值
 * 传递到目标注解。IDE 侧不依赖 Spring 运行期，按源码声明重新实现同一套规则：
 *
 * - 直接标注优先；
 * - `@AliasFor` 支持注解内别名、跨注解别名与多级链；
 * - 组合注解递归解析元注解，visited 防环，深度上限 [MAX_DEPTH]；
 * - 注解类无法解析（依赖缺失）时返回 null，由调用方回退默认值。
 *
 * 本类不修改 `SpelAnnotationResolver`，避免影响现有 SpEL 链路。
 */
object StdAnnotationValues {

    /**
     * 接口实际使用的 configId。
     *
     * 返回 null 表示没有显式配置，调用方应回退到接口简单类名（与运行期
     * `CommonFunctions.getApiConfigId` 一致）。
     */
    fun stdClientConfigId(psiClass: PsiClass): String? {
        directStdClient(psiClass)?.let { annotation ->
            stringValue(annotation.findDeclaredAttributeValue(CONFIG_ID))
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        directAnnotation(psiClass, API_CONFIG)?.let { annotation ->
            stringValue(annotation.findDeclaredAttributeValue(VALUE))
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        for (annotation in annotationsOf(psiClass)) {
            val fqName = annotation.qualifiedName ?: continue
            if (fqName == STD_HTTP_CLIENT || fqName == API_CONFIG) {
                continue
            }
            if (!carriesStdHttpClient(annotation)) {
                continue
            }
            // 使用处显式覆盖 → 使用处别名覆盖 → 组合注解声明上的默认值
            stringValue(annotation.findDeclaredAttributeValue(CONFIG_ID))
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
            aliasValueFor(annotation, STD_HTTP_CLIENT, CONFIG_ID)
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
            val declaration = annotation.resolveAnnotationType() ?: continue
            directStdClient(declaration)?.let { std ->
                stringValue(std.findDeclaredAttributeValue(CONFIG_ID))
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { return it }
            }
            directAnnotation(declaration, API_CONFIG)?.let { api ->
                stringValue(api.findDeclaredAttributeValue(VALUE))
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { return it }
            }
        }
        return null
    }

    /**
     * 方法的 apiId：`@Api#value` 优先，未配置时返回 null（调用方回退方法名）。
     */
    fun apiId(method: PsiMethod): String? {
        directAnnotation(method, API)?.let { annotation ->
            stringValue(annotation.findDeclaredAttributeValue(VALUE))
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }
        for (annotation in annotationsOf(method)) {
            val fqName = annotation.qualifiedName ?: continue
            if (fqName == API) {
                continue
            }
            if (!carriesMetaAnnotation(annotation, API)) {
                continue
            }
            stringValue(annotation.findDeclaredAttributeValue(VALUE))
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
            aliasValueFor(annotation, API, VALUE)
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
            val declaration = annotation.resolveAnnotationType() ?: continue
            directAnnotation(declaration, API)?.let { api ->
                stringValue(api.findDeclaredAttributeValue(VALUE))
                    ?.takeIf { it.isNotBlank() }
                    ?.let { return it }
            }
        }
        return null
    }

    /** 注解本身或任意层元注解是否携带 `@StdHttpClient`。 */
    fun carriesStdHttpClient(annotation: PsiAnnotation): Boolean {
        val fqName = annotation.qualifiedName ?: return false
        if (fqName == STD_HTTP_CLIENT) {
            return true
        }
        val declaration = annotation.resolveAnnotationType() ?: return false
        return carriesMeta(declaration, STD_HTTP_CLIENT, HashSet(), 0)
    }

    /**
     * 属性 [attribute] 是否最终映射到 `@StdHttpClient#configId`。
     *
     * 用于 Ctrl+Click：只有真正会传给运行期 configId 的字面量才建立跳转，
     * `path`/`beanId` 等其他属性以及注解声明处的默认值都不处理。
     */
    fun isConfigIdAttribute(annotation: PsiAnnotation, attribute: String): Boolean {
        val fqName = annotation.qualifiedName ?: return false
        val declaration = annotation.resolveAnnotationType() ?: return false
        if (fqName == STD_HTTP_CLIENT && attribute == CONFIG_ID) {
            return true
        }
        return resolvesTo(fqName, declaration, attribute, STD_HTTP_CLIENT, CONFIG_ID, HashSet(), 0)
    }

    // ---------------------------------------------------------------------------------------------
    // 注解与属性读取
    // ---------------------------------------------------------------------------------------------

    private fun annotationsOf(owner: PsiModifierListOwner): Array<PsiAnnotation> =
        owner.modifierList?.annotations ?: emptyArray()

    private fun directAnnotation(owner: PsiModifierListOwner, fqName: String): PsiAnnotation? =
        annotationsOf(owner).firstOrNull {
            it.qualifiedName == fqName ||
                it.nameReferenceElement?.referenceName == fqName.substringAfterLast('.')
        }

    private fun directStdClient(owner: PsiModifierListOwner): PsiAnnotation? =
        directAnnotation(owner, STD_HTTP_CLIENT)

    /**
     * 读取注解属性的字符串值。
     *
     * 支持字面量与常量引用；常量解析不到时返回 null，由调用方回退默认值。
     */
    private fun stringValue(value: PsiAnnotationMemberValue?): String? {
        (value as? PsiLiteralExpression)?.let { return it.value as? String }
        val field = (value as? PsiReferenceExpression)?.resolve() as? PsiField ?: return null
        return field.computeConstantValue() as? String
    }

    // ---------------------------------------------------------------------------------------------
    // 元注解传播
    // ---------------------------------------------------------------------------------------------

    private fun carriesMeta(
        declaration: PsiClass,
        targetFqName: String,
        visited: MutableSet<String>,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) {
            return false
        }
        val fqName = declaration.qualifiedName ?: return false
        if (!visited.add(fqName)) {
            return false
        }
        for (meta in declaration.modifierList?.annotations ?: emptyArray()) {
            val metaFqName = meta.qualifiedName ?: continue
            if (metaFqName == targetFqName) {
                return true
            }
            val metaDeclaration = meta.resolveAnnotationType() ?: continue
            if (carriesMeta(metaDeclaration, targetFqName, visited, depth + 1)) {
                return true
            }
        }
        return false
    }

    private fun carriesMetaAnnotation(annotation: PsiAnnotation, targetFqName: String): Boolean {
        val fqName = annotation.qualifiedName ?: return false
        if (fqName == targetFqName) {
            return true
        }
        val declaration = annotation.resolveAnnotationType() ?: return false
        return carriesMeta(declaration, targetFqName, HashSet(), 0)
    }

    // ---------------------------------------------------------------------------------------------
    // @AliasFor
    // ---------------------------------------------------------------------------------------------

    /** 组合注解使用处通过别名属性覆盖目标属性时，返回使用处的值。 */
    private fun aliasValueFor(
        annotation: PsiAnnotation,
        targetFqName: String,
        targetAttribute: String
    ): String? {
        val declaration = annotation.resolveAnnotationType() ?: return null
        for (method in declaration.methods) {
            if (!aliasTargets(method, targetFqName, targetAttribute)) {
                continue
            }
            stringValue(annotation.findDeclaredAttributeValue(method.name))
                ?.takeIf { it.isNotEmpty() }
                ?.let { return it }
        }
        return null
    }

    /** 方法上的 `@AliasFor` 是否指向目标注解的目标属性。 */
    private fun aliasTargets(method: PsiMethod, targetFqName: String, targetAttribute: String): Boolean {
        val alias = findAliasFor(method) ?: return false
        val targetAnnotation = readClassAttribute(alias, "annotation")
            ?.takeUnless { it == JAVA_LANG_ANNOTATION }
        val attribute = readStringAttribute(alias, "attribute")?.takeIf { it.isNotBlank() }
            ?: readStringAttribute(alias, "value")?.takeIf { it.isNotBlank() }
            ?: method.name
        val annotationFqName = targetAnnotation ?: method.containingClass?.qualifiedName ?: return false
        return annotationFqName == targetFqName && attribute == targetAttribute
    }

    /**
     * 属性沿 `@AliasFor` 与元注解链是否最终到达目标属性。
     *
     * 与 `SpelAnnotationResolver` 的别名规则一致，但只判断可达性，不读取元数据。
     */
    private fun resolvesTo(
        currentFqName: String,
        declaration: PsiClass,
        attribute: String,
        targetFqName: String,
        targetAttribute: String,
        visited: MutableSet<String>,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) {
            return false
        }
        if (!visited.add("$currentFqName#$attribute")) {
            return false
        }
        if (currentFqName == targetFqName && attribute == targetAttribute) {
            return true
        }
        aliasTarget(declaration, attribute)?.let { target ->
            val targetFq = target.annotationFqName ?: currentFqName
            val targetDeclaration = if (targetFq == currentFqName) {
                declaration
            } else {
                resolveClass(declaration, targetFq)
            }
            if (targetDeclaration != null &&
                resolvesTo(
                    targetFq,
                    targetDeclaration,
                    target.attribute,
                    targetFqName,
                    targetAttribute,
                    visited,
                    depth + 1
                )
            ) {
                return true
            }
        }
        for (meta in declaration.modifierList?.annotations ?: emptyArray()) {
            val metaFqName = meta.qualifiedName ?: continue
            val metaDeclaration = meta.resolveAnnotationType() ?: continue
            if (resolvesTo(
                    metaFqName,
                    metaDeclaration,
                    attribute,
                    targetFqName,
                    targetAttribute,
                    visited,
                    depth + 1
                )
            ) {
                return true
            }
        }
        return false
    }

    /**
     * 读取属性方法上的 `@AliasFor`，按 Spring 的默认值规则补全：
     * `annotation` 未指定表示当前注解；`attribute` 缺省时取 `value`，再缺省为当前属性名。
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

    private fun resolveClass(context: PsiClass, fqName: String): PsiClass? =
        JavaPsiFacade.getInstance(context.project)
            .findClass(fqName, GlobalSearchScope.allScope(context.project))

    private const val STD_HTTP_CLIENT = "io.github.lucklike.httpclient.std.StdHttpClient"
    private const val API_CONFIG = "com.luckyframework.httpclient.proxy.configapi.ApiConfig"
    private const val API = "com.luckyframework.httpclient.proxy.configapi.Api"
    private const val ALIAS_FOR = "org.springframework.core.annotation.AliasFor"
    private const val JAVA_LANG_ANNOTATION = "java.lang.annotation.Annotation"
    private const val CONFIG_ID = "configId"
    private const val VALUE = "value"
    private const val MAX_DEPTH = 16
}
