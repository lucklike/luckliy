package com.luckyframework.idea.navigator

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReferenceExpression

/**
 * 侧边栏需要的 HTTP 注解信息：请求方法与 URL。
 *
 * 整条链路只依赖注解声明，不引入框架 jar：
 *
 * - `@Get`/`@Post` 等组合注解在声明处标注 `@HttpRequest(method = RequestMethod.XXX)`，
 *   沿元注解链读取 `method` 即可；
 * - URL 取使用处显式书写的 `url` / `value`（`@Get("/x")` 的 `value` 通过
 *   `@AliasFor` 映射到 `@HttpRequest#url`，使用处的值本身已经是最终值）；
 * - 属性值支持字符串字面量与编译期常量。
 *
 * 用户自定义组合注解（`@MyApi` 上标 `@Get`）沿元注解链递归，深度上限
 * [MAX_DEPTH]，循环用 visited 集合截断。
 */
object LuckyHttpAnnotations {

    private const val HTTP_REQUEST = "com.luckyframework.httpclient.proxy.annotations.HttpRequest"

    private const val MAX_DEPTH = 4

    /**
     * 方法/类上声明的 HTTP 请求方法（大写枚举名，如 `GET`）。
     *
     * 未声明或声明为 `RequestMethod.NON` 时返回 null：`@HttpRequest` 的默认值是
     * `NON`，只有显式指定 `method` 才代表确定的方法。
     */
    fun httpMethodOf(owner: PsiModifierListOwner): String? =
        annotationsOf(owner).firstNotNullOfOrNull { requestMethodOf(it, HashSet(), 0) }

    /**
     * 使用处书写的 URL：优先显式 `url`，其次显式 `value`。
     *
     * 只读取 HTTP 相关注解（自身或元注解链上含 `@HttpRequest`），避免把
     * `@SuppressWarnings("x")` 一类无关注解的 `value` 当成 URL。
     * 组合注解（`@Get("/x")`）的属性值直接写在注解使用处；注解声明上的
     * 默认值是空串，不参与。返回原文本（可能包含 `#{...}`），由 UI 决定展示方式。
     */
    fun urlOf(owner: PsiModifierListOwner): String? {
        for (annotation in annotationsOf(owner)) {
            if (!isHttpAnnotation(annotation, HashSet(), 0)) {
                continue
            }
            urlAttributeOf(annotation, "url")?.let { return it }
            urlAttributeOf(annotation, "value")?.let { return it }
        }
        return null
    }

    private fun annotationsOf(owner: PsiModifierListOwner): List<PsiAnnotation> =
        owner.modifierList?.annotations?.toList().orEmpty()

    /** 注解自身是 `@HttpRequest`，或元注解链上能到达 `@HttpRequest`。 */
    private fun isHttpAnnotation(
        annotation: PsiAnnotation,
        visited: MutableSet<String>,
        depth: Int
    ): Boolean {
        if (depth > MAX_DEPTH) {
            return false
        }
        val fqName = annotation.qualifiedName ?: return false
        if (!visited.add(fqName)) {
            return false
        }
        if (fqName == HTTP_REQUEST) {
            return true
        }
        val declaration = annotation.resolveAnnotationType() ?: return false
        return (declaration.modifierList?.annotations ?: emptyArray())
            .any { isHttpAnnotation(it, visited, depth + 1) }
    }

    private fun requestMethodOf(
        annotation: PsiAnnotation,
        visited: MutableSet<String>,
        depth: Int
    ): String? {
        if (depth > MAX_DEPTH) {
            return null
        }
        val fqName = annotation.qualifiedName ?: return null
        if (!visited.add(fqName)) {
            return null
        }
        if (fqName == HTTP_REQUEST) {
            return methodName(annotation.findDeclaredAttributeValue("method"))
        }
        val declaration = annotation.resolveAnnotationType() ?: return null
        for (meta in declaration.modifierList?.annotations ?: emptyArray()) {
            requestMethodOf(meta, visited, depth + 1)?.let { return it }
        }
        return null
    }

    /** `RequestMethod.GET` 一类的枚举值取最后一段；`NON` 代表未设置。 */
    private fun methodName(value: PsiAnnotationMemberValue?): String? {
        if (value == null) {
            return null
        }
        val text = when (value) {
            is PsiReferenceExpression -> value.referenceName ?: value.text
            is PsiLiteralExpression -> value.value as? String
            else -> value.text
        } ?: return null
        return text.substringAfterLast('.').trim('"')
            .takeIf { it.isNotEmpty() && it != "NON" }
    }

    /** 使用处显式声明的属性值；未书写或为空返回 null。 */
    private fun urlAttributeOf(annotation: PsiAnnotation, attribute: String): String? {
        val value = annotation.findDeclaredAttributeValue(attribute) ?: return null
        return constantString(value)?.takeIf { it.isNotBlank() }
    }

    /** 字符串字面量或编译期常量；常量解析失败时回退初始化表达式。 */
    private fun constantString(value: PsiAnnotationMemberValue): String? {
        (value as? PsiLiteralExpression)?.let { return it.value as? String }
        val field = (value as? PsiReferenceExpression)?.resolve() as? PsiField ?: return null
        (field.computeConstantValue() as? String)?.let { return it }
        return (field.initializer as? PsiLiteralExpression)?.value as? String
    }
}
