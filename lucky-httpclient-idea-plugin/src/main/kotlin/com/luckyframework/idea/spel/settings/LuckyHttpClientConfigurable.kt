package com.luckyframework.idea.spel.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBUI
import com.luckyframework.idea.spel.highlight.SpelColorDefaults
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
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

    private val resetButton = JButton("还原到初始化配置").apply {
        toolTipText = "恢复启用状态与 SpEL 语法配色为插件默认值"
        addActionListener { resetToDefaults() }
    }

    override fun getId(): String = ID

    override fun getDisplayName(): String = "Lucky HttpClient SpEL"

    override fun createComponent(): JComponent {
        reset()
        return JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(12)
            add(enabledBox, BorderLayout.NORTH)
            add(hintLabel, BorderLayout.CENTER)
            add(
                JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply { add(resetButton) },
                BorderLayout.SOUTH
            )
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

    /** 启用状态与 12 项 SpEL 配色一起恢复为插件默认值。 */
    private fun resetToDefaults() {
        val answer = Messages.showYesNoDialog(
            "将启用状态与 SpEL 配色恢复为插件默认值？",
            "还原到初始化配置",
            Messages.getQuestionIcon()
        )
        if (answer != Messages.YES) {
            return
        }
        LuckyHttpClientSettings.getInstance().isEnabled = true
        enabledBox.isSelected = true
        val manager = EditorColorsManager.getInstance()
        SpelColorDefaults.applyTo(manager.globalScheme)
        // 重新设置方案触发 COLOR_SCHEME_CHANGED，已打开的编辑器立即重绘
        manager.setGlobalScheme(manager.globalScheme)
        restartDaemons()
        Messages.showInfoMessage("已还原 SpEL 默认配置", "还原到初始化配置")
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
