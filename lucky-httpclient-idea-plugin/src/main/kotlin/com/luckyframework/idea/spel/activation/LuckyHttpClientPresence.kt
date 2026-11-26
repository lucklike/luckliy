package com.luckyframework.idea.spel.activation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.luckyframework.idea.LuckyModificationTrackers
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * 依赖感知激活：只有能在模块中解析到 lucky-httpclient 注解时，SpEL 能力才生效。
 *
 * 判定依据是 `HttpRequest` 注解类是否存在（源码模块或二进制依赖均可），
 * 比 library 名称或构建脚本文本更可靠。
 *
 * 缓存按模块保存，并记录 Java 声明结构计数与项目根/依赖模型版本号：注解类
 * 动态加入/移除或依赖变化后的下一次访问重新检测，避免全项目扫描。
 * 结构计数不受方法体编辑影响，打字不会让模块判定反复重算。
 * `DumbService.isDumb()` 期间一律视为未激活，保证索引构建期间不阻塞 IDE。
 */
@Service(Service.Level.PROJECT)
class LuckyHttpClientPresence(private val project: Project) {

    private class CacheEntry(val enabled: Boolean, val stamp: Long, val rootsStamp: Long)

    private val cache = ConcurrentHashMap<Module, CacheEntry>()

    /**
     * 统一的入口判断：全局开关 → dumb mode → 模块依赖。
     */
    fun isEnabledFor(element: PsiElement): Boolean {
        if (!LuckyHttpClientSettings.getInstance().isEnabled) {
            return false
        }
        if (DumbService.isDumb(project)) {
            return false
        }
        // 补全与意图在 EDT 上调用：按内容根解析模块，不能查 ProjectFileIndex
        val module = ModuleResolver.moduleOf(element) ?: return false
        return isEnabledFor(module)
    }

    /**
     * 补全场景：`position` 位于编辑器副本中，模块必须按原始文件判定。
     * 副本元素的 `virtualFile` 是 LightVirtualFile，查找模块会得到 null。
     */
    fun isEnabledForCopy(element: PsiElement, originalFile: PsiFile): Boolean {
        if (!LuckyHttpClientSettings.getInstance().isEnabled) {
            return false
        }
        if (DumbService.isDumb(project)) {
            return false
        }
        val module = ModuleResolver.moduleOf(originalFile)
            ?: ModuleResolver.moduleOf(element)
            ?: return false
        return isEnabledFor(module)
    }

    fun isEnabledFor(module: Module): Boolean {
        val stamp = LuckyModificationTrackers.structure(project).modificationCount
        val rootsStamp = LuckyModificationTrackers.roots(project)
        val cached = cache[module]
        if (cached != null && cached.stamp == stamp && cached.rootsStamp == rootsStamp) {
            return cached.enabled
        }
        val enabled = detect(module)
        cache[module] = CacheEntry(enabled, stamp, rootsStamp)
        return enabled
    }

    /** 模块结构变化时清空缓存。 */
    fun invalidate() {
        cache.clear()
    }

    /** 折叠调试：输出缓存命中情况与注解类解析结果，定位“模块未被识别”的原因。 */
    fun debugReport(module: Module): String {
        val enabled = isEnabledFor(module)
        return "presence=$enabled httpRequest=${detect(module)}"
    }

    private fun detect(module: Module): Boolean {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false)
        return JavaPsiFacade.getInstance(project).findClass(HTTP_REQUEST_ANNOTATION, scope) != null
    }

    companion object {
        private const val HTTP_REQUEST_ANNOTATION =
            "com.luckyframework.httpclient.proxy.annotations.HttpRequest"

        fun getInstance(project: Project): LuckyHttpClientPresence = project.service()
    }
}
