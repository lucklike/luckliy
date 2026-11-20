package com.luckyframework.idea.navigator

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.luckyframework.idea.std.StdClientEntry
import com.luckyframework.idea.std.StdHttpClientIndex

/**
 * 导航树与调试面板共用的请求解析：请求方法、baseUrl 与方法 path。
 *
 * 两处必须来自同一套规则：侧边栏里看到的路径就是调试窗口展示、
 * 发送请求时使用的路径，不能出现分歧。
 */
internal object LuckyRequestInfo {

    /** 标准客户端在未配置、也未标注请求方法时使用的默认方法。 */
    const val DEFAULT_HTTP_METHOD = "POST"

    /** HTTP 方法未声明路径时的展示值。 */
    const val EMPTY_HTTP_PATH = "/"

    /** 方法在组件中的 apiId：YAML 方法清单优先，回退方法名。 */
    fun apiIdOf(entry: StdClientEntry?, method: PsiMethod): String =
        entry?.methods?.firstOrNull { it.method == method }?.apiId ?: method.name

    /**
     * 方法展示的请求方法。
     *
     * 标准客户端的所有方法都会被代理，`StandardLifeCycleManager#setRequestMethod`
     * 的取值顺序是：YAML 方法级/类级 `method` → 请求对象（方法注解）→ 默认 POST。
     * 普通接口只采用方法注解，未声明时保持未知。
     */
    fun httpMethodOf(method: PsiMethod, entry: StdClientEntry?, apiId: String): String? {
        val annotationMethod = LuckyHttpAnnotations.httpMethodOf(method)
        if (entry == null) {
            return annotationMethod
        }
        return yamlHttpMethodOf(entry, apiId) ?: annotationMethod ?: DEFAULT_HTTP_METHOD
    }

    /**
     * 方法级 path 富文本：注解 URL/`func` → YAML 中的
     * `method-configs.<apiId>.path`，都没有时为 `/`；非 HTTP 方法返回 null。
     */
    fun pathPresentationOf(
        method: PsiMethod,
        entry: StdClientEntry?,
        apiId: String,
        httpMethod: String?
    ): LuckyPathPresentation? {
        if (httpMethod == null) {
            return null
        }
        return LuckyNavigatorPath.presentationOf(
            LuckyHttpAnnotations.urlSourceOf(method),
            LuckyHttpAnnotations.funcSourceOf(method)
        ) ?: LuckyNavigatorPath.plainPathOf(yamlPathOf(entry, apiId) ?: EMPTY_HTTP_PATH)
    }

    /**
     * 组件级 baseUrl 富文本：类上注解的 `url`/`urlFunc` 与 `path`/`pathFunc` →
     * `@StdHttpClient` 的 YAML 配置 baseUrl + 注解 `path`。
     *
     * 显式属性优先于函数属性（与运行期 url 优先于 func 一致）；函数属性
     * 静态分析无法执行，按函数名展示并保留跳转。注解值保留源码字面量，
     * `#{...}` / `${...}` 与方法 path 走同一条着色、折叠与跳转链路；
     * YAML 值没有 PSI，按纯文本展示。
     */
    fun baseUrlPresentationOf(project: Project, psiClass: PsiClass): LuckyPathPresentation? {
        val parts = LuckyHttpAnnotations.baseUrlPartsOf(psiClass)
        // baseUrl 是完整地址，不做接口 path 的 `/` 前缀规范化
        val path = parts.path?.let {
            LuckyNavigatorPath.presentationOf(it, null, withPathPrefix = false)
        } ?: parts.pathFunc?.let {
            LuckyNavigatorPath.presentationOf(null, it, withPathPrefix = false)
        }
        val url = parts.url?.let {
            LuckyNavigatorPath.presentationOf(it, null, withPathPrefix = false)
        } ?: parts.urlFunc?.let {
            LuckyNavigatorPath.presentationOf(null, it, withPathPrefix = false)
        }
        if (url != null) {
            return LuckyNavigatorPath.concatOf(url, path)
        }
        val yamlBaseUrl = StdHttpClientIndex.getInstance(project).entryOf(psiClass)
            ?.configNodes
            ?.firstNotNullOfOrNull { node -> node.baseUrl?.takeIf { it.isNotBlank() } }
        if (yamlBaseUrl != null) {
            return LuckyNavigatorPath.concatOf(LuckyNavigatorPath.plainOf(yamlBaseUrl), path)
        }
        return path
    }

    /** YAML 声明的请求方法：同一配置节点内方法级优先于类级，多个文件取首个声明值。 */
    private fun yamlHttpMethodOf(entry: StdClientEntry, apiId: String): String? =
        entry.configNodes.firstNotNullOfOrNull { it.apiMethods[apiId] ?: it.httpMethod }

    /** 方法在任一份匹配的 YAML 配置中声明的 path。 */
    private fun yamlPathOf(entry: StdClientEntry?, apiId: String): String? =
        entry?.configNodes?.firstNotNullOfOrNull { it.methodPaths[apiId] }
}
