package com.luckyframework.idea.debug.ui

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.intellij.icons.AllIcons
import com.intellij.json.JsonFileType
import com.intellij.json.editor.folding.JsonFoldingBuilder
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.Alarm
import com.intellij.util.IconUtil
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.luckyframework.idea.debug.container.DebugContainerService
import com.luckyframework.idea.debug.invoke.DebugArgumentCodec
import com.luckyframework.idea.debug.invoke.DebugCallExecutor
import com.luckyframework.idea.debug.invoke.DebugCallOutcome
import com.luckyframework.idea.debug.invoke.DebugJsonDocumentation
import com.luckyframework.idea.debug.invoke.DebugJsonFormatter
import com.luckyframework.idea.debug.invoke.DebugJsonPsiCopySupport
import com.luckyframework.idea.debug.invoke.DebugJsonSyntax
import com.luckyframework.idea.debug.invoke.DebugParameter
import com.luckyframework.idea.debug.invoke.DebugParameterKind
import com.luckyframework.idea.debug.invoke.DebugTarget
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.RenderingHints
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import java.io.File
import java.nio.file.Files
import java.util.Base64
import javax.swing.BoxLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 接口调试面板：参数表单 + 执行 + 结果/日志。
 *
 * 容器生命周期只使用 [DebugContainerService.start]，调用只使用
 * [DebugContainerService.invoke]；状态与日志通过只读 [DebugContainerService.snapshot]
 * 获取，用于展示进度与失败原因。
 *
 * `start` 会阻塞（编译 + ReadAction + 进程启动），因此按钮触发的所有服务调用都在
 * [AppExecutorUtil] 的后台线程执行，EDT 只负责渲染。[execute] 在容器
 * 未就绪时先启动容器，再发送 invoke 请求。
 *
 * 面板不注册到 plugin.xml；由导航栏作为 Debug 页内的子标签按需创建，
 * 关闭时通过 [onClose] 收起，不弹出独立对话框。
 *
 * 顶部是两行头部：第一行标题行（左侧当前类名，右侧收起按钮），第二行是
 * 圆角地址输入框——最左端是候选方法三角与组件/方法图标，中间展示当前维度
 * 内容，右端依次是执行与更多，签名等细节放在 tooltip。
 * 展示维度（由“更多”菜单切换、随设置持久化）：默认方法维度为方法图标 +
 * 全限定方法路径，URL 维度为 HTTP 方法徽标 + 完整地址。
 */
