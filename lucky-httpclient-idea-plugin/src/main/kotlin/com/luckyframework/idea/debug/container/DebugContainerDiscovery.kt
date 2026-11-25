package com.luckyframework.idea.debug.container

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import com.luckyframework.idea.std.StdHttpClientIndex

/** 一个可运行容器：模块自己声明了 Spring Boot 主类，且模块内有 Lucky 组件。 */
data class DiscoveredContainer(val moduleName: String, val mainClass: String)

/**
 * 列出项目中所有可运行的调试容器。
 *
 * 判定规则与 [DebugContainerService] 启动时一致：带 `@SpringBootApplication`
 * 且声明 public static main 方法，主类必须位于该模块自己的源码中（依赖模块里的
 * 主类不能代表当前模块启动）。除此之外，模块内必须存在 Lucky 组件
 * （`@LuckyComponent` 组件或 `@StdHttpClient` 接口）；没有任何 Lucky 组件的
 * 普通 Spring Boot 模块不是本插件要调试的容器，不进入列表。一个模块有多个
 * 候选时只保留排序后的第一个，与启动时的默认主类保持一致。
 *
 * 扫描依赖索引，必须在读锁中调用；索引未就绪时返回 null，由调用方在智能模式
 * 结束后重试，不能把空结果当成「没有可运行容器」。
 */
object DebugContainerDiscovery {

    fun discover(project: Project): List<DiscoveredContainer>? {
        if (DumbService.isDumb(project)) {
            return null
        }
        val annotation = JavaPsiFacade.getInstance(project)
            .findClass(BootMainClassLocator.ANNOTATION, GlobalSearchScope.allScope(project))
            ?: return emptyList()
        val luckyModules = luckyModules(project)
        return AnnotatedElementsSearch.searchPsiClasses(annotation, GlobalSearchScope.projectScope(project))
            .findAll()
            .asSequence()
            .filter { it.isValid && it.qualifiedName != null }
            .filter { BootMainClassLocator.hasMainMethod(it) }
            .mapNotNull { psiClass ->
                val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return@mapNotNull null
                DiscoveredContainer(module.name, psiClass.qualifiedName!!)
            }
            .filter { it.moduleName in luckyModules }
            .sortedWith(compareBy({ it.moduleName.lowercase() }, { it.mainClass }))
            .distinctBy { it.moduleName }
            .toList()
    }

    /**
     * 含 Lucky 组件的模块。
     *
     * 组件清单复用侧边栏的扫描结果（`@LuckyComponent` 及其元注解传播、
     * `@HttpRequest` 等接口注解）；`@StdHttpClient` 接口由独立索引提供。
     */
    private fun luckyModules(project: Project): Set<String> {
        val modules = HashSet<String>()
        val facade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        for (component in SpelRuntimeSymbols.components(project)) {
            val psiClass = facade.findClass(component.className, scope) ?: continue
            ModuleUtilCore.findModuleForPsiElement(psiClass)?.let { modules += it.name }
        }
        for (entry in StdHttpClientIndex.getInstance(project).snapshot().entries) {
            ModuleUtilCore.findModuleForPsiElement(entry.clientClass)?.let { modules += it.name }
        }
        return modules
    }
}
