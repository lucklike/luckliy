package com.luckyframework.idea.navigator

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
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

/** 侧边栏的固定分组；声明顺序即展示顺序，标题与组件类型枚举保持一致。 */
enum class LuckyGroupKind(val title: String) {
    STD_CLIENT("std"),
    API("httpclient"),
    DATABASE("database"),
    FUNCTION("function"),
    CONFIG("config"),
    SUPPORT("support"),
    OTHER("other")
}

/**
 * 容器节点类型：项目模块、外部库容器，或外部库中的单个依赖包。
 *
 * [LIBRARY] 是固定的「External Libraries」节点，[JAR] 是它下面的
 * 单个依赖库（通常是一个三方 jar），按库名区分组件来源。
 */
enum class LuckyProjectKind { MODULE, LIBRARY, JAR }

/** 顶层节点清单：项目模块按名称排序，外部依赖库固定排在最后。 */
data class LuckyNavigatorModel(val projects: List<LuckyProjectNode>)

/**
 * 一个容器节点下的完整树。
 *
 * [kind] 区分项目模块、外部库与依赖包：项目模块按内容根目录嵌套，
 * 依赖 jar 中的 Lucky 组件（框架内置 API/函数以及三方扩展）先归入
 * 「External Libraries」，再按 jar 分节点，不和当前项目的源码混在一起。
 * [children] 是嵌套的子模块或子依赖库。
 */
data class LuckyProjectNode(
    val name: String,
    val groups: List<LuckyGroupNode>,
    val kind: LuckyProjectKind = LuckyProjectKind.MODULE,
    val children: List<LuckyProjectNode> = emptyList()
) {

    /** 该节点自身分组下的组件数。 */
    val ownComponentCount: Int get() = groups.sumOf { it.components.size }

    /** 该节点及全部子节点下的组件总数，用于行内计数。 */
    val componentCount: Int get() = ownComponentCount + children.sumOf { it.componentCount }
}

/** 一个固定分组，按包名再分一级，组件行展示在包节点下。 */
data class LuckyGroupNode(val kind: LuckyGroupKind, val packages: List<LuckyPackageNode>) {

    /** 分组下的全部组件；需要平铺列表的调用方直接使用。 */
    val components: List<LuckyComponentNode> get() = packages.flatMap { it.components }
}