class DebugNavigatorPanel(
    private val project: Project,
    val target: DebugTarget,
    private val siblings: List<DebugTarget> = emptyList(),
    private val onSwitchTarget: ((DebugTarget) -> Unit)? = null,
    private val onClose: (() -> Unit)? = null,
    /** 嵌入标签页时由标签页头承担标题与关闭，面板内部不再重复一行。 */
    private val showTitleRow: Boolean = true
) : Disposable {

    /** 标签页标题：只展示方法名，类名与完整签名放在 tooltip。 */
    val tabTitle: String = target.methodName

    private val module: Module? =
        ModuleManager.getInstance(project).findModuleByName(target.moduleName)

    private val service: DebugContainerService = DebugContainerService.getInstance(project)

    private val settings: LuckyHttpClientSettings = LuckyHttpClientSettings.getInstance()

    private val executor = DebugCallExecutor { request, timeout ->
        val current = module ?: throw IllegalStateException("Module not found: ${target.moduleName}")
        service.invoke(current, request, timeout)
    }

    private val alarmDisposable = Disposer.newDisposable("lucky.debug.panel")

    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, alarmDisposable)

    /** 下拉候选：调用方未提供同一组件的其它方法时退化为只展示当前目标。 */
    private val targets: List<DebugTarget> = if (siblings.isEmpty()) listOf(target) else siblings

    /** 收起按钮：由导航栏把该调试页从标签栏移除。 */
    private val closeButton = iconButton(AllIcons.Actions.Close, "Hide the debug panel").apply {
        isVisible = onClose != null
        addActionListener { onClose?.invoke() }
    }

    /** 「更多」按钮：原先独占一行的参数开关收进菜单，纯图案无按钮边框。 */
    private val moreButton = iconButton(AllIcons.Actions.More, "More options").apply {
        accessibleContext.accessibleName = "More options"
        addActionListener { showMorePopup() }
    }

    /** 标题行左侧：当前接口的类名；只显示简单类名，hover 显示全限定名。 */
    private val classNameLabel = JBLabel(target.className.substringAfterLast('.')).apply {
        font = font.deriveFont(font.size2D - 1f)
        foreground = UIUtil.getLabelDisabledForeground()
        toolTipText = target.className
        border = JBUI.Borders.emptyLeft(2)
    }

    /**
     * 地址栏上方的标题行：左侧类名，右侧收起按钮。
     *
     * 收起按钮从地址栏移出后，地址栏右侧只剩执行与更多两个操作。
     */
    private val titleRow = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.empty(0, 2, 2, 0)
        add(classNameLabel, BorderLayout.CENTER)
        add(closeButton, BorderLayout.EAST)
        isVisible = showTitleRow
    }

    /** 最近一次状态提示（启动中/失败/自动重启挂起）；只放进请求路径的 tooltip。 */
    private var panelHint: String? = null

    /** 头部 URL 维度的纯文本形式；tooltip 提示与测试直接读取。 */
    private var urlText: String = target.displayName

    /** 完整 URL 的两段：灰色 baseUrl 段与正常色的接口 path 段。 */
    private var urlBaseText: String = ""
    private var urlPathText: String = target.displayName

    /** 头部图标是否为 HTTP 方法徽标；方法维度与未识别方法时回退平台方法图标。 */
    private var headerShowsHttpMethodIcon = false

    /**
     * 输入框中部：URL 维度为完整请求路径（baseUrl 灰显 + 接口 path 正常色），
     * 方法维度为方法全路径；没有路径信息时回退完整方法名。
     */
    private val urlLabel = RequestUrlLabel().apply {
        minimumSize = Dimension(0, 0)
        font = font.deriveFont(font.size2D + URL_FONT_DELTA)
        foreground = INPUT_TEXT
        toolTipText = targetTooltip()
        border = JBUI.Borders.emptyLeft(6)
    }

    /**
     * 请求方法徽标：URL 维度按请求方法着色并放大到与行内按钮同高；
     * 方法维度换成绿色组件图标（原尺寸 16px 同样与按钮同高，切换不跳行高）。
     */
    private val methodIcon = JBLabel().apply {
        toolTipText = target.displayName
    }

    /** 执行中的转圈图标；尺寸与纸飞机一致，忙碌时临时替换图标。 */
    private val busySpinner = BusySpinnerIcon(LuckyIcons.Send.iconWidth)

    /** 执行当前接口方法：容器未就绪时在 Result 页提示并提供启动图标。 */
    private val executeButton = iconButton(
        LuckyIcons.Send,
        "Invoke the selected method; the container must be running first"
    ).apply {
        accessibleContext.accessibleName = "Execute"
        addActionListener { execute() }
    }

    /** 候选方法下拉：三角常驻，单方法时也可以打开列表确认当前目标。 */
    private val targetMenuButton = iconButton(
        AllIcons.General.ChevronDown,
        "Select a method of this component"
    ).apply {
        accessibleContext.accessibleName = "Select method"
        addActionListener { showTargetPopup() }
    }

    /**
     * 纯图案按钮：去掉按钮边框与内边距，并锁定为图标尺寸。
     *
     * 平台默认按钮边框即使不绘制也会贡献 insets，把相邻图标之间的空隙撑大。
     */
    private fun iconButton(icon: Icon, tooltip: String): JButton = JButton(icon).apply {
        toolTipText = tooltip
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusable = false
        margin = JBUI.insets(0)
        border = JBUI.Borders.empty()
        preferredSize = Dimension(icon.iconWidth, icon.iconHeight)
    }

    /**
     * 顶部地址输入框：纯色圆角底 + 细边框，最左端是候选方法三角与组件/方法
     * 图标，中间方法路径，右端是执行与更多；点击输入框任意位置都弹出方法候选。
     *
     * 图标与按钮都放在输入框内部：WEST/EAST 用 [GridBagLayout] 承载，
     * 内容在拉伸后的行高里垂直居中（FlowLayout 会把单行内容顶到上沿）。
     */
    private val addressPanel = object : JPanel(BorderLayout()) {
        override fun paintComponent(g: Graphics) {
            if (width <= 0 || height <= 0) return
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                val arc = JBUI.scale(INPUT_ARC).toFloat()
                val shape = RoundRectangle2D.Float(
                    0.5f, 0.5f, (width - 1).toFloat(), (height - 1).toFloat(), arc, arc
                )
                g2.color = INPUT_BACKGROUND
                g2.fill(shape)
                g2.color = INPUT_BORDER
                g2.draw(shape)
            } finally {
                g2.dispose()
            }
        }
    }.apply {
        isOpaque = false
        border = JBUI.Borders.empty(8, 8, 8, 8)
        add(
            JPanel(GridBagLayout()).apply {
                isOpaque = false
                // 候选三角贴在输入框最左端，后面才是组件/方法图标
                add(targetMenuButton, actionSpacingConstraints(leftInset = 0))
                add(methodIcon, actionSpacingConstraints())
            },
            BorderLayout.WEST
        )
        add(urlLabel, BorderLayout.CENTER)
        add(
            JPanel(GridBagLayout()).apply {
                isOpaque = false
                add(executeButton, actionSpacingConstraints())
                add(moreButton, actionSpacingConstraints())
            },
            BorderLayout.EAST
        )
        if (targets.size > 1) {
            // 名称标签带 tooltip 会自己接收鼠标事件，打开候选列表的监听要同时挂在标签上
            val openPopup = object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        showTargetPopup()
                    }
                }
            }
            val hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            cursor = hand
            urlLabel.cursor = hand
            methodIcon.cursor = hand
            addMouseListener(openPopup)
            urlLabel.addMouseListener(openPopup)
            methodIcon.addMouseListener(openPopup)
        }
    }

    /** 输入框内图标与按钮的间距约束：垂直居中，与左侧相邻元素留 8px。 */
    private fun actionSpacingConstraints(leftInset: Int = 8): GridBagConstraints = GridBagConstraints().apply {
        anchor = GridBagConstraints.CENTER
        insets = JBUI.insets(0, leftInset, 0, 0)
    }

    /**
     * 参数开关不再直接布局，而是作为「更多」菜单里的勾选项。
     * 复选框只保留状态与既有监听逻辑，供测试和菜单读写；
     * 初始值由参数结构决定：简单类型走表单，对象/集合直接走 JSON。
     */
    private val jsonCheckBox = JCheckBox("JSON arguments").apply {
        toolTipText = "Edit the whole argument list as a JSON object or array"
        addActionListener { switchArgumentMode() }
    }

    /** Web 开关同样收进「更多」菜单，复选框仅承载状态。 */
    private val webServerCheckBox = JCheckBox("Web server", settings.isDebugWebServerEnabled).apply {
        toolTipText = "Start the embedded web server for this container. " +
            "Disabled by default: the container runs as a non-web Spring application. " +
            "Takes effect on the next container start."
        addActionListener { onWebServerToggled() }
    }

    /** 已就绪容器切换 Web 开关后的提示；下一次显式启动时清除。 */
    @Volatile
    private var webServerHint: String? = null

    /** 参数表单：背景取编辑器 scheme，与 Result/Logs 的正文一致。 */
    private val parameterPanel = JPanel(GridBagLayout()).apply {
        isOpaque = true
        background = editorBackground()
    }

    private val parameterScroll = JBScrollPane(parameterPanel).apply {
        viewport.isOpaque = true
        viewport.background = editorBackground()
    }

    /** JSON 折叠构建器；无状态，整个面板复用一个实例。 */
    private val jsonFoldingBuilder = JsonFoldingBuilder()

    /**
     * JSON 编辑器背后的虚拟 PSI 文件。
     *
     * 折叠区域由 JSON 语言的 folding builder 基于 PSI 构建；
     * 纯文本 document 没有 PsiFile，编辑器只会高亮、不会出现折叠箭头。
     * 字段文档索引也挂在这个文件上，悬停 key 时由平台文档链读取。
     */
    private val jsonPsiFile = PsiFileFactory.getInstance(project)
        .createFileFromText(
            JSON_EDITOR_FILE_NAME,
            JsonFileType.INSTANCE,
            DebugArgumentCodec.sampleJson(target.parameters)
        )
        .also {
            DebugJsonDocumentation.attach(it, target.parameters)
            // 平台补全复制虚拟文件时要求副本带 original，否则触发平台断言
            DebugJsonPsiCopySupport.bindOriginalFile(it)
        }

    /** JSON 参数编辑器软换行；与 Result/Logs 一致，默认单行横向滚动。 */
    private var jsonWrapLines = false

    /** JSON 字段注释开关；默认关闭，开启后在字段行尾显示 Javadoc 说明。 */
    private var jsonShowComments = false

    /** JSON 属性路径 → 单行字段说明；没有文档的字段不进入结果。 */
    private val jsonComments: Map<String, String> =
        DebugJsonDocumentation.commentsOf(target.parameters)

    /** Result JSON 的字段说明：来自返回类型的字段文档。 */
    private val resultFieldComments: Map<String, String> =
        DebugJsonDocumentation.fieldCommentsOf(target.returnType)

    /**
     * JSON 模式的参数编辑器。
     *
     * 用平台编辑器 + JSON 语言：语法高亮、括号匹配、注释着色与代码折叠
     * 都由 JSON 语言提供；EditorTextField 自带滚动，不再外包滚动面板。
     * 折叠区域由 [installJsonFoldRegions] 主动构建：EditorTextField 没有 daemon，
     * 平台不会替它跑折叠更新。
     */
    private val jsonEditor = object : EditorTextField(
        jsonPsiFile.viewProvider.document,
        project,
        JsonFileType.INSTANCE
    ) {
        override fun onEditorAdded(editor: Editor) {
            super.onEditorAdded(editor)
            if (ApplicationManager.getApplication().isDispatchThread) {
                installJsonFoldRegions(editor)
            } else {
                scheduleJsonFoldRegions(editor)
            }
        }
    }.apply {
        // 参数较多时至少留出几行，避免分割条把 JSON 区压没
        minimumSize = Dimension(0, JBUI.scale(120))
        setOneLineMode(false)
        addSettingsProvider { editor ->
            // 与 Logs/Result 同一编辑器 scheme：字体、前景/背景色完全一致
            editor.colorsScheme = EditorColorsManager.getInstance().globalScheme
            editor.settings.apply {
                isLineNumbersShown = true
                isFoldingOutlineShown = true
                // 折叠区域由 installJsonFoldRegions 主动构建，不依赖平台自动折叠
                isAutoCodeFoldingEnabled = false
                isUseSoftWraps = jsonWrapLines
                isCaretRowShown = false
                isWhitespacesShown = false
                additionalLinesCount = 0
                additionalColumnsCount = 0
            }
            // 折叠栏只在折叠模型开启时才绘制箭头
            editor.foldingModel.isFoldingEnabled = true
            // EditorTextField 默认隐藏滚动条；参数超出可视区后需要能滚动查看
            editor.setVerticalScrollbarVisible(true)
            // 默认单行显示，长行需要横向滚动条
            editor.setHorizontalScrollbarVisible(true)
            // 初始化时编辑器把光标放到末尾，视图随之滚到底部；回到首行才能看到模板开头
            editor.caretModel.moveToOffset(0)
            SwingUtilities.invokeLater {
                if (!editor.isDisposed) {
                    editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
                }
            }
        }
        // 文档变化后重建折叠区域；新建的对象/数组才会出现新的折叠箭头
        addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                scheduleJsonFoldRegions(editor)
            }
        })
    }

    /** 合并连续的折叠重建请求：一次编辑器事件只排队一次更新。 */
    private val foldingRebuildPending = AtomicBoolean(false)

    /** 当前挂上的 JSON 语法错误高亮；重建前先移除，避免旧范围残留。 */
    private val syntaxErrorHighlighters = ArrayList<RangeHighlighter>()

    /**
     * 构建并安装 JSON 折叠区域，同时刷新语法错误标记。
     *
     * EditorTextField 的编辑器没有文件编辑器上下文，CodeFoldingManager 与 daemon
     * 都不会为它工作；折叠用 JSON 语言的 folding builder 构建，语法错误则从 PSI 的
     * [PsiErrorElement] 收集后直接标记到编辑器标记模型，左侧箭头与红色波浪线才会出现。
     */
    private fun installJsonFoldRegions(editor: Editor) {
        val document = editor.document
        if (document.textLength == 0 || editor.isDisposed) {
            refreshJsonSyntaxErrors(editor)
            refreshJsonComments(editor)
            jsonToolbar.updateActionsAsync()
            return
        }
        val psiManager = PsiDocumentManager.getInstance(project)
        if (!psiManager.isCommitted(document)) {
            // 文档改动后先提交 PSI，折叠范围和错误标记才能和最新文本对齐
            psiManager.commitDocument(document)
        }
        refreshJsonSyntaxErrors(editor)
        val ranges = ApplicationManager.getApplication().runReadAction(
            Computable<List<Triple<Int, Int, String>>> {
                val psiFile = psiManager.getPsiFile(document)
                if (psiFile !is JsonFile) {
                    return@Computable emptyList()
                }
                jsonFoldingBuilder.buildFoldRegions(psiFile.node, document)
                    .mapNotNull { descriptor ->
                        val range = descriptor.range
                        if (range.isEmpty || range.endOffset > document.textLength) {
                            return@mapNotNull null
                        }
                        val placeholder = descriptor.placeholderText
                            ?: jsonFoldingBuilder.getPlaceholderText(descriptor.element)
                            ?: "..."
                        Triple(range.startOffset, range.endOffset, placeholder)
                    }
            }
        )
        val foldingModel = editor.foldingModel
        // 折叠模型按只读约定提供区域读取/批量安装
        ApplicationManager.getApplication().runReadAction(
            Runnable {
                foldingModel.runBatchFoldingOperation {
                    for (region in foldingModel.allFoldRegions) {
                        foldingModel.removeFoldRegion(region)
                    }
                    for ((start, end, placeholder) in ranges) {
                        foldingModel.addFoldRegion(start, end, placeholder)
                    }
                }
            }
        )
        // 折叠区域装好后折叠/展开动作才可用
        refreshJsonComments(editor)
        jsonToolbar.updateActionsAsync()
    }

    /** 按开关重建 JSON 字段行尾注释；编辑器尚未创建时跳过。 */
    private fun refreshJsonComments(editor: Editor? = jsonEditor.getEditor(false)) {
        if (editor == null) {
            return
        }
        JsonFieldComments.refresh(editor, jsonComments, jsonShowComments)
    }

    /** 切换字段注释：立即重建 inlay，并刷新工具栏图标状态。 */
    private fun setJsonShowComments(value: Boolean) {
        if (jsonShowComments == value) {
            return
        }
        jsonShowComments = value
        refreshJsonComments()
        jsonToolbar.updateActionsAsync()
    }

    /** 文档变化后重建折叠区域；编辑器创建时直接调用 [installJsonFoldRegions]。 */
    private fun scheduleJsonFoldRegions(editor: Editor?) {
        if (editor == null || !foldingRebuildPending.compareAndSet(false, true)) {
            return
        }
        ApplicationManager.getApplication().invokeLater {
            foldingRebuildPending.set(false)
            if (!editor.isDisposed) {
                installJsonFoldRegions(editor)
            }
        }
    }

    /**
     * 用平台标记模型画出 JSON 语法错误。
     *
     * EditorTextField 没有 daemon，平台的语法检查不会运行；结构错误从 PSI 的
     * [PsiErrorElement] 收集（缺值、未闭合括号等），平台解析器漏掉的尾逗号、
     * 未加引号的键、未闭合字符串由 [DebugJsonSyntax] 的严格校验兜底，文本修复后
     * 重建时自动移除。
     */
    private fun refreshJsonSyntaxErrors(editor: Editor) {
        val markupModel = editor.markupModel
        for (highlighter in syntaxErrorHighlighters) {
            markupModel.removeHighlighter(highlighter)
        }
        syntaxErrorHighlighters.clear()
        val document = editor.document
        if (editor.isDisposed || document.textLength == 0) {
            return
        }
        val psiManager = PsiDocumentManager.getInstance(project)
        if (!psiManager.isCommitted(document)) {
            // 错误范围按最新文本计算，未提交时先同步 PSI
            psiManager.commitDocument(document)
        }
        val structural = ApplicationManager.getApplication().runReadAction(
            Computable<List<Pair<IntRange, String>>> {
                val psiFile = psiManager.getPsiFile(document) as? JsonFile ?: return@Computable emptyList()
                PsiTreeUtil.collectElementsOfType(psiFile, PsiErrorElement::class.java)
                    .mapNotNull { error ->
                        val range = error.textRange
                        if (range.isEmpty) null else (range.startOffset until range.endOffset) to error.errorDescription
                    }
            }
        )
        val issues = structural.ifEmpty {
            listOfNotNull(
                DebugJsonSyntax.strictIssue(document.text)?.let { issue ->
                    (issue.offset until issue.offset + 1) to issue.message
                }
            )
        }
        if (issues.isEmpty()) {
            return
        }
        val attributes = editor.colorsScheme.getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES)
        for ((range, message) in issues) {
            val highlighter = markupModel.addRangeHighlighter(
                range.first,
                range.last + 1,
                HighlighterLayer.ERROR,
                attributes,
                HighlighterTargetArea.EXACT_RANGE
            )
            highlighter.errorStripeTooltip = message
            syntaxErrorHighlighters.add(highlighter)
        }
    }

    // ===== JSON 工具栏动作：与 Result 的图标一致，另加两个插入文件动作 =====

    private val jsonFormatAction = object : AnAction(
        "Format JSON",
        "Reformat the JSON with 2-space indentation; comments are kept",
        AllIcons.Actions.ReformatCode
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = formatJson()

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = jsonEditor.document.textLength > 0
        }
    }

    /** 字段注释开关：工具栏第二个图标，默认关闭。 */
    private val jsonCommentAction = object : ToggleAction(
        "Show Field Comments",
        "Show Javadoc comments after JSON fields",
        JsonFieldComments.icon()
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = jsonShowComments

        override fun setSelected(e: AnActionEvent, state: Boolean) = setJsonShowComments(state)

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = jsonEditor.document.textLength > 0
            e.presentation.icon = JsonFieldComments.icon()
        }
    }

    private val jsonCollapseAction = object : AnAction(
        "Collapse All",
        "Fold every JSON object and array",
        AllIcons.Actions.Collapseall
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = setJsonRegionsExpanded(false)

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = jsonFoldRegionCount() > 0
        }
    }

    private val jsonExpandAction = object : AnAction(
        "Expand All",
        "Unfold every folded JSON object and array",
        AllIcons.Actions.Expandall
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = setJsonRegionsExpanded(true)

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = jsonFoldRegionCount() > 0
        }
    }

    /** JSON 参数的单行/多行切换；与 Result/Logs 一致，默认单行。 */
    private val jsonWrapAction = object : ToggleAction(
        "Wrap Lines",
        "Wrap long JSON lines; disabled keeps one line and scrolls horizontally",
        AllIcons.Actions.ToggleSoftWrap
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = jsonWrapLines

        override fun setSelected(e: AnActionEvent, state: Boolean) = setJsonWrapLines(state)
    }

    private val jsonCopyAction = object : AnAction(
        "Copy JSON",
        "Copy the JSON arguments to the clipboard",
        AllIcons.Actions.Copy
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = copyJson()

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = jsonEditor.text.isNotEmpty()
        }
    }

    /** 插入本地文件路径：把绝对路径作为 JSON 字符串插入光标处。 */
    private val jsonInsertPathAction = object : AnAction(
        "Insert File Path",
        "Choose a local file and insert its absolute path as a JSON string",
        AllIcons.Actions.MenuOpen
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = insertJsonFilePath()
    }

    /** 插入文件内容：把文件读成 Base64 后作为 JSON 字符串插入光标处。 */
    private val jsonInsertBase64Action = object : AnAction(
        "Insert File as Base64",
        "Choose a local file and insert its Base64 content as a JSON string",
        AllIcons.FileTypes.Archive
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = insertJsonFileBase64()
    }

    /** JSON 参数工具栏：与 Logs 一样竖排在编辑器右侧。 */
    private val jsonToolbar = ActionManager.getInstance()
        .createActionToolbar(
            "LuckyHttpClient.DebugArgumentsJson",
            DefaultActionGroup().apply {
                add(jsonFormatAction)
                add(jsonCommentAction)
                add(jsonCollapseAction)
                add(jsonExpandAction)
                add(jsonWrapAction)
                add(jsonCopyAction)
                add(jsonInsertPathAction)
                add(jsonInsertBase64Action)
            },
            false
        )
        .apply { setTargetComponent(jsonEditor) }

    /** JSON 参数容器：编辑器居中，工具栏竖排在右侧（与 Logs 一致）。 */
    private val jsonPanel = JPanel(BorderLayout()).apply {
        add(jsonEditor, BorderLayout.CENTER)
        add(jsonToolbar.component, BorderLayout.EAST)
    }

    private val argumentCards = JPanel(CardLayout())

    /** Result 正文：只读编辑器（与 Logs 同字体），JSON 带行号、高亮与折叠。 */
    private val resultView = DebugResultView(project)

    /** Result 页顶部的容器未启动提示；执行前容器未就绪时显示。 */
    private val startHintLabel = JBLabel("Container is not running, unable to invoke this method.").apply {
        foreground = UIUtil.getLabelForeground()
    }

    /** 提示条中的启动图标（平台运行图标），点击后启动当前模块容器。 */
    private val startHintButton = iconButton(AllIcons.Actions.Execute, "Start the container").apply {
        accessibleContext.accessibleName = "Start container"
        addActionListener { startContainerFromHint() }
    }

    /** 启动图标前的引导词：与图标、后置说明同处第二行。 */
    private val startHintActionLabel = JBLabel("Click").apply {
        foreground = UIUtil.getLabelForeground()
    }

    /** 启动图标后的说明：与图标、前置引导词拼成完整的一句话。 */
    private val startHintSuffixLabel = JBLabel("to start the container.").apply {
        foreground = UIUtil.getLabelForeground()
    }

    /**
     * Result 页顶部的容器未启动提示。
     *
     * 第一行说明无法执行的原因，第二行为「Click ▶ to start the container.」，
     * 启动图标夹在文字中间；提示只在提示条展示一次，Result 文本区不再重复。
     */
    private val startHintPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = JBUI.Borders.empty(4, 6, 4, 6)
        startHintLabel.alignmentX = Component.LEFT_ALIGNMENT
        add(startHintLabel)
        add(
            JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
                isOpaque = false
                add(startHintActionLabel)
                add(startHintButton)
                add(startHintSuffixLabel)
            }.apply { alignmentX = Component.LEFT_ALIGNMENT }
        )
        isVisible = false
    }

    /** Result 页：顶部是容器未启动提示，下方是只读结果视图。 */
    private val resultPage = JPanel(BorderLayout()).apply {
        add(startHintPanel, BorderLayout.NORTH)
        add(resultView.component, BorderLayout.CENTER)
    }

    /** Logs 渲染直接复用平台控制台，字体、颜色与缩进和 Run 控制台一致。 */
    private val logView = ConsoleLogView(project)

    /** 复制/清空是否可用；日志非空时由 [updateLogActions] 打开。 */
    private var logActionsEnabled = false

    /** Logs 单行/多行切换；选中表示自动换行，默认单行横向滚动。 */
    private val wrapLogsAction = object : ToggleAction(
        "Wrap Lines",
        "Wrap long log lines; disabled keeps one line per entry and scrolls horizontally",
        AllIcons.Actions.ToggleSoftWrap
    ) {
        // 选中态读取 Swing 组件状态，更新必须在 EDT 执行
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = logView.wrapLines

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            setLogWrap(state)
        }
    }

    /** 自动跟随最新日志；用户向上滚动查看历史时会暂停，回到底部后恢复。 */
    private var autoScroll = true

    private val autoScrollAction = object : ToggleAction(
        "Auto Scroll",
        "Follow the newest log line; scrolling up pauses following until you return to the bottom",
        AllIcons.RunConfigurations.Scroll_down
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = autoScroll

        override fun setSelected(e: AnActionEvent, state: Boolean) {
            setAutoScroll(state)
        }
    }

    private val copyLogsAction = object : AnAction(
        "Copy Logs",
        "Copy the displayed log lines without ANSI color escapes",
        AllIcons.Actions.Copy
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = copyLogs()

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = logActionsEnabled
        }
    }

    private val clearLogsAction = object : AnAction(
        "Clear Logs",
        "Clear the log view only; the container log buffer is kept",
        AllIcons.Actions.GC
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = clearLogs()

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = logActionsEnabled
        }
    }

    /** Logs 区右侧的图标条：平台工具栏渲染为紧凑图标，无按钮边框。 */
    private val logToolbar = ActionManager.getInstance()
        .createActionToolbar(
            "LuckyHttpClient.DebugLogs",
            DefaultActionGroup().apply {
                add(wrapLogsAction)
                add(autoScrollAction)
                add(copyLogsAction)
                add(clearLogsAction)
            },
            false
        )
        .apply { setTargetComponent(logView.component) }

    /** 日志区：中部是平台控制台，右侧留给操作图标列。 */
    private val logsPanel = JPanel(BorderLayout()).apply {
        add(logView.component, BorderLayout.CENTER)
        add(logToolbar.component, BorderLayout.EAST)
    }

    private val tabs = JTabbedPane().apply {
        addTab("Result", resultPage)
        addTab("Logs", logsPanel)
    }

    /** 参数区标题：与 Result/Logs 选中标签一致的 Args 胶囊。 */
    private val argsTab = ArgsTab("Args", editorFont())

    /** 参数区标题行：Args 胶囊紧贴参数列表上方。 */
    private val argsHeader = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = JBUI.Borders.empty(4, 6, 2, 6)
        add(argsTab, BorderLayout.WEST)
    }

    /** 参数区：顶部 Args 标题，下方是表单或 JSON 编辑器卡片。 */
    private val argumentsPanel = JPanel(BorderLayout()).apply {
        add(argsHeader, BorderLayout.NORTH)
        add(argumentCards, BorderLayout.CENTER)
        // CardLayout 会把 JSON 编辑器的 120px 最小高度传导成参数面板的最小高度，
        // 不放开的话分隔条压不到三行，底部会留出空白
        minimumSize = Dimension(0, 0)
    }

    private val rootPanel = JPanel(BorderLayout())

    private val inputFields = LinkedHashMap<String, ValueInput>()

    /** 表单里带本地文件选择按钮的路径（FILE/BYTES/流参数）。 */
    private val filePickerPaths = LinkedHashSet<String>()

    /** 已渲染字段的说明；键为参数路径（嵌套字段用点分隔）。 */
    private val inputDescriptions = LinkedHashMap<String, String>()

    /** 已添加的顶层参数分组标题，顺序与参数列表一致。 */
    private val groupLabels = mutableListOf<String>()

    /** 参数分组内可折叠的字段行组件。 */
    private val groupRows = LinkedHashMap<Int, MutableList<Component>>()

    /** 参数分组标题，用于切换折叠箭头。 */
    private val groupHeaders = LinkedHashMap<Int, JBLabel>()

    /** 已折叠的顶层参数下标；重建表单时保留。 */
    private val collapsedGroups = mutableSetOf<Int>()

    /** 参数表单当前渲染的总行数（含对象分组标题行）；默认高度按它决定是否截断。 */
    private var parameterRowCount = 0

    /** 上一次渲染到 Logs 的完整快照，用于增量追加新日志行。 */
    private var renderedLogs: List<String> = emptyList()

    /**
     * 本标签页自己的日志窗口：只收集本次执行产生的容器日志。
     *
     * 容器日志缓冲按模块共享，多个标签页直接读快照会显示同一份内容；
     * 每次执行只保留一个 [LogWindow]，Logs 只展示该窗口内的行，
     * 上一次请求的日志在开始新执行时丢弃。
     */
    private val logWindows = ArrayList<LogWindow>()

    /** 一次执行对应的日志序号区间；[end] 为负表示仍在写入。 */
    private class LogWindow(val start: Long) {
        var end: Long = -1L
    }

    /** 合并日志高频追加触发的刷新：EDT 队列里最多同时有一个待执行的刷新。 */
    private val refreshPending = AtomicBoolean(false)

    private var busy = false

    /** busy 期间的进度文案；失败时 [refreshSnapshot] 会优先展示失败提示。 */
    private var busyMessage = ""

    @Volatile
    private var disposed = false

    private val hasUnsupported = target.parameters.any { it.kind == DebugParameterKind.UNSUPPORTED }

    /** 复杂参数（对象/列表/Map）默认用 JSON 编辑，简单类型保留逐字段表单。 */
    private val defaultJsonMode = target.parameters.any { isComplexArgument(it) }

    init {
        resultView.showMessage("Run the method to see the response.")
        buildParameterRows()
        argumentCards.add(parameterScroll, CARD_FORM)
        argumentCards.add(jsonPanel, CARD_JSON)
        // 对象、列表与 Map 在表单里只能整段填 JSON，首次打开直接切到 JSON 模式
        jsonCheckBox.isSelected = defaultJsonMode
        switchArgumentMode()
        updateRequestUrl()
        buildLayout()
        // 首次同步图标条状态，日志为空时复制/清空保持禁用
        refreshLogToolbar()
        service.addListener({ changed ->
            if (changed.name == target.moduleName) {
                scheduleSnapshotRefresh()
            }
        }, alarmDisposable)
        setBusy(false, "")
        scheduleRefresh()
    }

    /** 标签页与测试使用的根组件。 */
    val component: JComponent get() = rootPanel

    internal fun inputRowCountForTest(): Int = inputFields.size

    /** 表单里的顶层参数分组标题；只显示参数名。 */
    internal fun groupLabelsForTest(): List<String> = groupLabels.toList()

    /** 参数分组是否处于折叠状态。 */
    internal fun groupCollapsedForTest(index: Int): Boolean = index in collapsedGroups

    /** 参数分组的字段行是否可见。 */
    internal fun groupFieldRowsVisibleForTest(index: Int): Boolean =
        groupRows[index]?.all { it.isVisible } ?: false

    /** 模拟点击分组标题切换折叠。 */
    internal fun setGroupCollapsedForTest(index: Int, collapsed: Boolean) {
        target.parameters.firstOrNull { it.index == index }?.let { setGroupCollapsed(it, collapsed) }
    }

    /** 表单输入路径：单对象参数时从字段名开始，否则以参数名为前缀（嵌套字段用 `.` 连接）。 */
    internal fun inputPathsForTest(): List<String> = inputFields.keys.toList()

    /** 带本地文件选择按钮的表单路径（FILE/BYTES/流）。 */
    internal fun filePickerPathsForTest(): List<String> = filePickerPaths.toList()

    /** 表单里展示的字段说明；没有文档的字段不出现。 */
    internal fun inputDescriptionsForTest(): Map<String, String> = inputDescriptions.toMap()

    /** 当前表单值：对象参数已从字段输入组装为 JSON 文本。 */
    internal fun formValuesForTest(): Map<String, String> = collectFormValues()

    /** JSON 模式的初始模板（纯 JSON 骨架，字段说明走悬停文档）。 */
    internal fun jsonTemplateForTest(): String = jsonEditor.text

    /** JSON 编辑器背后的虚拟 PSI 文件；用于验证悬停文档与折叠。 */
    internal fun jsonPsiFileForTest(): PsiFile = jsonPsiFile

    /** JSON 编辑器组件；用于验证折叠栏与滚动条设置。 */
    internal fun jsonEditorForTest(): EditorTextField = jsonEditor

    /** JSON 参数编辑器字体族：应与 Logs/Result 的编辑器字体一致。 */
    internal fun jsonEditorFontFamilyForTest(): String? =
        jsonEditor.getEditor(true)?.colorsScheme?.editorFontName

    /** JSON 参数编辑器软换行状态；默认应为 false（单行显示）。 */
    internal fun jsonSoftWrapEnabledForTest(): Boolean =
        jsonEditor.getEditor(false)?.settings?.isUseSoftWraps ?: jsonWrapLines

    /** 切换 JSON 参数的软换行，验证工具栏动作可用。 */
    internal fun setJsonSoftWrapForTest(value: Boolean) = setJsonWrapLines(value)

    /** JSON 工具栏动作；验证与 Result 一致的图标动作齐全。 */
    internal fun jsonToolbarActionsForTest(): List<AnAction> = listOf(
        jsonFormatAction,
        jsonCommentAction,
        jsonCollapseAction,
        jsonExpandAction,
        jsonWrapAction,
        jsonCopyAction,
        jsonInsertPathAction,
        jsonInsertBase64Action
    )

    /** JSON 字段注释开关的当前状态；默认关闭。 */
    internal fun jsonCommentsEnabledForTest(): Boolean = jsonShowComments

    /** 切换 JSON 字段注释开关，验证行尾注释数量。 */
    internal fun setJsonCommentsForTest(value: Boolean) = setJsonShowComments(value)

    /** JSON 编辑器中的字段注释 inlay 数量。 */
    internal fun jsonCommentInlayCountForTest(): Int =
        jsonEditor.getEditor(true)?.let { JsonFieldComments.count(it) } ?: 0

    /** JSON 工具栏是否竖排在 JSON 编辑器右侧。 */
    internal fun jsonToolbarOnRightForTest(): Boolean =
        (jsonPanel.layout as? BorderLayout)?.getLayoutComponent(BorderLayout.EAST) === jsonToolbar.component

    /** JSON 工具栏是否随参数模式显示/隐藏。 */
    internal fun jsonToolbarVisibleForTest(): Boolean = jsonToolbar.component.isVisible

    /** JSON 折叠区域数量；为 0 表示编辑器拿不到折叠区域。 */
    internal fun jsonFoldRegionCountForTest(): Int = ApplicationManager.getApplication().runReadAction(
        Computable {
            val document = jsonEditor.document
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
            if (psiFile is JsonFile) {
                JsonFoldingBuilder().buildFoldRegions(psiFile.node, document).size
            } else {
                0
            }
        }
    )

    /** 编辑器折叠模型中的区域数量；为 0 说明左侧不会出现折叠箭头。 */
    internal fun jsonEditorFoldRegionCountForTest(): Int =
        jsonEditor.getEditor(true)?.foldingModel?.allFoldRegions?.size ?: 0

    /** 语法错误高亮数量；正常 JSON 为 0，未闭合引号/括号应为正数。 */
    internal fun jsonSyntaxErrorCountForTest(): Int =
        jsonEditor.getEditor(true)?.markupModel?.allHighlighters
            ?.count { it.layer == HighlighterLayer.ERROR } ?: 0

    /** 模拟在 JSON 编辑器里粘贴文本。 */
    internal fun setJsonTextForTest(text: String) {
        setJsonText(text)
    }

    /** 模拟点击 JSON 工具栏的 Format。 */
    internal fun formatJsonForTest() = formatJson()

    /** 执行按钮是否正在显示转圈动画。 */
    internal fun executeSpinningForTest(): Boolean =
        executeButton.icon === busySpinner && busySpinner.isRunning()

    /** 直接切换忙碌状态，验证转圈动画与按钮禁用。 */
    internal fun setBusyForTest(value: Boolean) {
        setBusy(value, if (value) "busy" else "")
    }

    internal fun setInputValueForTest(path: String, value: String) {
        inputFields[path]?.write?.invoke(value)
    }

    /** 模拟选择了本地文件：FILE 填绝对路径，BYTES/流填 Base64。 */
    internal fun applyFileForTest(path: String, file: File, base64: Boolean) {
        val input = inputFields[path] ?: return
        input.write(if (base64) readBase64(file) else file.absolutePath)
    }

    /** 模拟 JSON 模式插入本地文件内容。 */
    internal fun insertJsonFileForTest(file: File, base64: Boolean) {
        setJsonText("")
        val text = if (base64) readBase64(file) else file.absolutePath
        insertJsonText(JsonPrimitive(text).toString())
    }

    internal fun executeEnabledForTest(): Boolean = executeButton.isEnabled

    internal fun targetMenuVisibleForTest(): Boolean = targetMenuButton.isVisible

    /** 候选三角是否位于输入框最左端（WEST 容器的第一个组件）。 */
    internal fun targetMenuAtLeftEdgeForTest(): Boolean {
        val west = (addressPanel.layout as? BorderLayout)
            ?.getLayoutComponent(BorderLayout.WEST) as? JPanel ?: return false
        return west.components.firstOrNull() === targetMenuButton
    }

    internal fun targetLabelsForTest(): List<String> = targets.map(::menuLabelOf)

    /** 「更多」菜单里的开关项。 */
    internal fun moreOptionsForTest(): List<String> = MORE_OPTIONS

    /** 头部展示的完整路径；没有路径信息时为完整方法名（始终为 URL 维度的文本）。 */
    internal fun requestUrlForTest(): String = urlText

    /** 头部实际绘制的文本；随展示维度变化。 */
    internal fun headerTextForTest(): String = urlLabel.baseText + urlLabel.pathText

    /** 标题行展示的类名；只显示简单类名。 */
    internal fun headerClassNameForTest(): String = classNameLabel.text

    /** 标题行是否承载了收起按钮（地址栏内不再有）。 */
    internal fun closeButtonInTitleRowForTest(): Boolean =
        (titleRow.layout as? BorderLayout)?.getLayoutComponent(BorderLayout.EAST) === closeButton

    internal fun addressBarHasCloseButtonForTest(): Boolean =
        (addressPanel.layout as? BorderLayout)?.getLayoutComponent(BorderLayout.EAST) === closeButton

    /** 头部当前是否为方法维度（方法图标 + 方法全路径）。 */
    internal fun headerShowsMethodPathForTest(): Boolean = settings.isDebugHeaderShowsMethodPath

    /** 头部当前是否使用 HTTP 方法徽标；方法维度为 false。 */
    internal fun headerUsesHttpMethodIconForTest(): Boolean = headerShowsHttpMethodIcon

    internal fun switchHeaderMethodPathForTest(methodPath: Boolean) = setHeaderShowsMethodPath(methodPath)

    /** 头部方法徽标对应的请求方法。 */
    internal fun requestMethodForTest(): String? = target.httpMethod

    internal fun renderLogsForTest(logs: List<String>) = renderLogs(logs)

    /** 标签页标题：类名#方法名。 */
    internal fun tabTitleForTest(): String = tabTitle

    /** 嵌入标签页时内部标题行应隐藏，避免与标签页头重复。 */
    internal fun titleRowVisibleForTest(): Boolean = titleRow.isVisible

    /** 参数表单内容的自然高度；验证默认高度按行数收敛。 */
    internal fun parameterContentHeightForTest(): Int = parameterPanel.preferredSize.height

    /** 参数表单当前渲染的总行数；验证截断判断。 */
    internal fun parameterRowCountForTest(): Int = parameterRowCount

    /** 参数表单实际布局出的行高；验证默认高度按行截断。 */
    internal fun parameterRowHeightsForTest(): List<Int> = argumentRowHeights().toList()

    /** 参数区（分割条上方）当前高度；需先手动布局组件树。 */
    internal fun argumentsHeightForTest(): Int =
        ((rootPanel.layout as? BorderLayout)?.getLayoutComponent(BorderLayout.CENTER) as? JBSplitter)
            ?.firstComponent?.height ?: 0

    /** 参数区上下不展示内容的高度（Args 标题行 + 滚动面板边框）；验证不靠固定行高。 */
    internal fun argumentsChromeHeightForTest(): Int = argumentsChromeHeight()

    /** 参数行字体族：验证与 Result/Logs 的编辑器字体一致。 */
    internal fun parameterFontFamilyForTest(): String? =
        inputFields.values.firstOrNull()?.component?.font?.family

    /** Args 标题字体族：与参数行同一编辑器字体。 */
    internal fun argsTabFontFamilyForTest(): String = argsTab.font.family

    /** Args 标题是否位于参数卡片上方。 */
    internal fun argsTabAboveArgumentsForTest(): Boolean =
        (argumentsPanel.layout as? BorderLayout)?.getLayoutComponent(BorderLayout.NORTH) === argsHeader

    /** 直接开一个已结束的日志窗口，验证按标签页过滤。 */
    internal fun addLogWindowForTest(start: Long, end: Long) {
        logWindows.add(LogWindow(start).also { it.end = end })
    }

    /** 启动一次执行对应的日志窗口；上一轮窗口应被丢弃。 */
    internal fun startLogWindowForTest() {
        openLogWindow()
    }

    internal fun logWindowCountForTest(): Int = logWindows.size

    /** 用模拟快照验证本标签页的日志窗口过滤。 */
    internal fun visibleLogsForTest(logs: List<String>, startSequence: Long, nextSequence: Long): List<String> =
        visibleLogs(
            DebugContainerService.Snapshot(
                moduleName = target.moduleName,
                state = DebugContainerService.State.READY,
                mainClass = null,
                info = null,
                failure = null,
                logs = logs,
                autoRestartSuspended = false,
                logStartSequence = startSequence,
                logNextSequence = nextSequence
            )
        )

    internal fun logTextForTest(): String = logView.text().removeSuffix("\n")

    internal fun logWrapEnabledForTest(): Boolean = logView.wrapLines

    internal fun logWrapLinesForTest(): Boolean = logView.wrapLines

    /** 软换行开启时内容宽度跟随视口，关闭时按最长行横向滚动。 */
    internal fun logTracksViewportWidthForTest(): Boolean = logView.wrapLines

    internal fun toggleLogWrapForTest() {
        setLogWrap(!logView.wrapLines)
    }

    /** 平台控制台的滚动条留在右侧：滚动面板不做左右镜像。 */
    internal fun logScrollBarRightSideForTest(): Boolean {
        val flip = logView.scrollPane?.getClientProperty(JBScrollPane.Flip::class.java)
        return flip != JBScrollPane.Flip.HORIZONTAL && flip != JBScrollPane.Flip.BOTH
    }

    /** Logs 的背景与字体来自编辑器 scheme，与内置控制台一致。 */
    internal fun logBackgroundForTest(): Color? = logView.editorBackground

    internal fun logFontFamilyForTest(): String? = logView.editorFontFamily

    /** 日志操作图标条是否挂在 Logs 区右侧。 */
    internal fun logActionsOnRightForTest(): Boolean =
        (logsPanel.layout as? BorderLayout)?.getLayoutComponent(BorderLayout.EAST) === logToolbar.component

    /** 图标条各动作的更新线程；平台 2026.x 已弃用默认的 OLD_EDT。 */
    internal fun logActionUpdateThreadsForTest(): List<ActionUpdateThread> =
        listOf(wrapLogsAction, autoScrollAction, copyLogsAction, clearLogsAction).map { it.actionUpdateThread }

    internal fun autoScrollEnabledForTest(): Boolean = autoScroll

    internal fun toggleAutoScrollForTest() {
        setAutoScroll(!autoScroll)
    }

    internal fun copyableLogsForTest(): String = plainLogs()

    internal fun copyLogsEnabledForTest(): Boolean = logActionsEnabled

    internal fun clearLogsForTest() = clearLogs()

    internal fun isJsonModeForTest(): Boolean = jsonCheckBox.isSelected

    /** 直接渲染一次调用结果，验证 Result 视图的编辑器形态。 */
    internal fun renderOutcomeForTest(outcome: DebugCallOutcome) = renderOutcome(outcome)

    internal fun resultViewForTest(): DebugResultView = resultView

    internal fun resultFontFamilyForTest(): String? = resultView.fontFamilyForTest()

    internal fun resultLineNumbersShownForTest(): Boolean = resultView.lineNumbersShownForTest()

    internal fun resultJsonModeForTest(): Boolean = resultView.jsonModeForTest()

    internal fun resultTextForTest(): String = resultView.textForTest()

    /** 强制创建结果编辑器并重建折叠区域后返回折叠数量。 */
    internal fun resultFoldRegionCountForTest(): Int {
        resultView.ensureEditorForTest()
        return resultView.foldRegionCountForTest()
    }

    internal fun isWebServerEnabledForTest(): Boolean = webServerCheckBox.isSelected

    internal fun switchJsonModeForTest(selected: Boolean) {
        jsonCheckBox.isSelected = selected
        switchArgumentMode()
    }

    private fun buildLayout() {
        // 头部两行：上方标题行（类名 + 收起按钮），下方地址输入框（执行/更多）
        val header = JPanel(BorderLayout()).apply {
            add(titleRow, BorderLayout.NORTH)
            add(addressPanel, BorderLayout.CENTER)
            border = JBUI.Borders.empty(4, 6, 2, 6)
        }

        // 嵌入标签页：参数在上、结果/日志在下，分隔条可拖动。
        // 分割条按比例分配空间，布局时按实际参数行高折算比例，参数区最多露三行。
        val splitter = object : OnePixelSplitter(true, DEFAULT_ARGUMENTS_PROPORTION) {
            /** 用户拖动过分隔条后不再按默认行数校准。 */
            private var userMovedDivider = false

            /** 上一次由默认行高写入的比例；用户拖动会改变 proportion。 */
            private var appliedProportion = DEFAULT_ARGUMENTS_PROPORTION

            override fun doLayout() {
                // 组件的尺寸往往在未显示时就被设定，componentResized 不会派发；
                // doLayout 每次布局都会执行，在这里校准默认高度才可靠。
                if (!userMovedDivider && height > 0) {
                    if (proportion != appliedProportion) {
                        // 比例不是我们写入的，说明用户拖过分隔条
                        userMovedDivider = true
                    } else {
                        // 小窗口下参数区最多占一半，保证结果/日志仍有空间
                        val wanted = defaultArgumentsHeight().coerceAtMost(height / 2)
                        appliedProportion = wanted.toFloat() / height
                        proportion = appliedProportion
                    }
                }
                super.doLayout()
            }
        }.apply {
            firstComponent = argumentsPanel
            secondComponent = tabs
        }

        rootPanel.add(header, BorderLayout.NORTH)
        rootPanel.add(splitter, BorderLayout.CENTER)
    }

    /**
     * 参数区默认高度：最多露出三行参数。
     *
     * 行高不写死像素：参数行不超过三行时直接取表单内容的自然高度，
     * 参数模块与输入框贴合、底部不留空白；超过三行则只取前三个可见行的
     * 实际布局高度，其余参数滚动查看。
     */
    private fun defaultArgumentsHeight(): Int {
        val contentHeight = parameterPanel.preferredSize.height
        val shownHeight = if (parameterRowCount <= DEFAULT_ARGUMENT_ROW_COUNT) {
            // 不超过三行：跟随内容自然高度，输入框刚好占满参数区
            contentHeight
        } else {
            val rows = argumentRowHeights()
            if (rows.size < DEFAULT_ARGUMENT_ROW_COUNT) contentHeight else rows.take(DEFAULT_ARGUMENT_ROW_COUNT).sum()
        }
        return argumentsChromeHeight() + shownHeight
    }

    /** 参数区上下不展示内容的高度：Args 标题行 + 滚动面板边框。 */
    private fun argumentsChromeHeight(): Int {
        val headerHeight = argsHeader.preferredSize.height
        val scrollInsets = parameterScroll.border?.getBorderInsets(parameterScroll) ?: Insets(0, 0, 0, 0)
        return headerHeight + scrollInsets.top + scrollInsets.bottom
    }

    /**
     * 参数表单实际布局出的行高。
     *
     * GridBagLayout 的行高缓存只在 layoutContainer 时填充，preferredSize 不会写入；
     * 所以先按首选尺寸排练一次布局（面板未显示，不会影响最终外观），再读行高。
     * 读到的行高已包含行内控件高度与表单行 insets。
     */
    private fun argumentRowHeights(): IntArray {
        val layout = parameterPanel.layout as? GridBagLayout ?: return IntArray(0)
        val preferred = parameterPanel.preferredSize
        parameterPanel.setSize(preferred.width, preferred.height)
        layout.layoutContainer(parameterPanel)
        val dimensions = layout.getLayoutDimensions()
        return if (dimensions.size > 1) dimensions[1] else IntArray(0)
    }

    /** 两行 tooltip：状态提示（可选）+ 路径与方法名 + 模块与签名；尖括号必须转义。 */
    private fun targetTooltip(): String {
        val name = StringUtil.escapeXmlEntities(target.displayName)
        val module = StringUtil.escapeXmlEntities(target.moduleName)
        val signature = StringUtil.escapeXmlEntities(target.signature)
        val hint = panelHint?.let { StringUtil.escapeXmlEntities(it) }
        // 路径可能被窄面板裁剪，tooltip 补全；未解析出路径时与方法名相同，不重复
        val url = StringUtil.escapeXmlEntities(urlText).takeIf { it != name }
        val head = if (url == null) "<b>$name</b>" else "<b>$url</b><br/>$name"
        return if (hint.isNullOrBlank()) {
            "<html>$head<br/>$module · $signature</html>"
        } else {
            "<html><b>$hint</b><br/>$head<br/>$module · $signature</html>"
        }
    }

    /** 下拉候选项文案：带上参数类型，区分同一方法名的重载。 */
    private fun menuLabelOf(option: DebugTarget): String {
        val parameters = option.parameters.joinToString(", ") { it.typeName.substringAfterLast('.') }
        return "${option.className.substringAfterLast('.')}#${option.methodName}($parameters)"
    }

    /** 下拉选择同组件的其它接口方法：交给导航栏重建面板，容器状态保持不变。 */
    private fun showTargetPopup() {
        if (targets.isEmpty() || busy || !targetMenuButton.isEnabled) {
            return
        }
        val labels = targets.map(::menuLabelOf)
        // 选项字体与输入框里的方法路径完全一致；不设标题，列表直接从输入框下方展开
        val itemFont = urlLabel.font
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(labels)
            .setRenderer(object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val label = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                    ) as JLabel
                    label.font = itemFont
                    return label
                }
            })
            .setItemChosenCallback { chosen ->
                val index = labels.indexOf(chosen)
                if (index >= 0 && targets[index] != target) {
                    onSwitchTarget?.invoke(targets[index])
                }
            }
            .createPopup()
            .showUnderneathOf(addressPanel)
    }

    /**
     * 头部「更多」菜单。
     *
     * 用平台 ActionPopupMenu 渲染原生勾选项：菜单项的布局、勾选标记与
     * 悬停高亮都由平台绘制，自绘菜单项会和 IDE 其它菜单风格不一致。
     */
    private fun showMorePopup() {
        val hints = mapOf(
            METHOD_PATH_OPTION to METHOD_PATH_OPTION_HINT,
            JSON_OPTION to JSON_OPTION_HINT,
            WEB_SERVER_OPTION to (webServerHint ?: WEB_SERVER_OPTION_HINT)
        )
        val group = DefaultActionGroup().apply {
            add(
                toggleAction(
                    text = METHOD_PATH_OPTION,
                    description = METHOD_PATH_OPTION_HINT,
                    selectedState = { settings.isDebugHeaderShowsMethodPath },
                    enabledState = { true }
                ) { selected ->
                    setHeaderShowsMethodPath(selected)
                }
            )
            add(
                toggleAction(
                    text = JSON_OPTION,
                    description = JSON_OPTION_HINT,
                    selectedState = { jsonCheckBox.isSelected },
                    enabledState = { jsonCheckBox.isEnabled }
                ) { selected ->
                    jsonCheckBox.isSelected = selected
                    switchArgumentMode()
                }
            )
            add(
                toggleAction(
                    text = WEB_SERVER_OPTION,
                    description = hints.getValue(WEB_SERVER_OPTION),
                    selectedState = { webServerCheckBox.isSelected },
                    enabledState = { webServerCheckBox.isEnabled }
                ) { selected ->
                    webServerCheckBox.isSelected = selected
                    onWebServerToggled()
                }
            )
        }
        val menu = ActionManager.getInstance()
            .createActionPopupMenu("LuckyHttpClient.DebugPanel", group)
            .apply { setTargetComponent(moreButton) }
            .component
        menu.addPopupMenuListener(object : PopupMenuListener {
            override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) = Unit

            override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) = detachMenuHints(menu)

            override fun popupMenuCanceled(e: PopupMenuEvent) = detachMenuHints(menu)
        })
        menu.show(moreButton, 0, moreButton.height)
        // 平台菜单项只把描述送到状态栏，而且要到显示时才创建；
        // 等当前事件处理完再补上气泡 tooltip。
        SwingUtilities.invokeLater { attachMenuHints(menu, hints) }
    }

    /** 按标题给菜单项配悬停提示；菜单项由平台按动作创建。 */
    private fun attachMenuHints(menu: JPopupMenu, hints: Map<String, String>) {
        menu.components.filterIsInstance<JMenuItem>().forEach { item ->
            val hint = hints[item.text] ?: return@forEach
            item.toolTipText = hint
            ToolTipManager.sharedInstance().registerComponent(item)
        }
    }

    /** 菜单关闭后注销，避免 ToolTipManager 持有已销毁的菜单项。 */
    private fun detachMenuHints(menu: JPopupMenu) {
        menu.components.filterIsInstance<JMenuItem>().forEach {
            ToolTipManager.sharedInstance().unregisterComponent(it)
        }
    }

    /** 菜单勾选项：选中态与可用性由调用方提供的读取器决定（隐藏复选框或持久化设置）。 */
    private fun toggleAction(
        text: String,
        description: String,
        selectedState: () -> Boolean,
        enabledState: () -> Boolean,
        applyState: (Boolean) -> Unit
    ): AnAction = object : ToggleAction(text, description, null) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = selectedState()

        override fun setSelected(e: AnActionEvent, state: Boolean) = applyState(state)

        override fun update(e: AnActionEvent) {
            super.update(e)
            e.presentation.isEnabled = enabledState()
        }
    }

    /**
     * 参数表单。
     *
     * 每个顶层参数一行参数名标题（如 `request`，不再带 `$0` 索引），
     * 可解析对象在标题下缩进展开字段（字段名不带参数前缀），
     * 标量/容器参数直接把分组行当作输入行；字体与 Result/Logs 的编辑器一致。
     */
    private fun buildParameterRows() {
        parameterPanel.removeAll()
        inputFields.clear()
        inputDescriptions.clear()
        filePickerPaths.clear()
        groupLabels.clear()
        groupRows.clear()
        groupHeaders.clear()
        if (target.parameters.isEmpty()) {
            parameterRowCount = 0
            parameterPanel.add(JBLabel("No arguments").apply { font = editorFont() })
            return
        }
        var row = 0
        for (parameter in target.parameters) {
            row = addParameterGroup(parameter, row)
        }
        parameterRowCount = row
        parameterPanel.add(
            JPanel(),
            GridBagConstraints().apply {
                gridx = 0
                gridy = row
                gridwidth = 4
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
        )
    }

    /** 顶层参数分组：对象展开为可折叠的缩进字段行，其它类型一行输入。 */
    private fun addParameterGroup(parameter: DebugParameter, row: Int): Int {
        val key = rootPathOf(parameter)
        val title = groupTitle(parameter)
        groupLabels.add(title)
        if (parameter.kind == DebugParameterKind.OBJECT && parameter.fields.isNotEmpty()) {
            val header = groupHeader(parameter, title)
            groupHeaders[parameter.index] = header
            parameterPanel.add(
                header,
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = row
                    gridwidth = 4
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    anchor = GridBagConstraints.WEST
                    insets = Insets(6, 2, 2, 4)
                }
            )
            val rows = mutableListOf<Component>()
            var next = row + 1
            for (field in parameter.fields) {
                next = addFieldRow(field, joinPath(key, field.name), field.name, 1, next, rows)
            }
            groupRows[parameter.index] = rows
            // 重建表单（例如切换 JSON 再切回）时恢复上一次的折叠状态
            setGroupCollapsed(parameter, parameter.index in collapsedGroups)
            return next
        }
        // 标量、列表、Map、无法展开的对象：分组标题与输入行合并
        addLeafRow(parameter, key, title, 0, row, null)
        return row + 1
    }

    /** 可折叠的分组标题；点击箭头或标题切换字段行。 */
    private fun groupHeader(parameter: DebugParameter, title: String): JBLabel =
        JBLabel(title).apply {
            icon = groupIcon(parameter.index in collapsedGroups)
            font = editorFont().deriveFont(Font.BOLD)
            toolTipText = fieldTooltip(parameter)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        setGroupCollapsed(parameter, parameter.index !in collapsedGroups)
                    }
                }
            })
        }

    private fun groupIcon(collapsed: Boolean): Icon =
        if (collapsed) AllIcons.General.ArrowRight else AllIcons.General.ArrowDown

    /** 折叠只切换字段行可见性；已填的值保留，提交时仍会带上。 */
    private fun setGroupCollapsed(parameter: DebugParameter, collapsed: Boolean) {
        val rows = groupRows[parameter.index] ?: return
        if (collapsed) {
            collapsedGroups.add(parameter.index)
        } else {
            collapsedGroups.remove(parameter.index)
        }
        rows.forEach { it.isVisible = !collapsed }
        groupHeaders[parameter.index]?.icon = groupIcon(collapsed)
        parameterPanel.revalidate()
        parameterPanel.repaint()
    }

    /** 对象字段行；嵌套对象继续展开，字段名只保留相对路径。 */
    private fun addFieldRow(
        parameter: DebugParameter,
        key: String,
        label: String,
        depth: Int,
        row: Int,
        rows: MutableList<Component>
    ): Int {
        if (parameter.kind == DebugParameterKind.OBJECT && parameter.fields.isNotEmpty()) {
            var next = row
            for (field in parameter.fields) {
                next = addFieldRow(
                    field,
                    joinPath(key, field.name),
                    joinPath(label, field.name),
                    depth + 1,
                    next,
                    rows
                )
            }
            return next
        }
        addLeafRow(parameter, key, label, depth, row, rows)
        return row + 1
    }

    /** 叶子输入行：名称、类型、输入控件、注释（截断，悬停看全文）。 */
    private fun addLeafRow(
        parameter: DebugParameter,
        key: String,
        label: String,
        depth: Int,
        row: Int,
        rows: MutableList<Component>?
    ) {
        val input = inputFor(parameter)
        applyEditorFont(input.component)
        inputFields[key] = input
        if (parameter.kind == DebugParameterKind.FILE || parameter.kind == DebugParameterKind.BYTES) {
            filePickerPaths.add(key)
        }
        val nameLabel = JBLabel(label).apply {
            font = editorFont()
            border = JBUI.Borders.emptyLeft(JBUI.scale(FIELD_INDENT) * depth)
            toolTipText = fieldTooltip(parameter)
        }
        val typeLabel = JBLabel(typeHint(parameter)).apply {
            font = editorFont().deriveFont((editorFont().size2D - 1f).coerceAtLeast(11f))
            foreground = UIUtil.getLabelDisabledForeground()
            toolTipText = parameter.typeName
        }
        val description = descriptionLabel(parameter.description)
        description?.let { inputDescriptions[key] = it.toolTipText.orEmpty() }

        parameterPanel.add(nameLabel, labelConstraints(row))
        parameterPanel.add(
            typeLabel,
            GridBagConstraints().apply {
                gridx = 1
                gridy = row
                weightx = 0.0
                anchor = GridBagConstraints.WEST
                insets = Insets(3, 0, 3, 8)
            }
        )
        parameterPanel.add(
            input.component,
            GridBagConstraints().apply {
                gridx = 2
                gridy = row
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                insets = Insets(3, 0, 3, 8)
            }
        )
        rows?.add(nameLabel)
        rows?.add(typeLabel)
        rows?.add(input.component)
        if (description != null) {
            parameterPanel.add(
                description,
                GridBagConstraints().apply {
                    gridx = 3
                    gridy = row
                    weightx = 0.0
                    anchor = GridBagConstraints.WEST
                    insets = Insets(3, 0, 3, 4)
                }
            )
            rows?.add(description)
        }
    }

    /** 参数标题：只显示参数名，不再带 `$0(...)` 索引。 */
    private fun groupTitle(parameter: DebugParameter): String = parameter.name

    /**
     * 表单根路径。
     *
     * 只有一个可展开对象参数时省略参数名前缀，表单直接以字段名（点路径）展示；
     * 多参数时保留参数名，避免不同对象的同名字段互相覆盖。
     */
    private fun rootPathOf(parameter: DebugParameter): String =
        if (target.parameters.size == 1 &&
            parameter.kind == DebugParameterKind.OBJECT &&
            parameter.fields.isNotEmpty()
        ) {
            ""
        } else {
            parameter.name
        }

    private fun joinPath(prefix: String, name: String): String =
        if (prefix.isEmpty()) name else "$prefix.$name"

    private fun labelConstraints(row: Int) = GridBagConstraints().apply {
        gridx = 0
        gridy = row
        weightx = 0.0
        anchor = GridBagConstraints.WEST
        insets = Insets(3, 4, 3, 8)
    }

    private fun descriptionLabel(description: String?): JBLabel? {
        val text = description
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        // 按代码点截断，避免把代理对（emoji 等）拆成半个字符
        val tooLong = text.codePointCount(0, text.length) > DESCRIPTION_PREVIEW_LENGTH
        val shown = if (tooLong) {
            text.substring(0, text.offsetByCodePoints(0, DESCRIPTION_PREVIEW_LENGTH)) + "…"
        } else {
            text
        }
        return JBLabel(shown).apply {
            font = font.deriveFont(font.size2D - 1f)
            foreground = UIUtil.getLabelDisabledForeground()
            toolTipText = text
        }
    }

    private fun inputFor(parameter: DebugParameter): ValueInput = when (parameter.kind) {
        DebugParameterKind.BOOLEAN -> {
            val combo = JComboBox(arrayOf("", "true", "false"))
            ValueInput(parameter, combo, { combo.selectedItem?.toString().orEmpty() }) {
                combo.selectedItem = it
            }
        }

        DebugParameterKind.ENUM -> {
            val items = arrayOf("") + parameter.enumConstants.toTypedArray()
            val combo = JComboBox(items)
            ValueInput(parameter, combo, { combo.selectedItem?.toString().orEmpty() }) {
                combo.selectedItem = it
            }
        }

        DebugParameterKind.FILE -> {
            val field = JBTextField().apply {
                toolTipText = "Local file path; type it or choose a file"
            }
            val browse = browseButton("Select a local file") {
                chooseLocalFile("Select File")?.let { file -> field.text = file.absolutePath }
            }
            ValueInput(parameter, inputWithButton(field, browse), { field.text }) {
                field.text = it
            }
        }

        DebugParameterKind.BYTES -> {
            val area = JBTextArea("", 3, 40).apply {
                lineWrap = true
                wrapStyleWord = false
                font = editorFont()
                toolTipText = "Base64 text; or choose a local file and read it as Base64"
            }
            val browse = browseButton("Select a local file and read its content as Base64") {
                chooseLocalFile("Select File")?.let { file ->
                    readInBackground(file) { base64 ->
                        if (!disposed) area.text = base64
                    }
                }
            }
            val scroll = JBScrollPane(area)
            ValueInput(parameter, inputWithButton(scroll, browse), { area.text }) {
                area.text = it
            }
        }

        DebugParameterKind.LIST,
        DebugParameterKind.MAP,
        DebugParameterKind.OBJECT -> {
            val area = JBTextArea(DebugArgumentCodec.formTemplateOf(parameter), 4, 40).apply {
                lineWrap = true
                wrapStyleWord = false
                font = editorFont()
            }
            ValueInput(parameter, JBScrollPane(area), { area.text }) {
                area.text = it
            }
        }

        DebugParameterKind.UNSUPPORTED -> {
            val field = JBTextField(parameter.unsupportedReason ?: "Unsupported").apply {
                isEnabled = false
            }
            ValueInput(parameter, field, { "" })
        }

        else -> {
            val field = JBTextField().apply {
                toolTipText = "${parameter.typeName} — ${DebugArgumentCodec.exampleOf(parameter)}"
            }
            ValueInput(parameter, field, { field.text }) {
                field.text = it
            }
        }
    }

    /** 输入框 + 右侧文件选择按钮；按钮不占输入框宽度。 */
    private fun inputWithButton(component: JComponent, button: JButton): JPanel =
        JPanel(BorderLayout(4, 0)).apply {
            isOpaque = false
            add(component, BorderLayout.CENTER)
            add(button, BorderLayout.EAST)
        }

    private fun browseButton(tooltip: String, action: () -> Unit): JButton =
        iconButton(AllIcons.Actions.MenuOpen, tooltip).apply {
            addActionListener { action() }
        }

    /** 选择本地文件；取消返回 null。 */
    private fun chooseLocalFile(title: String): File? {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .withTitle(title)
        return FileChooser.chooseFile(descriptor, project, null)?.let { File(it.path) }
    }

    /** 读文件可能很大，放后台线程；完成后回 EDT 填空。 */
    private fun readInBackground(file: File, onDone: (String) -> Unit) {
        AppExecutorUtil.getAppExecutorService().execute {
            val result = runCatching { readBase64(file) }
            ui {
                result.onSuccess(onDone).onFailure { showMessage("Failed to read file\n\n${it.message}") }
            }
        }
    }

    private fun readBase64(file: File): String =
        Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()))

    /** JSON 模式：插入所选文件的绝对路径。 */
    private fun insertJsonFilePath() {
        chooseLocalFile("Select File")?.let { file ->
            insertJsonText(JsonPrimitive(file.absolutePath).toString())
        }
    }

    /** JSON 模式：插入所选文件的 Base64 内容。 */
    private fun insertJsonFileBase64() {
        val file = chooseLocalFile("Select File") ?: return
        readInBackground(file) { base64 -> insertJsonText(JsonPrimitive(base64).toString()) }
    }

    /** 在 JSON 编辑器的光标/选区处插入文本。 */
    private fun insertJsonText(text: String) {
        val document = jsonEditor.document
        val length = document.textLength
        val selection = jsonEditor.editor?.selectionModel
        val start = (selection?.selectionStart ?: length).coerceIn(0, length)
        val end = (selection?.selectionEnd ?: length).coerceIn(start, length)
        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, text)
        }
        jsonEditor.editor?.caretModel?.moveToOffset(start + text.length)
        jsonEditor.requestFocusInWindow()
    }

    /** 整段替换 JSON 文本；文档修改要包在写命令里，才能进入撤销栈。 */
    private fun setJsonText(text: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            jsonEditor.document.setText(text)
        }
    }

    /**
     * 格式化 JSON：只重排空白与缩进，注释原样保留。
     *
     * 模板本身就是标准缩进，格式化后保持不变；粘贴的单行 JSON 会被展开。
     */
    private fun formatJson() {
        val document = jsonEditor.document
        val formatted = DebugJsonFormatter.format(document.text)
        if (formatted.isEmpty() || formatted == document.text) {
            return
        }
        WriteCommandAction.runWriteCommandAction(project) {
            document.setText(formatted)
        }
        // 重排会改写偏移，光标回到开头，避免落在错位的位置
        jsonEditor.editor?.caretModel?.moveToOffset(0)
    }

    /** 收起/展开全部 JSON 折叠区域；区域由 [installJsonFoldRegions] 构建。 */
    private fun setJsonRegionsExpanded(expanded: Boolean) {
        val editor = jsonEditor.getEditor(false) ?: return
        ApplicationManager.getApplication().runReadAction(
            Runnable {
                editor.foldingModel.runBatchFoldingOperation {
                    for (region in editor.foldingModel.allFoldRegions) {
                        region.isExpanded = expanded
                    }
                }
            }
        )
        jsonToolbar.updateActionsAsync()
    }

    /** 当前 JSON 参数的折叠区域数量；为 0 时折叠/展开动作不可用。 */
    private fun jsonFoldRegionCount(): Int =
        jsonEditor.getEditor(false)?.foldingModel?.allFoldRegions?.size ?: 0

    /** 切换 JSON 参数编辑器的软换行；默认单行，长行横向滚动。 */
    private fun setJsonWrapLines(value: Boolean) {
        jsonWrapLines = value
        jsonEditor.getEditor(false)?.settings?.isUseSoftWraps = value
        jsonToolbar.updateActionsAsync()
    }

    /** 复制 JSON 参数文本；注释与缩进原样保留。 */
    private fun copyJson() {
        val text = jsonEditor.text
        if (text.isEmpty()) {
            return
        }
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    /** 类型提示带上列表元素类型，例如 List<WeatherDay>；字段可省略不标记。 */
    private fun typeHint(parameter: DebugParameter): String {
        val simple = parameter.typeName.substringAfterLast('.')
        val element = parameter.element ?: return simple
        return "$simple<${element.typeName.substringAfterLast('.')}>"
    }

    private fun fieldTooltip(parameter: DebugParameter): String = buildString {
        append(parameter.typeName)
        parameter.description?.takeIf { it.isNotBlank() }?.let { append(" — ").append(it) }
        if (parameter.kind != DebugParameterKind.OBJECT && parameter.kind != DebugParameterKind.LIST) {
            append("，示例：").append(DebugArgumentCodec.exampleOf(parameter))
        }
    }

    /** 读取表单：对象参数用字段输入组装 JSON，其它类型直接读控件文本。 */
    private fun collectFormValues(): Map<String, String> =
        target.parameters.associate { parameter ->
            parameter.name to readParameter(parameter, rootPathOf(parameter))
        }

    private fun readParameter(parameter: DebugParameter, path: String): String {
        if (parameter.kind == DebugParameterKind.OBJECT && parameter.fields.isNotEmpty()) {
            return readObject(parameter, path).toString()
        }
        return inputFields[path]?.read().orEmpty()
    }

    /** 递归读取对象字段；留空的字段不输出，交给目标类型使用默认值/null。 */
    private fun readObject(parameter: DebugParameter, path: String): JsonObject {
        val result = JsonObject()
        for (field in parameter.fields) {
            val fieldPath = joinPath(path, field.name)
            if (field.kind == DebugParameterKind.OBJECT && field.fields.isNotEmpty()) {
                val child = readObject(field, fieldPath)
                if (child.size() > 0) {
                    result.add(field.name, child)
                }
                continue
            }
            val text = inputFields[fieldPath]?.read().orEmpty()
            if (text.isBlank()) {
                continue
            }
            val element = try {
                DebugArgumentCodec.fieldElement(field, text)
            } catch (e: DebugArgumentCodec.CodecException) {
                throw DebugArgumentCodec.CodecException("字段 $fieldPath：${e.message}", e)
            }
            result.add(field.name, element)
        }
        return result
    }

    private fun execute() {
        if (busy) {
            return
        }
        val current = module
        if (current == null) {
            showMessage("Module not found: ${target.moduleName}")
            return
        }
        if (hasUnsupported) {
            showMessage("This method has unsupported parameters and cannot be invoked yet.")
            return
        }
        if (containerState() != DebugContainerService.State.READY) {
            // 容器未启动：不自动拉起，提示用户用提示条上的图标启动
            showContainerNotRunning()
            return
        }
        val jsonMode = jsonCheckBox.isSelected
        // 对象参数在这里组装 JSON，字段格式错误需要直接提示，不能带到后台线程
        val values = if (jsonMode) {
            emptyMap()
        } else {
            try {
                collectFormValues()
            } catch (e: DebugArgumentCodec.CodecException) {
                showMessage("参数格式错误\n\n${e.message}")
                return
            }
        }
        val jsonText = if (jsonMode) jsonEditor.text else null

        // 每次执行只展示本次请求的日志：先丢弃上一轮窗口与输出，再开本次窗口
        clearLogs()
        val logWindow = openLogWindow()
        setBusy(true, "Invoking…")
        AppExecutorUtil.getAppExecutorService().execute {
            try {
                val outcome = executor.invoke(target, values, jsonText)
                ui {
                    closeLogWindow(logWindow)
                    renderOutcome(outcome)
                    refreshSnapshot()
                    setBusy(false, "")
                }
            } catch (e: Throwable) {
                ui {
                    closeLogWindow(logWindow)
                    showMessage("Unexpected error: ${e.message ?: e.javaClass.name}")
                    setBusy(false, "")
                }
            }
        }
    }

    /** 当前模块的轻量容器状态：只读 volatile 状态，不复制日志。 */
    private fun containerState(): DebugContainerService.State? =
        service.statuses().firstOrNull { it.moduleName == target.moduleName }?.state

    /** 非启动状态下点击执行：Result 页顶部提示先启动容器，并提供启动图标。 */
    private fun showContainerNotRunning() {
        startHintPanel.isVisible = true
        startHintPanel.revalidate()
        tabs.selectedIndex = 0
        // 完整提示只留在顶部提示条，文本区不再写重复内容
        resultView.clear()
    }

    /** Result 页提示条上的启动图标：后台启动当前模块容器，完成后提示再次执行。 */
    private fun startContainerFromHint() {
        val current = module ?: return
        if (busy) {
            return
        }
        val state = containerState()
        if (state == DebugContainerService.State.STARTING ||
            state == DebugContainerService.State.RESTARTING ||
            state == DebugContainerService.State.STOPPING
        ) {
            return
        }
        val webServer = webServerCheckBox.isSelected
        webServerHint = null
        startHintPanel.isVisible = false
        setBusy(true, "Starting container…")
        AppExecutorUtil.getAppExecutorService().execute {
            val result = try {
                service.start(current, programArgs = DebugContainerService.programArgsFor(webServer))
            } catch (e: Throwable) {
                DebugContainerService.StartResult.Failed(e.message ?: e.javaClass.name, emptyList())
            }
            ui {
                setBusy(false, "")
                refreshSnapshot()
                tabs.selectedIndex = 0
                resultView.showMessage(
                    if (result is DebugContainerService.StartResult.Ready) {
                        "Container started. Run the method again."
                    } else {
                        describeStartFailure(result)
                    }
                )
            }
        }
    }

    private fun describeStartFailure(result: DebugContainerService.StartResult): String = when (result) {
        is DebugContainerService.StartResult.Ready -> ""

        is DebugContainerService.StartResult.Failed ->
            buildString {
                append("Container failed to start\n\n").append(result.message)
                if (result.logs.isNotEmpty()) {
                    append("\n\nLast log lines:")
                    result.logs.takeLast(20).forEach { append('\n').append(it) }
                }
            }

        is DebugContainerService.StartResult.ClasspathTooLong -> buildString {
            append("Classpath is too long: ").append(result.length)
            append(" characters (command line limit ").append(result.limit).append(").\n")
            append("Reduce dependencies in this module or split it into smaller modules.\n\n")
            append("Sample entries:")
            result.sample.forEach { append("\n  ").append(it) }
        }

        is DebugContainerService.StartResult.MainClassMissing -> buildString {
            append("No Spring Boot main class\n\n").append(result.message)
            if (result.candidates.isNotEmpty()) {
                append("\n\nCandidates:")
                result.candidates.forEach { append("\n  ").append(it) }
            }
        }

        is DebugContainerService.StartResult.IndexNotReady ->
            "Index is not ready\n\n${result.message}"
    }

    private fun renderOutcome(outcome: DebugCallOutcome) {
        when (outcome) {
            is DebugCallOutcome.Success -> resultView.showSuccess(
                returnType = outcome.returnType,
                kind = outcome.kind,
                body = outcome.body,
                elapsedMs = outcome.elapsedMs,
                comments = resultFieldComments
            )

            is DebugCallOutcome.Failure -> resultView.showFailure(
                title = "ERROR [${outcome.code.code}] ${outcome.code.displayName}",
                body = buildString {
                    append(outcome.message)
                    outcome.detail?.takeIf { it.isNotBlank() }?.let {
                        append("\n\n").append(it)
                    }
                },
                elapsedMs = outcome.elapsedMs
            )
        }
    }

    private fun showMessage(message: String) {
        resultView.showMessage(message)
        tabs.selectedIndex = 0
    }

    /**
     * 头部固定展示请求路径；状态提示只写进路径的 tooltip。
     *
     * 三个点按钮只描述自身功能，Web server 的「重启后生效」提示
     * 只出现在菜单项悬停上，避免悬停按钮时弹出无关文档。
     */
    private fun setStatusHint(text: String) {
        val hint = text.ifBlank { null }
        if (hint == panelHint) {
            return
        }
        panelHint = hint
        urlLabel.toolTipText = targetTooltip()
    }

    /**
     * 生成头部展示：URL 维度为 baseUrl 灰显 + 接口 path 正常色；
     * 方法维度为方法图标 + 方法全路径。
     */
    private fun updateRequestUrl() {
        val baseUrl = target.baseUrl.orEmpty()
        val path = target.requestPath.orEmpty()
        if (baseUrl.isEmpty() && path.isEmpty()) {
            urlText = target.displayName
            urlBaseText = ""
            urlPathText = urlText
        } else {
            val (fullUrl, pathStart) = joinRequestUrl(baseUrl, path)
            urlText = fullUrl
            // 两段文本分别着色：baseUrl 灰显、path 用正常前景色，由标签自绘，不依赖 HTML
            urlBaseText = fullUrl.substring(0, pathStart)
            urlPathText = fullUrl.substring(pathStart)
        }
        // 路径解析完成后同步 tooltip：窄面板裁剪时仍可悬停查看完整 URL
        urlLabel.toolTipText = targetTooltip()
        applyHeaderDimension()
    }

    /**
     * 按当前展示维度刷新头部：
     *
     * - URL 维度：HTTP 方法徽标 + baseUrl/path 两段着色的完整地址；
     * - 方法维度：组件图标 + 全限定方法路径（className.methodName(简单类型)）。
     *
     * 两种维度的图标高度都接近行内按钮尺寸，切换时头部行高不跳动。
     */
    private fun applyHeaderDimension() {
        val methodPath = settings.isDebugHeaderShowsMethodPath
        val httpIcon = if (methodPath) null else LuckyIcons.httpMethod(target.httpMethod)
        headerShowsHttpMethodIcon = httpIcon != null
        methodIcon.icon = httpIcon
            ?.let { IconUtil.scale(it, null, METHOD_BADGE_SCALE) }
            ?: LuckyIcons.Component
        if (methodPath) {
            urlLabel.baseText = ""
            urlLabel.pathText = methodPathText()
        } else {
            urlLabel.baseText = urlBaseText
            urlLabel.pathText = urlPathText
        }
    }

    /** 输入框方法维度文本：全限定类名.方法名(简单参数类型)，与 IDE 输入框风格一致。 */
    private fun methodPathText(): String {
        val parameters = target.parameters.joinToString(", ") { simplifyTypeName(it.typeName) }
        return "${target.className}.${target.methodName}($parameters)"
    }

    /** 去掉类型名中的包名前缀，保留泛型与数组写法。 */
    private fun simplifyTypeName(typeName: String): String =
        PACKAGE_PREFIX.replace(typeName, "$1")

    /** 切换头部展示维度并持久化；只刷新头部，不影响容器与调用目标。 */
    private fun setHeaderShowsMethodPath(methodPath: Boolean) {
        settings.isDebugHeaderShowsMethodPath = methodPath
        applyHeaderDimension()
    }

    /**
     * 拼接 baseUrl 与接口 path，并返回 path 在完整 URL 中的起点。
     *
     * 分隔规则与运行期一致：两侧都带 `/` 时只保留一个，任一侧带 `/` 时
     * 直接拼接，都没有时补一个 `/`；补出的分隔符归入灰色 baseUrl 段。
     */
    private fun joinRequestUrl(baseUrl: String, path: String): Pair<String, Int> = when {
        baseUrl.isEmpty() -> path to 0
        path.isEmpty() -> baseUrl to baseUrl.length
        baseUrl.endsWith("/") && path.startsWith("/") ->
            (baseUrl + path.substring(1)) to baseUrl.length
        baseUrl.endsWith("/") || path.startsWith("/") -> (baseUrl + path) to baseUrl.length
        else -> "$baseUrl/$path" to baseUrl.length + 1
    }

    private fun refreshSnapshot() {
        val current = module
        if (current == null) {
            setStatusHint("Module not found: ${target.moduleName}")
            syncControls(null)
            return
        }
        val snapshot = service.snapshot(current)
        if (snapshot == null) {
            // 容器状态归属导航树项目行，面板只展示进度与失败提示
            setStatusHint(if (busy) busyMessage else "")
            syncControls(null)
            return
        }
        syncControls(snapshot.state)
        if (snapshot.state == DebugContainerService.State.READY) {
            // 容器已就绪（包括从提示条启动成功）：撤掉 Result 页的未启动提示
            startHintPanel.isVisible = false
        }
        // 启动失败可能发生在后台线程；即使 start() 尚未返回，也要先撤掉
        // “Starting container…”，让用户知道失败已经发生。
        setStatusHint(
            when {
                snapshot.state == DebugContainerService.State.FAILED ->
                    "Container failed to start — see Result / Logs"
                busy -> busyMessage
                snapshot.autoRestartSuspended ->
                    "Auto restart suspended after repeated failures. Use the project action to retry."
                else -> ""
            }
        )
        // 开始新一次启动时 logs 会被清空，这里同步清掉旧日志。
        renderLogs(visibleLogs(snapshot))
    }

    /**
     * 打开本次执行的日志窗口：起点为当前容器日志末尾。
     *
     * 窗口列表只保留这一个，上一次请求的日志窗口直接丢弃，
     * 保证 Logs 只展示当前请求产生的行。
     */
    private fun openLogWindow(): LogWindow {
        val start = module?.let { service.snapshot(it)?.logNextSequence } ?: 0L
        logWindows.clear()
        return LogWindow(start).also { logWindows.add(it) }
    }

    /**
     * 结束日志窗口：延迟到 stdout 落盘后再截断到当前末尾。
     *
     * 请求响应与日志经不同通道到达界面，立即截断可能漏掉最后几行；
     * 延迟后窗口固定，之后其它来源的日志不再进入本标签页。
     */
    private fun closeLogWindow(window: LogWindow) {
        alarm.addRequest({
            // 新一次执行可能已丢弃旧窗口，过时的回调不再刷新界面
            if (!disposed && logWindows.contains(window)) {
                val next = module?.let { service.snapshot(it)?.logNextSequence } ?: 0L
                window.end = next.coerceAtLeast(window.start)
                refreshSnapshot()
            }
        }, LOG_WINDOW_CLOSE_DELAY_MILLIS)
    }

    /** 本标签页的日志：只拼接自己窗口内的行，缓冲区被裁剪时跳过已滚出的旧行。 */
    private fun visibleLogs(snapshot: DebugContainerService.Snapshot): List<String> {
        if (logWindows.isEmpty()) {
            return emptyList()
        }
        val result = ArrayList<String>()
        for (window in logWindows) {
            val from = window.start.coerceAtLeast(snapshot.logStartSequence)
            val to = (if (window.end < 0L) snapshot.logNextSequence else window.end)
                .coerceAtMost(snapshot.logNextSequence)
            if (to <= from) {
                continue
            }
            val fromIndex = (from - snapshot.logStartSequence).toInt().coerceIn(0, snapshot.logs.size)
            val toIndex = (to - snapshot.logStartSequence).toInt().coerceIn(fromIndex, snapshot.logs.size)
            result.addAll(snapshot.logs.subList(fromIndex, toIndex))
        }
        return result
    }

    /**
     * 增量渲染日志：只往平台控制台追加新增行，ANSI 颜色由控制台解码。
     *
     * 日志被清空或缓冲被裁剪导致新旧内容不再有公共前缀时退回全量重设。
     * 是否跟随到底部在插入前判断：用户向上滚动查看历史时保持当前位置。
     */
    private fun renderLogs(logs: List<String>) {
        if (logs == renderedLogs) {
            updateLogActions()
            return
        }
        val follow = shouldFollowBottom()
        val previous = renderedLogs
        val common = commonPrefixLength(previous, logs)
        if (common < previous.size) {
            logView.clear()
        }
        logView.printLines(if (common < previous.size) logs else logs.subList(common, logs.size), follow)
        renderedLogs = logs
        updateLogActions()
    }

    /** 日志增删后同步复制/清空图标是否可用；图标条由平台工具栏绘制。 */
    private fun updateLogActions() {
        val enabled = renderedLogs.isNotEmpty()
        if (enabled == logActionsEnabled) {
            return
        }
        logActionsEnabled = enabled
        refreshLogToolbar()
    }

    /** 切换自动换行，并让图标条的选中态跟上当前状态。 */
    private fun setLogWrap(wrap: Boolean) {
        logView.wrapLines = wrap
        refreshLogToolbar()
    }

    /** 切换自动跟随；重新开启时立即滚到末尾。 */
    private fun setAutoScroll(enabled: Boolean) {
        autoScroll = enabled
        if (enabled) {
            logView.requestScrollingToEnd()
        }
        refreshLogToolbar()
    }

    /** 工具栏刷新限定在 EDT；测试线程直接渲染日志时推迟到 EDT 执行。 */
    private fun refreshLogToolbar() {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            logToolbar.updateActionsAsync()
        } else {
            application.invokeLater({ logToolbar.updateActionsAsync() }, ModalityState.nonModal())
        }
    }

    /**
     * 清空本标签页的日志：同时丢弃自己的日志窗口。
     *
     * 只清本标签页，容器缓冲区与其它标签页不受影响；下一次执行重新开窗口。
     */
    private fun clearLogs() {
        logWindows.clear()
        renderedLogs = emptyList()
        logView.clear()
        updateLogActions()
    }

    private fun copyLogs() {
        val text = plainLogs()
        if (text.isEmpty()) {
            return
        }
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    /** 界面当前日志的纯文本；复制时不带 ANSI 颜色转义。 */
    private fun plainLogs(): String = renderedLogs.joinToString("\n") { AnsiLogText.plainText(it) }

    /** 插入日志前判断视图是否在底部附近；用户向上滚动后自动暂停跟随。 */
    private fun shouldFollowBottom(): Boolean = autoScroll && logView.isAtBottom()

    private fun commonPrefixLength(left: List<String>, right: List<String>): Int {
        val limit = minOf(left.size, right.size)
        var index = 0
        while (index < limit && left[index] == right[index]) {
            index++
        }
        return index
    }

    private fun setBusy(value: Boolean, message: String) {
        busy = value
        busyMessage = message
        showExecuting(value)
        setStatusHint(message)
        // 执行按钮与参数开关的可用性由 refreshSnapshot 统一跟随容器状态更新
        refreshSnapshot()
    }

    /** 执行期间把纸飞机换成转圈图标；结束后恢复。 */
    private fun showExecuting(executing: Boolean) {
        if (executing) {
            busySpinner.start()
            executeButton.icon = busySpinner
            // 忙碌时按钮禁用，显式指定禁用态图标，否则会被 LaF 换成灰化的纸飞机
            executeButton.disabledIcon = busySpinner
        } else {
            busySpinner.stop()
            executeButton.disabledIcon = null
            executeButton.icon = LuckyIcons.Send
        }
    }

    /**
     * 让执行按钮与参数开关实时跟随容器状态。
     *
     * 容器状态由后台线程推进（STARTING → READY/FAILED），过渡期间禁止执行，
     * 避免在编译或停止过程中向不可用的容器发请求。
     */
    private fun syncControls(state: DebugContainerService.State?) {
        val transitioning = state == DebugContainerService.State.STARTING ||
            state == DebugContainerService.State.RESTARTING ||
            state == DebugContainerService.State.STOPPING
        executeButton.isEnabled = !busy && module != null && !hasUnsupported && !transitioning
        targetMenuButton.isEnabled = !busy && module != null
        jsonCheckBox.isEnabled = !busy
        webServerCheckBox.isEnabled = !busy
    }

    /** 复杂参数：对象/列表/Map 在表单里只能整段填 JSON，默认直接用 JSON 模式。 */
    private fun isComplexArgument(parameter: DebugParameter): Boolean = when (parameter.kind) {
        DebugParameterKind.OBJECT,
        DebugParameterKind.LIST,
        DebugParameterKind.MAP -> true
        else -> false
    }

    private fun switchArgumentMode() {
        val jsonMode = jsonCheckBox.isSelected
        val layout = argumentCards.layout as CardLayout
        layout.show(argumentCards, if (jsonMode) CARD_JSON else CARD_FORM)
        // 图标工具栏只服务 JSON 编辑；表单模式隐藏右侧图标条
        jsonToolbar.component.isVisible = jsonMode
        jsonPanel.revalidate()
        jsonPanel.repaint()
        // JSON 工具栏的可用状态跟随当前文本与折叠区域
        jsonToolbar.updateActionsAsync()
    }

    /** 保存 Web 开关；已就绪容器不会自动重启，提示下次启动生效。 */
    private fun onWebServerToggled() {
        settings.isDebugWebServerEnabled = webServerCheckBox.isSelected
        val state = module?.let { service.snapshot(it)?.state }
        webServerHint = if (state == DebugContainerService.State.READY) {
            "Web server setting applies after restarting the container"
        } else {
            null
        }
        refreshSnapshot()
    }

    private fun scheduleRefresh() {
        alarm.addRequest({
            if (!disposed) {
                // 定时刷新只做兜底：日志追加会即时触发刷新，单次异常也不能终止轮询。
                scheduleSnapshotRefresh()
                scheduleRefresh()
            }
        }, REFRESH_INTERVAL_MILLIS)
    }

    /**
     * 请求一次界面刷新。
     *
     * 子进程每写一行日志都会走到这里；用 CAS 合并同一批通知，避免 EDT 队列被
     * 大量刷新任务淹没。面板嵌在工具窗口中，非模态刷新即可。
     */
    private fun scheduleSnapshotRefresh() {
        if (disposed || !refreshPending.compareAndSet(false, true)) {
            return
        }
        ApplicationManager.getApplication().invokeLater({
            refreshPending.set(false)
            if (!disposed) {
                runCatching { refreshSnapshot() }
            }
        }, ModalityState.nonModal())
    }

    private fun ui(block: () -> Unit) {
        if (disposed) {
            return
        }
        // 面板已改为工具窗口内嵌，不再被模态对话框包裹，使用非模态刷新。
        ApplicationManager.getApplication().invokeLater({
            if (!disposed) {
                block()
            }
        }, ModalityState.nonModal())
    }

    /**
     * 参数区字体：与 Result/Logs 的编辑器字体一致（等宽，参数名与输入值对齐）。
     *
     * 字段说明仍用界面字体：编辑器字体在部分主题下不含中文字形，说明文案
     * 可能出现方块；参数名、类型与输入值都是代码标识，跟随编辑器即可。
     */
    private fun editorFont(): Font = runCatching {
        val scheme = EditorColorsManager.getInstance().globalScheme
        Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
    }.getOrDefault(UIUtil.getLabelFont())

    /** 参数区背景：与 Result/Logs 编辑器同一 scheme 背景。 */
    private fun editorBackground(): Color = runCatching {
        EditorColorsManager.getInstance().globalScheme.defaultBackground
    }.getOrDefault(UIUtil.getPanelBackground())

    /** 输入控件统一用编辑器字体；容器向下递归，文件选择按钮等一并生效。 */
    private fun applyEditorFont(component: Component) {
        component.font = editorFont()
        if (component is Container) {
            component.components.forEach { applyEditorFont(it) }
        }
    }

    override fun dispose() {
        disposed = true
        busySpinner.stop()
        alarm.cancelAllRequests()
        Disposer.dispose(alarmDisposable)
        resultView.dispose()
        logView.dispose()
    }

    private class ValueInput(
        val parameter: DebugParameter,
        val component: JComponent,
        val read: () -> String,
        val write: (String) -> Unit = {}
    )

    /**
     * 地址栏路径标签：手绘 baseUrl 与 path 两段文本。
     *
     * 不用 HTML `<font color>` 着色：部分 LaF 下会被忽略，整段变成同一颜色；
     * 自绘还能在超出可用宽度时裁剪到标签边界，不覆盖右侧的执行/更多按钮。
     */
    private class RequestUrlLabel : JBLabel() {

        /** 灰色显示的 baseUrl 段；为空时不绘制。 */
        var baseText: String = ""
            set(value) {
                field = value
                repaint()
            }

        /** 正常前景色的接口 path 段；为空时不绘制。 */
        var pathText: String = ""
            set(value) {
                field = value
                repaint()
            }

        init {
            isOpaque = false
        }

        override fun paintComponent(g: Graphics) {
            if (baseText.isEmpty() && pathText.isEmpty()) return
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g2.clipRect(0, 0, width, height)
                g2.font = font
                val metrics = g2.fontMetrics
                var x = insets.left
                val baseline = insets.top +
                    (height - insets.top - insets.bottom - metrics.height) / 2 + metrics.ascent
                if (baseText.isNotEmpty()) {
                    g2.color = UIUtil.getLabelDisabledForeground()
                    g2.drawString(baseText, x, baseline)
                    x += metrics.stringWidth(baseText)
                }
                if (pathText.isNotEmpty()) {
                    g2.color = foreground
                    g2.drawString(pathText, x, baseline)
                }
            } finally {
                g2.dispose()
            }
        }
    }

    companion object {

        private const val CARD_FORM = "form"
        private const val CARD_JSON = "json"
        // 「更多」菜单项及其悬停提示
        private const val METHOD_PATH_OPTION = "Show method path"
        private const val JSON_OPTION = "JSON arguments"
        private const val WEB_SERVER_OPTION = "Web server"
        private const val METHOD_PATH_OPTION_HINT =
            "Show the method icon and the fully qualified method path instead of the HTTP method and URL"
        private const val JSON_OPTION_HINT = "Edit the whole argument list as a JSON object or array"
        private const val WEB_SERVER_OPTION_HINT =
            "Run the embedded web server; takes effect on the next container start"
        private val MORE_OPTIONS = listOf(METHOD_PATH_OPTION, JSON_OPTION, WEB_SERVER_OPTION)
        // 地址输入框：圆角半径、全局字号增量与方法徽标缩放
        private const val INPUT_ARC = 15
        private const val URL_FONT_DELTA = 1f
        private const val METHOD_BADGE_SCALE = 1.3f
        // 去掉类型名的包名前缀：java.util.List<demo.User> -> List<User>
        private val PACKAGE_PREFIX = Regex("""(?:[a-zA-Z_$][\w$]*\.)+([A-Z_$][\w$]*)""")
        // 表单注释列的预览长度（完整内容在 tooltip）
        private const val DESCRIPTION_PREVIEW_LENGTH = 16
        // 对象字段相对分组标题的缩进像素
        private const val FIELD_INDENT = 18
        // 参数区默认高度：首次布局时按实际参数行数折算分割比例，最多露三行
        private const val DEFAULT_ARGUMENTS_PROPORTION = 0.4f
        private const val DEFAULT_ARGUMENT_ROW_COUNT = 3
        // JSON 编辑器的虚拟文件名：扩展名决定语言与折叠构建器
        private const val JSON_EDITOR_FILE_NAME = "lucky-arguments.json"

        /** 地址输入框：纯色圆角底 + 细边框，浅色主题用浅灰底。 */
        private val INPUT_BACKGROUND = JBColor(Color(0xF7F8FA), Color(0x2B2D30))
        private val INPUT_BORDER = JBColor(Color(0xD3D6DC), Color(0x43454A))

        /** 输入框正文颜色：比普通标签灰一档，贴近只读输入框。 */
        private val INPUT_TEXT = JBColor(Color(0x3C4043), Color(0xBCBEC4))
        // 日志由子进程持续写入；轮询间隔太长会让启动中的日志看起来卡住。
        private const val REFRESH_INTERVAL_MILLIS = 300L

        /** 请求结束后等待 stdout 落盘的时延，避免最后几行日志被截在窗口之外。 */
        private const val LOG_WINDOW_CLOSE_DELAY_MILLIS = 500L
    }
}

