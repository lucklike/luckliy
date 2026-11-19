package com.luckyframework.idea.navigator

import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReferenceExpression

/**
 * 侧边栏需要的 HTTP 注解信息：请求方法、URL 与 func。
 *
 * 整条链路只依赖注解声明，不引入框架 jar：
 *
 * - `@Get`/`@Post` 等组合注解在声明处标注 `@HttpRequest(method = RequestMethod.XXX)`，
 *   沿元注解链读取 `method` 即可；
 * - URL 取使用处显式书写的 `url` / `value`（`@Get("/x")` 的 `value` 通过
 *   `@AliasFor` 映射到 `@HttpRequest#url`，使用处的值本身已经是最终值）；
 * - 获取 URL 的函数取 `func`；运行期 URL 优先，URL 为空或校验失败才调用 func；
 * - 属性值支持字符串字面量与编译期常量，并保留源码字面量供侧边栏
 *   定位 `#{...}` / `${...}` 并跳转。
 *
 * 用户自定义组合注解（`@MyApi` 上标 `@Get`）沿元注解链递归，深度上限
 * [MAX_DEPTH]，循环用 visited 集合截断。
 */
object LuckyHttpAnnotations {

    private const val HTTP_REQUEST = "com.luckyframework.httpclient.proxy.annotations.HttpRequest"

    /** 3.0.3 的 legacy 组件注解，没有 `@HttpRequest` 元注解。 */
    private const val LEGACY_HTTP_CLIENT = "io.github.lucklike.httpclient.discovery.HttpClient"

    /** 标准客户端注解：baseUrl 来自 YAML 配置，注解上的 `path` 仍需拼接。 */
    private const val STD_HTTP_CLIENT = "io.github.lucklike.httpclient.std.StdHttpClient"

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
     * 默认值是空串，不参与。
     */
    fun urlOf(owner: PsiModifierListOwner): String? = urlSourceOf(owner)?.text

    /** URL 的值与字面量；运行期优先使用 URL，其次才使用 `func`。 */
    fun urlSourceOf(owner: PsiModifierListOwner): HttpValue? {
        for (annotation in annotationsOf(owner)) {
            if (!isHttpAnnotation(annotation, HashSet(), 0)) {
                continue
            }
            attributeSourceOf(annotation, "url")?.let { return it }
            attributeSourceOf(annotation, "value")?.let { return it }
        }
        return null
    }

    /** 获取 URL 的函数（`func`）；函数名或含 `#{}` 的表达式由 UI 决定展示方式。 */
    fun funcSourceOf(owner: PsiModifierListOwner): HttpValue? {
        for (annotation in annotationsOf(owner)) {
            if (!isHttpAnnotation(annotation, HashSet(), 0)) {
                continue
            }
            attributeSourceOf(annotation, "func")?.let { return it }
        }
        return null
    }

    /**
     * 类级 baseUrl 的组成部分：`url`/`urlFunc` 与 `path`/`pathFunc`。
     *
     * `@HttpClient` 同时支持四个属性，`@StdHttpClient` 的 `path` 通过
     * `@AliasFor` 映射到 `@HttpClient#path`。注意 `@StdHttpClient` 的元注解
     * 链上含 `@HttpRequest`，因此 HTTP 注解分支也必须读取 `path`。
     */
    fun baseUrlPartsOf(owner: PsiModifierListOwner): BaseUrlParts {
        var url: HttpValue? = null
        var urlFunc: HttpValue? = null
        var path: HttpValue? = null
        var pathFunc: HttpValue? = null
        for (annotation in annotationsOf(owner)) {
            val httpAnnotation = isHttpAnnotation(annotation, HashSet(), 0)
            if (!httpAnnotation && !annotation.isLegacyHttpClient() && !annotation.isStdHttpClient()) {
                continue
            }
            if (url == null) {
                url = attributeSourceOf(annotation, "url") ?: attributeSourceOf(annotation, "value")
            }
            if (urlFunc == null) {
                urlFunc = attributeSourceOf(annotation, "urlFunc")
            }
            if (path == null) {
                path = attributeSourceOf(annotation, "path")
            }
            if (pathFunc == null) {
                pathFunc = attributeSourceOf(annotation, "pathFunc")
            }
        }
        return BaseUrlParts(url, urlFunc, path, pathFunc)
    }

    /** 注解尚不可解析时按短名匹配，兼容只写了源码而依赖未就绪的场景。 */
    private fun PsiAnnotation.isLegacyHttpClient(): Boolean =
        qualifiedName == LEGACY_HTTP_CLIENT ||
            (qualifiedName == null && nameReferenceElement?.referenceName == "HttpClient")

    private fun PsiAnnotation.isStdHttpClient(): Boolean =
        qualifiedName == STD_HTTP_CLIENT ||
            (qualifiedName == null && nameReferenceElement?.referenceName == "StdHttpClient")

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
    private fun attributeSourceOf(annotation: PsiAnnotation, attribute: String): HttpValue? {
        val value = annotation.findDeclaredAttributeValue(attribute) ?: return null
        return valueOf(value)?.takeIf { it.text.isNotBlank() }
    }

    /** 字符串字面量或编译期常量；常量解析失败时回退初始化表达式。 */
    private fun valueOf(value: PsiAnnotationMemberValue): HttpValue? {
        (value as? PsiLiteralExpression)?.let { literal ->
            val text = literal.value as? String ?: return null
            return HttpValue(text, literal)
        }
        val field = (value as? PsiReferenceExpression)?.resolve() as? PsiField ?: return null
        val initializer = field.initializer as? PsiLiteralExpression
        val text = (field.computeConstantValue() as? String) ?: (initializer?.value as? String) ?: return null
        return HttpValue(text, initializer)
    }
}

/**
 * 一个 HTTP 注解属性值。
 *
 * [text] 是运行期值（已还原 Java 转义）；[literal] 是书写处的字符串字面量，
 * 侧边栏用它定位 `#{...}` / `${...}` 的偏移并解析跳转目标。常量字段引用
 * 指向字段初始化表达式；无法定位时为 null。
 */
data class HttpValue(val text: String, val literal: PsiLiteralExpression? = null)

/** 类级 baseUrl 的四部分；未声明的属性为 null。 */
data class BaseUrlParts(
    val url: HttpValue?,
    val urlFunc: HttpValue?,
    val path: HttpValue?,
    val pathFunc: HttpValue?
)
