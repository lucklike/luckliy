package com.luckyframework.idea.spel.settings

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.IconUtil
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.component.ComponentIconResolver
import com.luckyframework.idea.std.StdHttpClientIndex
import java.awt.BorderLayout
import java.io.File
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

/**
 * 设置页：SpEL 编辑支持总开关，以及 `@LuckyComponent` 组件类图标配置。
 *
 * 图标既可指定本地 SVG/PNG/JPG/GIF 文件，也可关闭替换沿用平台默认类图标；
 * 应用后重启代码分析并刷新项目视图，使开关与图标立即生效。
 */
class LuckyHttpClientConfigurable : SearchableConfigurable {

    private val log = Logger.getInstance(LuckyHttpClientConfigurable::class.java)

    private val enabledBox = JBCheckBox("启用 lucky-httpclient 的 SpEL 编辑支持")

    private val enabledHint = hint("关闭后高亮、补全、跳转与检查立即失效，不会修改任何源码。")

    private val environmentPreviewBox = JBCheckBox("将 ${'$'}{key} 折叠显示为当前配置值")

    private val environmentPreviewHint =
        hint("仅对依赖 lucky-httpclient-spring-boot-starter 的模块生效；点击折叠区域可展开原表达式。")

    private val iconEnabledBox = JBCheckBox("替换 @LuckyComponent 组件类的图标")

    private val iconPathField = JBTextField()

    private val browseButton = JButton("选择图标文件…")

    private val resetIconButton = JButton("恢复默认")

    private val previewLabel = JLabel()

    private val iconHint = hint("留空使用插件内置图标（也可点「恢复默认」）；支持 SVG、PNG、JPG 文件。")

    private val stdScopeBox = JComboBox(arrayOf(SCOPE_LABEL_ALL, SCOPE_LABEL_APPLICATION))

    private val stdMissingBox = JBCheckBox("提示未找到关联的接口或配置")

    private val stdHint = hint("接口行标记与配置文件之间的双向跳转；缺失提示只影响行标记，不修改源码。")