/** 分组内的包节点：[name] 是完整包名，无包声明的组件统一使用「默认包」。 */
data class LuckyPackageNode(val name: String, val components: List<LuckyComponentNode>)

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

    /**
     * 接口方法：[httpMethod] 为 GET/POST 等，未声明时为 null。
     *
     * HTTP 方法用 [subtitle] 展示路径；非 HTTP 方法用 [signature] 展示完整签名，
     * 避免大量重载方法在树中无法区分。
     */
    data class Api(
        override val name: String,
        val httpMethod: String?,
        override val subtitle: String?,
        val signature: String?,
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
 * 项目源码组件按所属模块分组，模块按内容根目录的嵌套关系组成层级；依赖
 * jar 中的组件归入 [EXTERNAL_LIBRARIES]，再按依赖库（jar）分类。没有任何
 * Lucky 组件且没有可见子节点的模块/依赖包不展示。
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
        if (sources.isEmpty()) {
            return LuckyNavigatorModel(emptyList())
        }

        val (librarySources, moduleSources) = sources.values
            .partition { isLibraryClass(project, it.psiClass) }
        val nodes = ArrayList<LuckyProjectNode>()
        if (moduleSources.isNotEmpty()) {
            nodes += moduleNodes(project, moduleSources)
        }
        if (librarySources.isNotEmpty()) {
            nodes += externalLibrariesNode(project, librarySources)
        }
        return LuckyNavigatorModel(nodes)
    }

    /**
     * 模块树。
     *
     * 模块的内容根目录互相嵌套时按层级展示，例如 `auth-server` 的内容根位于
     * `ldemo` 之下时作为 `ldemo` 的子节点。没有任何 Lucky 组件的模块会被
     * 剪掉；父模块即使自身没有组件，只要子模块有组件也会保留，用于承载层级。
     */
    private fun moduleNodes(project: Project, sources: List<LuckySource>): List<LuckyProjectNode> {
        val byModule = sources.groupBy { moduleNameOf(project, it.psiClass) }
        val locations = ModuleManager.getInstance(project).modules.mapNotNull { module ->
            ModuleRootManager.getInstance(module).contentRoots.firstOrNull()
                ?.let { LuckyModuleLocation(module.name, it.path) }
        }
        val parentOf = moduleParentsOf(locations)
        val childrenOf = parentOf.entries.groupBy({ it.value }, { it.key })
        val topLevel = LinkedHashSet<String>()
        topLevel += locations.map { it.name }.filter { it !in parentOf }
        // 没有内容根的源码模块（极少见）无法参与嵌套，作为顶层节点处理
        topLevel += byModule.keys.filter { name -> locations.none { it.name == name } }

        val visited = HashSet<String>()
        fun nodeOf(name: String): LuckyProjectNode? {
            if (!visited.add(name)) {
                return null
            }
            val children = childrenOf[name].orEmpty()
                .sortedWith(String.CASE_INSENSITIVE_ORDER)
                .mapNotNull { nodeOf(it) }
            val node = projectNode(
                project,
                name,
                byModule[name].orEmpty(),
                LuckyProjectKind.MODULE,
                children
            )
            return node.takeIf { it.componentCount > 0 }
        }

        return topLevel.sortedWith(String.CASE_INSENSITIVE_ORDER).mapNotNull { nodeOf(it) }
    }

    /**
     * 外部库树：`External Libraries` 下按依赖库（通常是一个三方 jar）分节点，
     * 每个 jar 节点下再按固定分组、包、组件展示。
     */
    private fun externalLibrariesNode(project: Project, sources: List<LuckySource>): LuckyProjectNode {
        val jars = sources
            .groupBy { libraryNameOf(project, it.psiClass) }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            .map { (name, entries) ->
                projectNode(project, name, entries, LuckyProjectKind.JAR)
            }
        return LuckyProjectNode(
            EXTERNAL_LIBRARIES,
            emptyList(),
            LuckyProjectKind.LIBRARY,
            jars
        )
    }

    /** 依赖库名称（如 `Maven: ...`）；拿不到库信息时回退到 jar 文件名。 */
    private fun libraryNameOf(project: Project, psiClass: PsiClass): String {
        val file = psiClass.containingFile?.virtualFile ?: return UNKNOWN_LIBRARY
        val name = runCatching {
            ProjectFileIndex.getInstance(project)
                .getOrderEntriesForFile(file)
                .filterIsInstance<LibraryOrderEntry>()
                .firstOrNull()
                ?.libraryName
        }.getOrNull()
        if (!name.isNullOrBlank()) {
            return name
        }
        val jarPath = file.path.substringBefore("!/", file.path)
        return jarPath.substringAfterLast('/').substringAfterLast('\\')
            .takeIf { it.endsWith(".jar", ignoreCase = true) }
            ?: UNKNOWN_LIBRARY
    }

    /** 依赖 jar 中的类归入外部库：包括内容根之外的依赖，以及项目内以库方式引入的 jar。 */
    private fun isLibraryClass(project: Project, psiClass: PsiClass): Boolean {
        val file = psiClass.containingFile?.virtualFile ?: return false
        val index = ProjectFileIndex.getInstance(project)
        return index.isInLibrary(file) || !index.isInContent(file)
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
        name: String,
        entries: List<LuckySource>,
        kind: LuckyProjectKind,
        children: List<LuckyProjectNode> = emptyList()
    ): LuckyProjectNode {
        val groups = entries
            .groupBy { it.kind }
            .toSortedMap(compareBy { it.ordinal })
            .map { (kind, list) ->
                val byPackage = list.map { it.psiClass }
                    .sortedBy { it.name ?: it.qualifiedName ?: "" }
                    .groupBy { packageOf(it) ?: DEFAULT_PACKAGE }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                LuckyGroupNode(
                    kind,
                    byPackage.map { (pkg, classes) ->
                        LuckyPackageNode(pkg, classes.map { componentNode(project, it, kind) })
                    }
                )
            }
        return LuckyProjectNode(name, groups, kind, children)
    }

    private fun componentNode(project: Project, psiClass: PsiClass, kind: LuckyGroupKind): LuckyComponentNode {
        val items = when (kind) {
            LuckyGroupKind.STD_CLIENT, LuckyGroupKind.API, LuckyGroupKind.DATABASE ->
                apiItemsOf(project, psiClass)
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
     * `@StdHttpClient` 接口的所有方法都会被代理成请求，请求方法按运行期规则
     * 取值（见 [httpMethodOf]），路径优先取方法上的注解 URL，其次回退到 YAML
     * 中的 `method-configs.<apiId>.path`，都没有时显示占位符。
     */
    private fun apiItemsOf(project: Project, psiClass: PsiClass): List<LuckyItemNode.Api> {
        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass)
        val apiIdByMethod = entry?.methods?.associate { it.method to it.apiId }.orEmpty()
        return psiClass.methods
            .filterNot { it.isConstructor }
            .filterNot { it.hasModifierProperty(PsiModifier.STATIC) }
            .map { method ->
                val apiId = apiIdByMethod[method] ?: method.name
                val httpMethod = httpMethodOf(method, entry, apiId)
                LuckyItemNode.Api(
                    name = method.name,
                    httpMethod = httpMethod,
                    subtitle = if (httpMethod == null) {
                        null
                    } else {
                        LuckyHttpAnnotations.urlOf(method) ?: yamlPathOf(entry, apiId) ?: EMPTY_HTTP_PATH
                    },
                    signature = if (httpMethod == null) methodSignatureOf(method) else null,
                    element = method
                )
            }
    }

    /**
     * 非 HTTP 方法的完整签名，格式与 Structure 视图一致：
     * `method(String, int): void`。
     */
    private fun methodSignatureOf(method: PsiMethod): String {
        val parameters = method.parameterList.parameters
            .joinToString(", ") { it.type.presentableText }
        val returnType = method.returnType?.presentableText ?: "void"
        return "${method.name}($parameters): $returnType"
    }

    /**
     * 方法行展示的请求方法。
     *
     * 标准客户端的所有方法都会被代理，`StandardLifeCycleManager#setRequestMethod`
     * 的取值顺序是：YAML 方法级/类级 `method` → 请求对象（方法注解）→ 默认 POST。
     * 普通接口只采用方法注解，未声明时保持未知。
     */
    private fun httpMethodOf(method: PsiMethod, entry: StdClientEntry?, apiId: String): String? {
        val annotationMethod = LuckyHttpAnnotations.httpMethodOf(method)
        if (entry == null) {
            return annotationMethod
        }
        return yamlHttpMethodOf(entry, apiId) ?: annotationMethod ?: DEFAULT_HTTP_METHOD
    }

    /** YAML 声明的请求方法：同一配置节点内方法级优先于类级，多个文件取首个声明值。 */
    private fun yamlHttpMethodOf(entry: StdClientEntry, apiId: String): String? =
        entry.configNodes.firstNotNullOfOrNull { it.apiMethods[apiId] ?: it.httpMethod }

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
        "DATABASE" -> LuckyGroupKind.DATABASE
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

    companion object {
        private const val FUNCTION_FILTER = "com.luckyframework.httpclient.proxy.spel.FunctionFilter"

        private const val NO_PATH = "-"

        /** HTTP 方法未声明路径时的展示值。 */
        private const val EMPTY_HTTP_PATH = "/"

        /** 标准客户端在未配置、也未标注请求方法时使用的默认方法。 */
        private const val DEFAULT_HTTP_METHOD = "POST"

        /** 无包声明的组件统一归入该包节点。 */
        const val DEFAULT_PACKAGE = "Default Package"

        /** 依赖 jar 中 Lucky 组件的顶层节点名称，与 Project 视图的命名保持一致。 */
        const val EXTERNAL_LIBRARIES = "External Libraries"

        /** 拿不到库信息时使用的依赖包名称。 */
        private const val UNKNOWN_LIBRARY = "Unknown Library"
    }
}

