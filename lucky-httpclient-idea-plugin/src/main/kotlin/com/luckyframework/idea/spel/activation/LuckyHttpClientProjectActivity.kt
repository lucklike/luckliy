package com.luckyframework.idea.spel.activation

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.ProjectActivity
import com.luckyframework.idea.spel.documentation.SpelLookupDocumentationBridge

/**
 * 项目启动后初始化激活状态，并在模块根变更时刷新缓存。
 *
 * 只做轻量初始化与事件订阅，不遍历项目、不解析用户源码。
 */
class LuckyHttpClientProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val presence = LuckyHttpClientPresence.getInstance(project)
        presence.invalidate()
        // 补全文档的 lookup 目标提供器需要运行时注册，见 SpelLookupDocumentationBridge
        SpelLookupDocumentationBridge.install(project)
        // 连接以 project 为 parent，项目关闭时自动释放
        project.messageBus.connect(project).subscribe(ModuleRootListener.TOPIC, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                presence.invalidate()
            }
        })
    }
}
