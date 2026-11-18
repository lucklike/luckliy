package com.luckyframework.idea.spel.activation

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.ProjectActivity
import com.luckyframework.idea.spel.component.LuckyComponentIconRefresher
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
    }
}
