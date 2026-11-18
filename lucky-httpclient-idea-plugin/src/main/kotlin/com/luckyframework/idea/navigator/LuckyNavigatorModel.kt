package com.luckyframework.idea.navigator

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import com.luckyframework.idea.spel.symbol.SpelSymbol
import com.luckyframework.idea.spel.symbol.SpelSymbolKind
import com.luckyframework.idea.std.StdClientEntry
import com.luckyframework.idea.std.StdHttpClientIndex

/** 侧边栏的固定分组；声明顺序即展示顺序。 */
enum class LuckyGroupKind(val title: String) {
    STD_CLIENT("StdHttpClient 客户端"),
    API("HTTP 接口"),
    FUNCTION("SpEL 函数库"),
    CONFIG("配置模型"),
    SUPPORT("支撑组件"),
    OTHER("其他组件")
}

/** 一个模块（项目文件夹）下的完整树。 */
data class LuckyNavigatorModel(val projects: List<LuckyProjectNode>)

data class LuckyProjectNode(val name: String, val groups: List<LuckyGroupNode>)

data class LuckyGroupNode(val kind: LuckyGroupKind, val components: List<LuckyComponentNode>)

/**
 * 一个 Lucky 组件。
 *
 * [subtitle] 是行内灰色文字：HTTP 接口写类级 baseUrl，其余组件写包名，
 * 让同一分组内的组件仍然可区分。
 */
data class LuckyComponentNode(
    val title: String,
    val qualifiedName: String,
    val subtitle: String?,
    val psiClass: PsiClass,
    val items: List<LuckyItemNode>
)

/** 组件展开后的接口方法或函数。 */
sealed class LuckyItemNode {

    abstract val name: String

    abstract val subtitle: String?

    abstract val element: PsiElement

    /** 接口方法：[httpMethod] 为 GET/POST 等，未声明时为 null。 */
    data class Api(
        override val name: String,
        val httpMethod: String?,
        override val subtitle: String?,
        override val element: PsiElement
    ) : LuckyItemNode()

    /** SpEL 函数：灰色文字是方法签名或返回类型。 */
    data class Function(
        override val name: String,
        override val subtitle: String?,
        override val element: PsiElement
    ) : LuckyItemNode()
}

/**
 * 侧边栏模型的构建器。
 *
 * 组件来源三路并集（与补全、检查同源，避免同一份代码在不同能力里清单不一致）：
 *
 * 1. `@StdHttpClient` 接口：来自 [StdHttpClientIndex] 快照，单独分组；
 * 2. `@LuckyComponent` 类：来自 [SpelRuntimeSymbols]，按声明的类型分组；
 * 3. 注册的函数类：来自 [SpelRuntimeSymbols] 的函数清单，归入函数组。
 *
 * 扫描必须在 read action 中执行；dumb 模式或插件关闭时返回空模型。
 */
class LuckyNavigatorModelBuilder {

    /** 三路来源合并后的一项：同一全限定名只保留首次出现的分组与类实例。 */
    private class LuckySource(val psiClass: PsiClass, val kind: LuckyGroupKind)

    fun build(project: Project): LuckyNavigatorModel {
        if (!LuckyHttpClientSettings.getInstance().isEnabled || DumbService.isDumb(project)) {
            return LuckyNavigatorModel(emptyList())
        }
        val sources = LinkedHashMap<String, LuckySource>()
        collectStdClients(project, sources)
        collectComponents(project, sources)
        collectFunctionClasses(project, sources)
        return LuckyNavigatorModel(
            sources.values
                .groupBy { moduleNameOf(project, it.psiClass) }
                .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                .map { (module, entries) -> projectNode(project, module, entries) }
        )
    }