    private val iconButtons = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        add(browseButton)
        add(Box.createHorizontalStrut(JBUI.scale(4)))
        add(resetIconButton)
    }

    private val iconPathRow = JPanel(BorderLayout()).apply {
        add(iconPathField, BorderLayout.CENTER)
        add(iconButtons, BorderLayout.EAST)
    }

    init {
        iconEnabledBox.addActionListener { updateIconControls() }
        browseButton.addActionListener { chooseIconFile() }
        resetIconButton.toolTipText = "清空自定义图标，使用插件内置图标"
        resetIconButton.addActionListener { restoreDefaultIcon() }
        iconPathField.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                updatePreview()
            }
        })
    }

    override fun getId(): String = ID

    override fun getDisplayName(): String = "Lucky HttpClient"

    override fun createComponent(): JComponent {
        reset()
        return FormBuilder.createFormBuilder()
            .addComponent(enabledBox)
            .addComponent(enabledHint)
            .addVerticalGap(8)
            .addComponent(environmentPreviewBox)
            .addComponent(environmentPreviewHint)
            .addVerticalGap(16)
            .addComponent(iconEnabledBox)
            .addLabeledComponent("图标文件：", iconPathRow)
            .addLabeledComponent("预览：", previewLabel)
            .addComponent(iconHint)
            .addVerticalGap(16)
            .addSeparator()
            .addComponent(JLabel("StdHttpClient 配置关联"))
            .addComponent(stdScopeBox)
            .addComponent(stdMissingBox)
            .addComponent(stdHint)
            .panel
            .apply { border = JBUI.Borders.empty(12) }
    }

    override fun isModified(): Boolean {
        val settings = LuckyHttpClientSettings.getInstance()
        return enabledBox.isSelected != settings.isEnabled ||
            environmentPreviewBox.isSelected != settings.isEnvironmentPreviewEnabled ||
            iconEnabledBox.isSelected != settings.isComponentIconEnabled ||
            iconPathField.text.trim() != settings.componentIconPath ||
            selectedScope() != settings.stdConfigFileScope ||
            stdMissingBox.isSelected != settings.stdConfigMissingHint
    }

    override fun apply() {
        val settings = LuckyHttpClientSettings.getInstance()
        settings.isEnabled = enabledBox.isSelected
        settings.isEnvironmentPreviewEnabled = environmentPreviewBox.isSelected
        settings.isComponentIconEnabled = iconEnabledBox.isSelected
        settings.componentIconPath = iconPathField.text.trim()
        settings.stdConfigFileScope = selectedScope()
        settings.stdConfigMissingHint = stdMissingBox.isSelected
        ComponentIconResolver.invalidate()
        ProjectManager.getInstance().openProjects
            .filterNot { it.isDisposed }
            .forEach { StdHttpClientIndex.getInstance(it).invalidate() }
        restartDaemons()
        refreshFolding()
        refreshProjectViews()
        log.info("[LuckyFold] apply global=${settings.isEnabled} preview=${settings.isEnvironmentPreviewEnabled}")
    }

    override fun reset() {
        val settings = LuckyHttpClientSettings.getInstance()
        enabledBox.isSelected = settings.isEnabled
        environmentPreviewBox.isSelected = settings.isEnvironmentPreviewEnabled
        iconEnabledBox.isSelected = settings.isComponentIconEnabled
        iconPathField.text = settings.componentIconPath
        stdScopeBox.selectedIndex =
            if (settings.stdConfigFileScope == LuckyHttpClientSettings.SCOPE_APPLICATION_ONLY) 1 else 0
        stdMissingBox.isSelected = settings.stdConfigMissingHint
        updateIconControls()
    }

    private fun selectedScope(): String =
        if (stdScopeBox.selectedIndex == 1) {
            LuckyHttpClientSettings.SCOPE_APPLICATION_ONLY
        } else {
            LuckyHttpClientSettings.SCOPE_ALL_YAML
        }

    private fun updateIconControls() {
        val selected = iconEnabledBox.isSelected
        iconPathField.isEnabled = selected
        browseButton.isEnabled = selected
        updatePreview()
    }

    private fun updatePreview() {
        if (!iconEnabledBox.isSelected) {
            previewLabel.icon = null
            previewLabel.text = "已关闭：使用平台默认类图标"
            return
        }
        val path = iconPathField.text.trim()
        if (path.isEmpty()) {
            showPreview(LuckyIcons.Component, "内置图标")
            return
        }
        val custom = ComponentIconResolver.preview(path)
        showPreview(
            custom ?: LuckyIcons.Component,
            if (custom == null) "无法读取该图标文件，将回退内置图标" else File(path).name
        )
    }

    /** 预览渲染失败时只显示文字，不能因为图标问题让整个设置页无法创建。 */
    private fun showPreview(icon: Icon, text: String) {
        previewLabel.icon = try {
            previewIcon(icon)
        } catch (e: Exception) {
            null
        }
        previewLabel.text = text
    }

    /**
     * 预览统一渲染到固定 32 逻辑尺寸。
     *
     * 使用平台缩放包装图标，避免把图标转成位图后调用 `Image#getScaledInstance`：
     * HiDPI 下位图的逻辑尺寸与像素栅格不一致，构造 `ImageIcon` 时会触发栅格越界，
     * 导致整个设置页创建失败。
     */
    private fun previewIcon(icon: Icon?): Icon? {
        icon ?: return null
        val width = icon.iconWidth
        if (width <= 0 || width == PREVIEW_SIZE) {
            return icon
        }
        return IconUtil.scale(icon, null, PREVIEW_SIZE.toFloat() / width)
    }

    private fun chooseIconFile() {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle("选择组件类图标")
            .withFileFilter { it.extension?.lowercase() in ICON_EXTENSIONS }
        val file = FileChooser.chooseFile(descriptor, null, null) ?: return
        iconPathField.text = file.path
    }

    /** 回到默认配置：开启图标替换并使用内置图标。 */
    private fun restoreDefaultIcon() {
        iconEnabledBox.isSelected = true
        iconPathField.text = ""
        updateIconControls()
    }

    private fun restartDaemons() {
        ProjectManager.getInstance().openProjects
            .filterNot { it.isDisposed }
            .forEach { DaemonCodeAnalyzer.getInstance(it).restart() }
    }

    /**
     * 让打开的编辑器重新运行折叠构建器。
     *
     * 折叠区域不随 daemon 重启重建，设置变化后必须显式刷新：关闭预览时
     * 本插件的 `${key}` 折叠立即移除并展开，开启后立即按配置值折叠。
     */
    private fun refreshFolding() {
        var editors = 0
        ProjectManager.getInstance().openProjects
            .filterNot { it.isDisposed }
            .forEach { project ->
                val foldingManager = CodeFoldingManager.getInstance(project)
                FileEditorManager.getInstance(project).allEditors
                    .filterIsInstance<TextEditor>()
                    .forEach {
                        editors++
                        foldingManager.updateFoldRegions(it.editor)
                    }
            }
        log.info("[LuckyFold] refreshFolding editors=$editors")
    }

    /** 让项目树与编辑器标签页重新查询元素图标，使新图标立即显示。 */
    private fun refreshProjectViews() {
        ProjectManager.getInstance().openProjects
            .filterNot { it.isDisposed }
            .forEach { project ->
                val projectView = ProjectView.getInstance(project)
                projectView.currentProjectViewPane?.updateFromRoot(true)
                projectView.refresh()
                refreshEditorTabIcons(project)
            }
        WindowManager.getInstance().allProjectFrames.forEach { it.component.repaint() }
    }

    /**
     * 编辑器标签页图标由 [com.intellij.ide.FileIconProvider] 提供，
     * 重新呈现打开的文件让平台重新计算 tab 图标。
     */
    private fun refreshEditorTabIcons(project: Project) {
        val manager = FileEditorManager.getInstance(project)
        manager.openFiles.forEach { manager.updateFilePresentation(it) }
    }

    private fun hint(text: String): JLabel = JLabel(text).apply {
        foreground = JBColor.GRAY
    }

    companion object {
        const val ID = "lucky.httpclient.spel.settings"

        private const val PREVIEW_SIZE = 32

        private const val SCOPE_LABEL_ALL = "全部 YAML 文件"

        private const val SCOPE_LABEL_APPLICATION = "仅 application/bootstrap"

        private val ICON_EXTENSIONS = setOf("svg", "png", "jpg", "jpeg", "gif")
    }
}
