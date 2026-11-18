package com.luckyframework.idea.spel.environment

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiModificationTracker
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * `${...}` 环境变量能力的激活与数据入口。
 *
 * 只有同时满足以下条件才生效：
 * 1. 插件总开关打开；
 * 2. 当前模块依赖 lucky-httpclient（复用 [LuckyHttpClientPresence]）；
 * 3. 模块 classpath 上存在 `lucky-httpclient-spring-boot-starter` 的
 *    `SpringSpELConvert`：只有该 starter 才会在 SpEL 求值前解析 `${...}`。
 *
 * 配置快照按模块缓存，并随 PSI 修改计数失效；索引构建期间一律视为未激活，
 * 避免在 Dumb mode 访问索引。
 */
@Service(Service.Level.PROJECT)
class SpringEnvironmentService(private val project: Project) {

    private class CacheEntry(
        val starterPresent: Boolean,
        val model: SpringEnvironmentModel,
        val stamp: Long
    )

    private val cache = ConcurrentHashMap<Module, CacheEntry>()

    private val index = SpringPropertyIndex()

    fun isEnabledFor(element: PsiElement): Boolean {
        if (!LuckyHttpClientSettings.getInstance().isEnabled || DumbService.isDumb(project)) {
            return false
        }
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false
        return isEnabledFor(module)
    }

    /** 补全位置位于编辑器副本，模块要按原始文件判定。 */
    fun isEnabledForCopy(element: PsiElement, originalFile: PsiFile): Boolean {
        if (!LuckyHttpClientSettings.getInstance().isEnabled || DumbService.isDumb(project)) {
            return false
        }
        val module = ModuleUtilCore.findModuleForPsiElement(originalFile)
            ?: ModuleUtilCore.findModuleForPsiElement(element)
            ?: return false
        return isEnabledFor(module)
    }

    fun isEnabledFor(module: Module): Boolean {
        if (!LuckyHttpClientSettings.getInstance().isEnabled || DumbService.isDumb(project)) {
            return false
        }
        if (!LuckyHttpClientPresence.getInstance(project).isEnabledFor(module)) {
            return false
        }
        return entry(module).starterPresent
    }

    fun modelFor(element: PsiElement): SpringEnvironmentModel? {
        if (!isEnabledFor(element)) {
            return null
        }
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
        return entry(module).model
    }

    /** 补全位置在编辑器副本中，模块按原始文件解析。 */
    fun modelForCopy(element: PsiElement, originalFile: PsiFile): SpringEnvironmentModel? {
        if (!isEnabledForCopy(element, originalFile)) {
            return null
        }
        val module = ModuleUtilCore.findModuleForPsiElement(originalFile)
            ?: ModuleUtilCore.findModuleForPsiElement(element)
            ?: return null
        return entry(module).model
    }

    fun modelFor(module: Module): SpringEnvironmentModel = entry(module).model

    /**
     * 折叠调试信息：一次性输出激活条件、模块与配置快照规模。
     *
     * 仅供 [SpringPlaceholderFoldingBuilder] 输出到 idea.log，
     * 用来定位“为什么没有折叠”：总开关、预览开关、模块识别、依赖标记。
     */
    fun debugReport(element: PsiElement): String {
        val settings = LuckyHttpClientSettings.getInstance()
        val module = ModuleUtilCore.findModuleForPsiElement(element)
        val report = StringBuilder("global=").append(settings.isEnabled)
            .append(" preview=").append(settings.isEnvironmentPreviewEnabled)
            .append(" dumb=").append(DumbService.isDumb(project))
            .append(" module=").append(module?.name ?: "<null>")
        if (module != null && settings.isEnabled && !DumbService.isDumb(project)) {
            report.append(' ').append(LuckyHttpClientPresence.getInstance(project).debugReport(module))
            val cacheEntry = entry(module)
            report.append(" starter=").append(cacheEntry.starterPresent)
                .append(" keys=").append(cacheEntry.model.keys().size)
                .append(" hasServerPort=").append(cacheEntry.model.resolveValue("server.port") != null)
        }
        return report.toString()
    }

    fun invalidate() {
        cache.clear()
    }

    private fun entry(module: Module): CacheEntry {
        val stamp = PsiModificationTracker.getInstance(project).modificationCount
        cache[module]?.takeIf { it.stamp == stamp }?.let { return it }
        if (DumbService.isDumb(project)) {
            return CacheEntry(false, emptyModel(), stamp)
        }
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, true)
        val starterPresent = JavaPsiFacade.getInstance(project)
            .findClass(STARTER_MARKER, scope) != null
        val model = if (starterPresent) {
            SpringEnvironmentModel(index.snapshot(module))
        } else {
            emptyModel()
        }
        val entry = CacheEntry(starterPresent, model, stamp)
        cache[module] = entry
        return entry
    }

    private fun emptyModel(): SpringEnvironmentModel =
        SpringEnvironmentModel(SpringPropertySnapshot(emptyMap(), emptyMap(), emptyList()))

    companion object {
        /** lucky-httpclient-spring-boot-starter 提供的 SpEL 转换器，作为依赖标记。 */
        private const val STARTER_MARKER = "io.github.lucklike.httpclient.SpringSpELConvert"

        fun getInstance(project: Project): SpringEnvironmentService = project.service()
    }
}