    /** `@StdHttpClient` 接口优先：同一类既被 `@HttpRequest` 标注又配置了标准客户端时仍归入独立组。 */
    private fun collectStdClients(project: Project, result: MutableMap<String, LuckySource>) {
        for (entry in StdHttpClientIndex.getInstance(project).snapshot().entries) {
            val name = entry.clientClass.qualifiedName ?: continue
            result.putIfAbsent(name, LuckySource(entry.clientClass, LuckyGroupKind.STD_CLIENT))
        }
    }

    private fun collectComponents(project: Project, result: MutableMap<String, LuckySource>) {
        for (meta in SpelRuntimeSymbols.components(project)) {
            val psiClass = findClass(project, meta.className) ?: continue
            // 注解声明本身是组件身份的载体，不作为侧边栏组件展示
            if (psiClass.isAnnotationType) {
                continue
            }
            result.putIfAbsent(
                meta.className,
                LuckySource(psiClass, groupKindOf(meta.type, psiClass))
            )
        }
    }

    private fun collectFunctionClasses(project: Project, result: MutableMap<String, LuckySource>) {
        val owners = SpelRuntimeSymbols.functions(project)
            .asSequence()
            .filter { it.kind == SpelSymbolKind.FUNCTION }
            .mapNotNull { it.owner }
            .distinct()
        for (owner in owners) {
            val psiClass = findClass(project, owner) ?: continue
            result.putIfAbsent(owner, LuckySource(psiClass, LuckyGroupKind.FUNCTION))
        }
    }

    private fun projectNode(
        project: Project,
        module: String,
        entries: List<LuckySource>
    ): LuckyProjectNode {
        val groups = entries
            .groupBy { it.kind }
            .toSortedMap(compareBy { it.ordinal })
            .map { (kind, list) ->
                LuckyGroupNode(
                    kind,
                    list.map { it.psiClass }
                        .sortedBy { it.name ?: it.qualifiedName ?: "" }
                        .map { componentNode(project, it, kind) }
                )
            }
        return LuckyProjectNode(module, groups)
    }

    private fun componentNode(project: Project, psiClass: PsiClass, kind: LuckyGroupKind): LuckyComponentNode {
        val items = when (kind) {
            LuckyGroupKind.STD_CLIENT, LuckyGroupKind.API -> apiItemsOf(project, psiClass)
            LuckyGroupKind.FUNCTION -> functionItemsOf(project, psiClass)
            else -> apiItemsOf(project, psiClass) + functionItemsOf(project, psiClass)
        }
        return LuckyComponentNode(
            title = psiClass.name ?: psiClass.qualifiedName.orEmpty(),
            qualifiedName = psiClass.qualifiedName.orEmpty(),
            subtitle = baseUrlOf(project, psiClass) ?: packageOf(psiClass),
            psiClass = psiClass,
            items = items
        )
    }

    /**
     * 组件行的灰色副标题：优先类上注解的 URL，`@StdHttpClient` 接口再回退到
     * YAML 配置 `standard-client-configs.<configId>.url`。
     */
    private fun baseUrlOf(project: Project, psiClass: PsiClass): String? =
        LuckyHttpAnnotations.urlOf(psiClass)
            ?: StdHttpClientIndex.getInstance(project).entryOf(psiClass)
                ?.configNodes
                ?.firstNotNullOfOrNull { it.baseUrl }