/** 模块在磁盘上的内容根，用于推导模块之间的父子关系。 */
internal data class LuckyModuleLocation(val name: String, val path: String)

/**
 * 按内容根目录的嵌套关系推导父模块：取路径最深的祖先模块。
 * 内容根相同或互不包含的模块没有父子关系。
 */
internal fun moduleParentsOf(modules: Collection<LuckyModuleLocation>): Map<String, String> =
    modules.mapNotNull { child ->
        modules.asSequence()
            .filter { it.name != child.name && isAncestorPath(it.path, child.path) }
            .maxByOrNull { it.path.length }
            ?.let { child.name to it.name }
    }.toMap()

/** 大小写不敏感的严格路径包含，兼容 Windows 的 `/` 与 `\`。 */
internal fun isAncestorPath(ancestor: String, path: String): Boolean {
    val parent = ancestor.trimEnd('\\', '/').replace('\\', '/')
    val child = path.trimEnd('\\', '/').replace('\\', '/')
    if (parent.equals(child, ignoreCase = true)) {
        return false
    }
    return child.startsWith("$parent/", ignoreCase = true)
}

/**
 * 导航树的搜索过滤。
 *
 * 大小写不敏感的包含匹配，匹配范围包括模块/依赖库名、包名、分组标题、
 * 组件名/全限定名/副标题，以及方法名/路径/请求方法。组件自身命中时保留
 * 它的全部方法，否则只保留命中的方法；没有任何命中的模块、依赖库、分组
 * 不展示，但保留通往命中子节点的父级层级。
 */