/**
 * 参数区标题胶囊：与 Result/Logs 选中标签同款。
 *
 * 主题选中底色 + 1px 主题描边 + 平台圆角；字号取参数区的编辑器字体，
 * 与下方参数行保持同一排版。
 */
private class ArgsTab(text: String, font: Font) : JComponent() {

    private val label = text

    init {
        this.font = font
        isOpaque = false
    }

    override fun getPreferredSize(): Dimension {
        val metrics = getFontMetrics(font)
        val insets = JBUI.insets(5, 12)
        return Dimension(
            metrics.stringWidth(label) + insets.left + insets.right,
            metrics.height + insets.top + insets.bottom
        )
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
            val arc = JBUI.scale(SelectedTabStyle.arc).toFloat()
            // 半像素偏移让 1px 描边落在整像素上，圆角边缘不发虚
            val shape = RoundRectangle2D.Float(0.5f, 0.5f, width - 1f, height - 1f, arc, arc)
            g2.color = SelectedTabStyle.background
            g2.fill(shape)
            g2.color = SelectedTabStyle.border
            g2.stroke = BasicStroke(1f)
            g2.draw(shape)
            g2.font = font
            g2.color = UIUtil.getLabelForeground()
            val metrics = g2.fontMetrics
            g2.drawString(
                label,
                (width - metrics.stringWidth(label)) / 2,
                (height - metrics.height) / 2 + metrics.ascent
            )
        } finally {
            g2.dispose()
        }
    }
}
