package com.luckyframework.idea.navigator

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence

/**
 * Lucky 侧边导航栏的 Tool Window。
 *
 * 只在至少一个模块能解析到 lucky-httpclient 注解时出现，与其他能力
 * 的依赖感知激活保持一致；面板本身是 DumbAware 的，索引期间展示提示，
 * 索引结束后自动刷新。
 */
class LuckyNavigatorToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun shouldBeAvailable(project: Project): Boolean {
        // 平台只在项目打开时评估一次可用性：索引构建期间无法探测依赖，
        // 此时先显示，面板会先展示「索引构建中」并在就绪后自动刷新。
        if (DumbService.isDumb(project)) {
            return true
        }
        return runCatching {
            val presence = LuckyHttpClientPresence.getInstance(project)
            ModuleManager.getInstance(project).modules.any { presence.isEnabledFor(it) }
        }.getOrDefault(true)
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = LuckyNavigatorPanel(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(panel.component, null, false)
        // 面板订阅了 PSI 事件与定时器，content 关闭时一并释放
        Disposer.register(content, panel)
        toolWindow.setIcon(LuckyIcons.ToolWindow)
        toolWindow.contentManager.addContent(content)
    }
}
