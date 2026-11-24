package com.luckyframework.idea.spel.activation

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.ProjectActivity
import com.luckyframework.idea.debug.container.DebugContainerService
import com.luckyframework.idea.spel.component.LuckyComponentIconRefresher
import com.luckyframework.idea.spel.component.LuckyComponentLookupListener
import com.luckyframework.idea.spel.documentation.SpelLookupDocumentationBridge
import com.luckyframework.idea.spel.environment.SpringEnvironmentService

/**
 * 项目启动后初始化激活状态，并在模块根变更时刷新缓存。
 *
 * 只做轻量初始化与事件订阅，不遍历项目、不解析用户源码。
 *
 * 组件图标依赖注解扫描：首次打开项目时索引/依赖模型未就绪会先按空结果渲染，
 * 因此订阅索引完成与模块根变化，在扫描可用后刷新项目树与标签页图标。
 */
class LuckyHttpClientProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val presence = LuckyHttpClientPresence.getInstance(project)
        presence.invalidate()
        val environment = SpringEnvironmentService.getInstance(project)
        environment.invalidate()
        // 补全文档的 lookup 目标提供器需要运行时注册，见 SpelLookupDocumentationBridge
        SpelLookupDocumentationBridge.install(project)
        // 补全列表图标不经过 IconProvider：lookup 创建后注入一次，
        // 继续输入时新进入列表的候选则在文档变更后补齐
        val lookupIcons = LuckyComponentLookupListener(project)
        project.messageBus.connect(project)
            .subscribe(LookupManagerListener.TOPIC, lookupIcons)
        EditorFactory.getInstance().getEventMulticaster()
            .addDocumentListener(lookupIcons, project)
        val iconRefresher = LuckyComponentIconRefresher.getInstance(project)
        // 连接以 project 为 parent，项目关闭时自动释放
        project.messageBus.connect(project).subscribe(ModuleRootListener.TOPIC, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                presence.invalidate()
                environment.invalidate()
                // 依赖导入/模块根变化会改变组件扫描结果，图标需要重新计算
                iconRefresher.scheduleRefresh()
            }
        })
        // 首次打开项目时图标早于索引完成渲染，索引结束后补一次刷新
        project.messageBus.connect(project).subscribe(DumbService.DUMB_MODE, object : DumbService.DumbModeListener {
            override fun exitDumbMode() {
                iconRefresher.scheduleRefresh()
            }
        })
        iconRefresher.scheduleRefresh()
        installDebugMarkerRefresh(project)
    }

    /**
     * 调试容器进入/离开 READY 时重启后台高亮。
     *
     * 编辑器方法行的小飞机图标只在容器运行中展示，而行标记结果会被高亮缓存，
     * 不主动重启 daemon 就会停留在旧状态。日志追加也会触发容器通知，因此只在
     * READY 边界重算，避免容器每输出一行日志就重启一次高亮。
     */
    private fun installDebugMarkerRefresh(project: Project) {
        val containerService = DebugContainerService.getInstance(project)
        val lastReady = HashMap<String, Boolean>()
        containerService.addListener({ module ->
            val ready = containerService.snapshot(module)?.state == DebugContainerService.State.READY
            val wasReady = synchronized(lastReady) {
                lastReady.put(module.name, ready)
            } ?: false
            if (ready == wasReady) {
                return@addListener
            }
            ApplicationManager.getApplication().invokeLater {
                if (!project.isDisposed) {
                    DaemonCodeAnalyzer.getInstance(project).restart()
                }
            }
        }, project)
    }
}