    /**
     * 接口方法：只取非静态、非构造方法。
     *
     * `@StdHttpClient` 接口的所有方法都会被代理成请求，没有 HTTP 注解时按
     * 「方法未知」展示：路径优先取方法上的注解 URL，其次回退到 YAML 中
     * `method-configs.<apiId>.path`，都没有时显示占位符。
     */
    private fun apiItemsOf(project: Project, psiClass: PsiClass): List<LuckyItemNode.Api> {
        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass)
        val apiIdByMethod = entry?.methods?.associate { it.method to it.apiId }.orEmpty()
        return psiClass.methods
            .filterNot { it.isConstructor }
            .filterNot { it.hasModifierProperty(PsiModifier.STATIC) }
            .map { method ->
                val apiId = apiIdByMethod[method] ?: method.name
                LuckyItemNode.Api(
                    name = method.name,
                    httpMethod = LuckyHttpAnnotations.httpMethodOf(method),
                    subtitle = LuckyHttpAnnotations.urlOf(method)
                        ?: yamlPathOf(entry, apiId)
                        ?: NO_PATH,
                    element = method
                )
            }
    }

    /** 方法在任一份匹配的 YAML 配置中声明的 path。 */
    private fun yamlPathOf(entry: StdClientEntry?, apiId: String): String? =
        entry?.configNodes?.firstNotNullOfOrNull { it.methodPaths[apiId] }

    /**
     * 函数项：优先使用注册函数清单（已应用 `@FunctionFilter` 与 `@FunctionAlias`），
     * 类不在注册清单中时降级为 public static 方法。
     */
    private fun functionItemsOf(project: Project, psiClass: PsiClass): List<LuckyItemNode.Function> {
        val registered = SpelRuntimeSymbols.functions(project)
            .filter {
                it.kind == SpelSymbolKind.FUNCTION &&
                    it.owner == psiClass.qualifiedName &&
                    it.declaration != null
            }
        if (registered.isNotEmpty()) {
            return registered.map { symbol ->
                LuckyItemNode.Function(
                    name = symbol.name,
                    subtitle = symbol.signature ?: symbol.type ?: NO_PATH,
                    element = symbol.declaration!!
                )
            }
        }
        return psiClass.methods
            .filter { it.isPublicStaticFunction() }
            .filterNot { method -> method.hasAnnotation(FUNCTION_FILTER) }
            .map { method ->
                LuckyItemNode.Function(
                    name = method.name,
                    subtitle = method.returnType?.presentableText ?: NO_PATH,
                    element = method
                )
            }
    }

    /** `@LuckyComponent#type` → 分组；AUTO 按结构推导。 */
    private fun groupKindOf(type: String, psiClass: PsiClass): LuckyGroupKind = when (type) {
        "API" -> LuckyGroupKind.API
        "FUNCTION" -> LuckyGroupKind.FUNCTION
        "CONFIG" -> LuckyGroupKind.CONFIG
        "SUPPORT" -> LuckyGroupKind.SUPPORT
        else -> inferKind(psiClass)
    }

    private fun inferKind(psiClass: PsiClass): LuckyGroupKind = when {
        psiClass.methods.any { !it.isConstructor && LuckyHttpAnnotations.httpMethodOf(it) != null } ->
            LuckyGroupKind.API

        psiClass.methods.any { it.isPublicStaticFunction() } -> LuckyGroupKind.FUNCTION
        else -> LuckyGroupKind.OTHER
    }

    private fun moduleNameOf(project: Project, psiClass: PsiClass): String =
        ModuleUtilCore.findModuleForPsiElement(psiClass)?.name ?: project.name

    private fun packageOf(psiClass: PsiClass): String? =
        psiClass.qualifiedName?.substringBeforeLast('.', "")?.takeIf { it.isNotEmpty() }

    private fun findClass(project: Project, fqName: String): PsiClass? = runCatching {
        JavaPsiFacade.getInstance(project).findClass(fqName, GlobalSearchScope.allScope(project))
    }.getOrNull()

    private fun PsiMethod.isPublicStaticFunction(): Boolean =
        !isConstructor &&
            hasModifierProperty(PsiModifier.PUBLIC) &&
            hasModifierProperty(PsiModifier.STATIC)

    private fun PsiMethod.hasAnnotation(fqName: String): Boolean =
        (modifierList?.annotations ?: emptyArray<PsiAnnotation>()).any { it.qualifiedName == fqName }

    private companion object {
        private const val FUNCTION_FILTER = "com.luckyframework.httpclient.proxy.spel.FunctionFilter"

        private const val NO_PATH = "-"
    }
}
