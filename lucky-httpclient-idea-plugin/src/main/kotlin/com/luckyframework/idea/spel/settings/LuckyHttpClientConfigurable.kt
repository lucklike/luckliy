package com.luckyframework.idea.spel.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * 设置页：全局启用/禁用 SpEL 编辑能力。
 *
 * 应用后重启所有已打开项目的代码分析，使高亮立即按新状态刷新。
 */
class LuckyHttpClientConfigurable : SearchableConfigurable {

    private val enabledBox = JBCheckBox("启用 lucky-httpclient 的 SpEL 编辑支持")

    private val hintLabel = JLabel(
        "关闭后高亮、补全、跳转与检查立即失效，不会修改任何源码。"
    ).apply {
        foreground = JBColor.GRAY
    }

    override fun getId(): String = ID

    override fun getDisplayName(): String = "Lucky HttpClient SpEL"

    override fun createComponent(): JComponent {
        reset()
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(12)
            add(enabledBox, BorderLayout.NORTH)
            add(hintLabel, BorderLayout.CENTER)
        }
    }

    override fun isModified(): Boolean = enabledBox.isSelected != LuckyHttpClientSettings.getInstance().isEnabled

    override fun apply() {
        LuckyHttpClientSettings.getInstance().isEnabled = enabledBox.isSelected
        restartDaemons()
    }

    override fun reset() {
        enabledBox.isSelected = LuckyHttpClientSettings.getInstance().isEnabled
    }

    private fun restartDaemons() {
        ProjectManager.getInstance().openProjects
            .filterNot { it.isDisposed }
            .forEach { DaemonCodeAnalyzer.getInstance(it).restart() }
    }

    companion object {
        const val ID = "lucky.httpclient.spel.settings"
    }
}
