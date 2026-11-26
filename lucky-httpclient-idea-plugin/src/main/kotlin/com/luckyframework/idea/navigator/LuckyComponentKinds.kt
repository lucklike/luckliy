package com.luckyframework.idea.navigator

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.luckyframework.idea.LuckyModificationTrackers
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import com.luckyframework.idea.std.StdAnnotationValues

/**
 * Lucky 组件身份的分类规则。
 *
 * 侧边栏分组与编辑器 gutter 调试图标共用同一份判断：只要规则一致，
 * 同一份代码在导航树与编辑器里看到的能力就不会分叉。
 */
internal object LuckyComponentKinds {

    /** `@LuckyComponent#type` → 分组；`AUTO` 按结构推导。 */
    fun of(type: String, psiClass: PsiClass): LuckyGroupKind = when (type) {
        "API" -> LuckyGroupKind.API
        "DATABASE" -> LuckyGroupKind.DATABASE
        "FUNCTION" -> LuckyGroupKind.FUNCTION
        "CONFIG" -> LuckyGroupKind.CONFIG
        "SUPPORT" -> LuckyGroupKind.SUPPORT
        else -> infer(psiClass)
    }

    /**
     * 方法是否可由调试面板调用。
     *
     * 与侧边栏方法行的调试图标同源：`@StdHttpClient` 接口，以及除函数组之外的
     * `@LuckyComponent` 组件，其非静态方法都经代理执行。注解声明本身不是
     * 可调用对象，不参与判断。
     *
     * 该判定运行在 daemon 行标记的后台线程，每次高亮都会对每个方法执行，
     * 必须保持轻量：
     *
     * - 组件身份只读 [SpelRuntimeSymbols.cachedComponents]（最近一次成功扫描的
     *   清单），不主动触发全项目注解扫描；
     * - 清单未命中时按当前类的本地注解与成员结构兜底，同样不访问项目级索引。
     *
     * 组件清单更新后由组件刷新服务重启 daemon，行标记随之重算。
     */
    fun isDebugApiMethod(project: Project, method: PsiMethod): Boolean {
        if (method.isConstructor || method.hasModifierProperty(PsiModifier.STATIC)) {
            return false
        }
        val psiClass = method.containingClass ?: return false
        if (psiClass.isAnnotationType) {
            return false
        }
        val className = psiClass.qualifiedName
        val cached = SpelRuntimeSymbols.cachedComponents(project)
        if (cached != null && className != null) {
            val component = cached.firstOrNull { it.className == className }
            if (component != null) {
                return of(component.type, psiClass) != LuckyGroupKind.FUNCTION
            }
        }
        return localKind(psiClass)?.let { it != LuckyGroupKind.FUNCTION } ?: false
    }

    /**
     * 组件清单未命中时的本地判定：只看当前类上的注解与成员结构。
     *
     * 结果按 Java 声明结构版本缓存，方法体编辑不会让缓存失效；返回 null
     * 表示无法确认组件身份（普通类或依赖未就绪）。
     */
    private fun localKind(psiClass: PsiClass): LuckyGroupKind? =
        CachedValuesManager.getCachedValue(psiClass, KIND_KEY) {
            CachedValueProvider.Result.create(
                computeLocalKind(psiClass),
                LuckyModificationTrackers.structure(psiClass.project)
            )
        }

    private fun computeLocalKind(psiClass: PsiClass): LuckyGroupKind? {
        if (isStdClient(psiClass) || hasAnnotation(psiClass, LEGACY_HTTP_CLIENT, "HttpClient")) {
            return LuckyGroupKind.API
        }
        val component = luckyComponentOf(psiClass)
            ?: return inferLocalKind(psiClass)
        return of(typeOf(component), psiClass)
    }

    /** 没有直接 `@LuckyComponent` 时，按成员结构的 HTTP/静态函数特征推导。 */
    private fun inferLocalKind(psiClass: PsiClass): LuckyGroupKind? = when {
        psiClass.methods.any { !it.isConstructor && LuckyHttpAnnotations.httpMethodOf(it) != null } ->
            LuckyGroupKind.API

        psiClass.methods.any { it.isPublicStaticFunction() } -> LuckyGroupKind.FUNCTION
        else -> null
    }

    /** 当前类是否是标准客户端：`@StdHttpClient` 或其组合注解（含 `@ApiConfig`）。 */
    private fun isStdClient(psiClass: PsiClass): Boolean =
        annotationsOf(psiClass).any { annotation ->
            annotation.qualifiedName == API_CONFIG ||
                annotation.nameReferenceElement?.referenceName == "ApiConfig" ||
                runCatching { StdAnnotationValues.carriesStdHttpClient(annotation) }.getOrDefault(false)
        }

    /** 两个版本 `@LuckyComponent` 的全限定名（3.x 元注解与 starter 2.x legacy）。 */
    private fun luckyComponentOf(psiClass: PsiClass): PsiAnnotation? =
        annotationsOf(psiClass).firstOrNull { annotation ->
            annotation.qualifiedName == LUCKY_COMPONENT ||
                annotation.qualifiedName == LEGACY_LUCKY_COMPONENT
        }

    /** 注解按全限定名匹配；类尚未解析时按简单名退化。 */
    private fun hasAnnotation(owner: PsiClass, fqName: String, simpleName: String): Boolean =
        annotationsOf(owner).any {
            it.qualifiedName == fqName ||
                (it.qualifiedName == null && it.nameReferenceElement?.referenceName == simpleName)
        }

    private fun annotationsOf(psiClass: PsiClass): List<PsiAnnotation> =
        psiClass.modifierList?.annotations?.toList().orEmpty()

    /** `@LuckyComponent#type` 的枚举名；未声明或旧版本注解没有该属性时为 AUTO。 */
    private fun typeOf(annotation: PsiAnnotation): String {
        val value = runCatching { annotation.findAttributeValue("type") }.getOrNull() ?: return AUTO
        val text = when (value) {
            is PsiReferenceExpression -> value.referenceName ?: value.text
            is PsiLiteralExpression -> value.value as? String
            else -> value.text
        } ?: return AUTO
        return text.substringAfterLast('.').trim('"').takeIf { it.isNotBlank() } ?: AUTO
    }

    private fun infer(psiClass: PsiClass): LuckyGroupKind = when {
        psiClass.methods.any { !it.isConstructor && LuckyHttpAnnotations.httpMethodOf(it) != null } ->
            LuckyGroupKind.API

        psiClass.methods.any { it.isPublicStaticFunction() } -> LuckyGroupKind.FUNCTION
        else -> LuckyGroupKind.OTHER
    }

    private fun PsiMethod.isPublicStaticFunction(): Boolean =
        !isConstructor &&
            hasModifierProperty(PsiModifier.PUBLIC) &&
            hasModifierProperty(PsiModifier.STATIC)

    private val KIND_KEY: Key<CachedValue<LuckyGroupKind?>> =
        Key.create("lucky.component.kind.cache")

    private const val API_CONFIG = "com.luckyframework.httpclient.proxy.configapi.ApiConfig"
    private const val LEGACY_HTTP_CLIENT = "io.github.lucklike.httpclient.discovery.HttpClient"
    private const val LUCKY_COMPONENT = "com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent"
    private const val LEGACY_LUCKY_COMPONENT = "io.github.lucklike.httpclient.annotation.LuckyComponent"
    private const val AUTO = "AUTO"
}