object LuckyNavigatorFilter {

    fun filter(model: LuckyNavigatorModel, query: String): LuckyNavigatorModel {
        val needle = query.trim().lowercase()
        if (needle.isEmpty()) {
            return model
        }
        return LuckyNavigatorModel(
            model.projects.mapNotNull { project -> filterContainer(project, needle) }
        )
    }

    private fun filterContainer(container: LuckyProjectNode, needle: String): LuckyProjectNode? {
        if (container.name.lowercase().contains(needle)) {
            return container
        }
        val groups = container.groups.mapNotNull { group -> filterGroup(group, needle) }
        val children = container.children.mapNotNull { child -> filterContainer(child, needle) }
        if (groups.isEmpty() && children.isEmpty()) {
            return null
        }
        return container.copy(groups = groups, children = children)
    }

    private fun filterGroup(group: LuckyGroupNode, needle: String): LuckyGroupNode? {
        if (group.kind.title.lowercase().contains(needle)) {
            return group
        }
        val packages = group.packages.mapNotNull { pkg ->
            val components = pkg.components.mapNotNull { filterComponent(it, needle, pkg.name) }
            if (components.isEmpty()) null else pkg.copy(components = components)
        }
        return if (packages.isEmpty()) null else group.copy(packages = packages)
    }

    private fun filterComponent(
        component: LuckyComponentNode,
        needle: String,
        packageName: String
    ): LuckyComponentNode? {
        val componentMatched = listOfNotNull(
            component.title,
            component.qualifiedName,
            component.subtitle,
            packageName
        ).any { it.lowercase().contains(needle) }
        if (componentMatched) {
            return component
        }
        val items = component.items.filter { matches(it, needle) }
        return if (items.isEmpty()) null else component.copy(items = items)
    }

    private fun matches(item: LuckyItemNode, needle: String): Boolean {
        val texts = when (item) {
            is LuckyItemNode.Api ->
                listOfNotNull(item.name, item.httpMethod, item.subtitle, item.signature)
            is LuckyItemNode.Function -> listOfNotNull(item.name, item.subtitle)
        }
        return texts.any { it.lowercase().contains(needle) }
    }
}
