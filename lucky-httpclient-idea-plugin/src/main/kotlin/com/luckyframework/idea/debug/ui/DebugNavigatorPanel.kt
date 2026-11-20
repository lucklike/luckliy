package com.luckyframework.idea.debug.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
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
import com.luckyframework.idea.debug.invoke.DebugParameter
import com.luckyframework.idea.debug.invoke.DebugParameterKind
import com.luckyframework.idea.debug.invoke.DebugTarget
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GradientPaint
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
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.text.StyledDocument
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
 * 面板不注册到 plugin.xml；由导航栏在工具窗口下半部分按需创建，
 * 关闭时通过 [onClose] 收起，不弹出独立对话框。
 *
 * 顶部是一行毛玻璃样式的地址栏：左侧 V 切换同组件的其它接口与方法徽标，
 * 中间完整请求路径（baseUrl 灰显 + 接口 path 正常色），右侧执行与更多
 * 菜单；签名等细节放在 tooltip。
 */
class DebugNavigatorPanel(
    private val project: Project,
    val target: DebugTarget,
    private val siblings: List<DebugTarget> = emptyList(),
    private val onSwitchTarget: ((DebugTarget) -> Unit)? = null,
    private val onClose: (() -> Unit)? = null
) : Disposable {

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

    /** 收起按钮：由导航栏把面板从工具窗口下半部分移除。 */
    private val closeButton = iconButton(AllIcons.Actions.Close, "Hide the debug panel").apply {
        isVisible = onClose != null
        addActionListener { onClose?.invoke() }
    }

    /** 「更多」按钮：原先独占一行的参数开关收进菜单，纯图案无按钮边框。 */
    private val moreButton = iconButton(AllIcons.Actions.More, "More options").apply {
        accessibleContext.accessibleName = "More options"
        addActionListener { showMorePopup() }
    }

    /** 最近一次状态提示（启动中/失败/自动重启挂起）；只放进请求路径的 tooltip。 */
    private var panelHint: String? = null

    /** 头部路径的纯文本形式；tooltip 提示与测试直接读取。 */
    private var urlText: String = target.displayName

    /**
     * 输入框中部：完整请求路径。baseUrl 用灰色、接口 path 用正常色，
     * 与导航栏的 baseUrl + path 两段展示对应；没有路径信息时回退完整方法名。
     */
    private val urlLabel = RequestUrlLabel().apply {
        minimumSize = Dimension(0, 0)
        font = font.deriveFont(font.size2D + URL_FONT_DELTA)
        foreground = UIUtil.getLabelForeground()
        toolTipText = targetTooltip()
        border = JBUI.Borders.emptyLeft(6)
    }

    /** 请求方法徽标：GET/POST 等按方法着色，与放大的地址栏字号同比例。 */
    private val methodIcon = JBLabel().apply {
        icon = LuckyIcons.httpMethod(target.httpMethod)
            ?.let { IconUtil.scale(it, null, METHOD_BADGE_SCALE) }
            ?: AllIcons.Nodes.Method
        toolTipText = target.displayName
    }

    /** 执行当前接口方法：容器未就绪时先启动；纸飞机纯图案无边框，只占图标尺寸。 */
    private val executeButton = iconButton(
        LuckyIcons.Send,
        "Start the container if needed, then invoke the selected method"
    ).apply {
        accessibleContext.accessibleName = "Execute"
        addActionListener { execute() }
    }

    /** 候选方法下拉：同一组件有多个接口方法时可用，切换后由导航栏重建面板。 */
    private val targetMenuButton = iconButton(
        AllIcons.General.ChevronDown,
        "Select another method of this component"
    ).apply {
        accessibleContext.accessibleName = "Select method"
        isVisible = targets.size > 1
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
     * 头部毛玻璃地址栏：半透明底色 + 顶部受光渐变，不用黑色输入框底；
     * 左侧 V 切换同组件接口与方法徽标，中间完整路径，右侧执行与更多。
     */
    private val addressPanel = object : JPanel(BorderLayout()) {
        override fun paintComponent(g: Graphics) {
            if (width <= 0 || height <= 0) return
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                // 圆角裁剪：半透明底色与高光只画在圆角内部，避免四个小角露出
                val arc = JBUI.scale(GLASS_ARC).toFloat()
                val shape = RoundRectangle2D.Float(
                    0.5f, 0.5f, (width - 1).toFloat(), (height - 1).toFloat(), arc, arc
                )
                g2.color = GLASS_BACKGROUND
                g2.fill(shape)
                val previousClip = g2.clip
                g2.clip(shape)
                g2.paint = GradientPaint(
                    0f, 0f, GLASS_HIGHLIGHT,
                    0f, height * GLASS_HIGHLIGHT_HEIGHT, GLASS_HIGHLIGHT_END
                )
                g2.fillRect(0, 0, width, height)
                g2.clip = previousClip
                g2.color = GLASS_BORDER
                g2.draw(shape)
            } finally {
                g2.dispose()
            }
        }
    }.apply {
        isOpaque = false
        border = JBUI.Borders.empty(7, 8, 7, 6)
        add(
            JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)).apply {
                isOpaque = false
                add(targetMenuButton)
                add(methodIcon)
            },
            BorderLayout.WEST
        )
        add(urlLabel, BorderLayout.CENTER)
        add(
            JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
                isOpaque = false
                add(executeButton)
                add(moreButton)
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

    /**
     * 参数开关不再直接布局，而是作为「更多」菜单里的勾选项。
     * 复选框只保留状态与既有监听逻辑，供测试和菜单读写。
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

    private val parameterPanel = JPanel(GridBagLayout())

    private val parameterScroll = JBScrollPane(parameterPanel)

    private val jsonArea = JBTextArea(DebugArgumentCodec.sampleJson(target.parameters), 8, 60).apply {
        lineWrap = true
        wrapStyleWord = false
        font = codeFont()
    }

    private val jsonScroll = JBScrollPane(jsonArea)

    private val argumentCards = JPanel(CardLayout())

    private val resultArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = false
        font = messageFont()
        border = JBUI.Borders.empty(6)
        text = "Run the method to see the response.\n"
    }

    private val logPane = LogTextPane().apply {
        isEditable = false
        font = messageFont()
        background = UIUtil.getTextFieldBackground()
        foreground = UIUtil.getLabelForeground()
        border = JBUI.Borders.empty(6)
    }

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

        override fun isSelected(e: AnActionEvent): Boolean = logPane.wrapLines

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
        .apply { setTargetComponent(logPane) }

    /** 日志滚动区：纵向滚动条固定在左侧，右侧留给操作图标列。 */
    private val logScroll = JBScrollPane(logPane).apply {
        // Flip 是 JBScrollPane 布局支持的镜像开关，只移动滚动条，不影响文本方向
        putClientProperty(JBScrollPane.Flip::class.java, JBScrollPane.Flip.HORIZONTAL)
    }

    private val logsPanel = JPanel(BorderLayout()).apply {
        add(logScroll, BorderLayout.CENTER)
        add(logToolbar.component, BorderLayout.EAST)
    }

    private val tabs = JTabbedPane().apply {
        addTab("Result", JBScrollPane(resultArea))
        addTab("Logs", logsPanel)
    }

    private val rootPanel = JPanel(BorderLayout())

    private val inputFields = LinkedHashMap<String, ValueInput>()

    /** 上一次渲染到 Logs 的完整快照，用于增量追加新日志行。 */
    private var renderedLogs: List<String> = emptyList()

    /**
     * 界面清空时日志缓冲的下一行序号；之后只渲染序号更大的新日志。
     * 只清界面，不修改容器缓冲区；重新打开面板会重新显示缓冲区内容。
     */
    private var clearedSequence: Long = -1L

    /** 合并日志高频追加触发的刷新：EDT 队列里最多同时有一个待执行的刷新。 */
    private val refreshPending = AtomicBoolean(false)

    private var busy = false

    /** busy 期间的进度文案；失败时 [refreshSnapshot] 会优先展示失败提示。 */
    private var busyMessage = ""

    @Volatile
    private var disposed = false

    private val hasUnsupported = target.parameters.any { it.kind == DebugParameterKind.UNSUPPORTED }

    init {
        buildParameterRows()
        argumentCards.add(parameterScroll, CARD_FORM)
        argumentCards.add(jsonScroll, CARD_JSON)
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

    /** 工具窗口底部与测试使用的根组件。 */
    val component: JComponent get() = rootPanel

    internal fun inputRowCountForTest(): Int = inputFields.size

    internal fun executeEnabledForTest(): Boolean = executeButton.isEnabled

    internal fun targetMenuVisibleForTest(): Boolean = targetMenuButton.isVisible

    internal fun targetLabelsForTest(): List<String> = targets.map(::menuLabelOf)

    /** 「更多」菜单里的开关项，对应原先独占一行的两个复选框。 */
    internal fun moreOptionsForTest(): List<String> = MORE_OPTIONS

    /** 头部展示的完整路径；没有路径信息时为完整方法名。 */
    internal fun requestUrlForTest(): String = urlText

    /** 头部方法徽标对应的请求方法。 */
    internal fun requestMethodForTest(): String? = target.httpMethod

    internal fun renderLogsForTest(logs: List<String>) = renderLogs(logs)

    internal fun logTextForTest(): String = logPane.text

    internal fun logWrapEnabledForTest(): Boolean = logPane.wrapLines

    internal fun logWrapLinesForTest(): Boolean = logPane.wrapLines

    internal fun logTracksViewportWidthForTest(): Boolean = logPane.scrollableTracksViewportWidth

    internal fun toggleLogWrapForTest() {
        setLogWrap(!logPane.wrapLines)
    }

    /** 纵向滚动条是否排在日志区左侧；由滚动面板镜像开关驱动。 */
    internal fun logScrollBarLeftSideForTest(): Boolean {
        val flip = logScroll.getClientProperty(JBScrollPane.Flip::class.java)
        return flip == JBScrollPane.Flip.HORIZONTAL || flip == JBScrollPane.Flip.BOTH
    }

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

    internal fun isWebServerEnabledForTest(): Boolean = webServerCheckBox.isSelected

    internal fun switchJsonModeForTest(selected: Boolean) {
        jsonCheckBox.isSelected = selected
        switchArgumentMode()
    }

    private fun buildLayout() {
        // 头部一行输入框样式：V 切换接口 + 完整方法名 + 执行/更多，最右收起
        val header = JPanel(BorderLayout(2, 0)).apply {
            add(addressPanel, BorderLayout.CENTER)
            add(closeButton, BorderLayout.EAST)
            border = JBUI.Borders.empty(4, 6, 2, 6)
        }

        val arguments = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Arguments")
            add(argumentCards, BorderLayout.CENTER)
        }

        // 内嵌在工具窗口下半部分：参数在上、结果/日志在下，分隔条可拖动
        val splitter = OnePixelSplitter(true, 0.5f).apply {
            firstComponent = arguments
            secondComponent = tabs
        }

        rootPanel.add(header, BorderLayout.NORTH)
        rootPanel.add(splitter, BorderLayout.CENTER)
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
        if (targets.size <= 1 || busy || !targetMenuButton.isEnabled) {
            return
        }
        val labels = targets.map(::menuLabelOf)
        JBPopupFactory.getInstance()
            .createPopupChooserBuilder(labels)
            .setTitle("Select Method")
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
            JSON_OPTION to JSON_OPTION_HINT,
            WEB_SERVER_OPTION to (webServerHint ?: WEB_SERVER_OPTION_HINT)
        )
        val group = DefaultActionGroup().apply {
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

    /** 菜单勾选项：选中态与可用性跟随同名的隐藏复选框。 */
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

    private fun buildParameterRows() {
        parameterPanel.removeAll()
        if (target.parameters.isEmpty()) {
            parameterPanel.add(JBLabel("No arguments"))
            return
        }
        var row = 0
        for (parameter in target.parameters) {
            val input = inputFor(parameter)
            inputFields[parameter.name] = input
            parameterPanel.add(
                JBLabel(parameter.name).apply {
                    toolTipText = "${parameter.typeName} — ${DebugArgumentCodec.exampleOf(parameter)}"
                },
                GridBagConstraints().apply {
                    gridx = 0
                    gridy = row
                    weightx = 0.0
                    anchor = GridBagConstraints.WEST
                    insets = Insets(3, 4, 3, 8)
                }
            )
            parameterPanel.add(
                input.component,
                GridBagConstraints().apply {
                    gridx = 1
                    gridy = row
                    weightx = 1.0
                    fill = GridBagConstraints.HORIZONTAL
                    insets = Insets(3, 0, 3, 8)
                }
            )
            parameterPanel.add(
                JBLabel(typeHint(parameter)).apply {
                    font = font.deriveFont(font.size2D - 1f)
                    foreground = UIUtil.getLabelDisabledForeground()
                    toolTipText = parameter.typeName
                },
                GridBagConstraints().apply {
                    gridx = 2
                    gridy = row
                    weightx = 0.0
                    anchor = GridBagConstraints.WEST
                    insets = Insets(3, 0, 3, 4)
                }
            )
            row++
        }
        parameterPanel.add(
            JPanel(),
            GridBagConstraints().apply {
                gridx = 0
                gridy = row
                gridwidth = 3
                weighty = 1.0
                fill = GridBagConstraints.BOTH
            }
        )
    }

    private fun inputFor(parameter: DebugParameter): ValueInput = when (parameter.kind) {
        DebugParameterKind.BOOLEAN -> {
            val combo = JComboBox(arrayOf("", "true", "false"))
            ValueInput(parameter, combo) { combo.selectedItem?.toString().orEmpty() }
        }

        DebugParameterKind.ENUM -> {
            val items = arrayOf("") + parameter.enumConstants.toTypedArray()
            val combo = JComboBox(items)
            ValueInput(parameter, combo) { combo.selectedItem?.toString().orEmpty() }
        }

        DebugParameterKind.LIST,
        DebugParameterKind.MAP,
        DebugParameterKind.OBJECT -> {
            val area = JBTextArea(DebugArgumentCodec.exampleOf(parameter), 3, 40).apply {
                lineWrap = true
                wrapStyleWord = false
                font = codeFont()
            }
            ValueInput(parameter, JBScrollPane(area)) { area.text }
        }

        DebugParameterKind.UNSUPPORTED -> {
            val field = JBTextField(parameter.unsupportedReason ?: "Unsupported").apply {
                isEnabled = false
            }
            ValueInput(parameter, field) { "" }
        }

        else -> {
            val field = JBTextField().apply {
                toolTipText = "${parameter.typeName} — ${DebugArgumentCodec.exampleOf(parameter)}"
            }
            ValueInput(parameter, field) { field.text }
        }
    }

    private fun typeHint(parameter: DebugParameter): String {
        val simple = parameter.typeName.substringAfterLast('.')
        return if (parameter.required) simple else "$simple?"
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
        val jsonMode = jsonCheckBox.isSelected
        val webServer = webServerCheckBox.isSelected
        val values = if (jsonMode) emptyMap() else inputFields.mapValues { it.value.read() }
        val jsonText = if (jsonMode) jsonArea.text else null

        setBusy(true, "Starting container and invoking…")
        AppExecutorUtil.getAppExecutorService().execute {
            try {
                val startError = ensureStarted(current, webServer)
                if (startError != null) {
                    ui {
                        showMessage(startError)
                        refreshSnapshot()
                        setBusy(false, "")
                    }
                    return@execute
                }
                val outcome = executor.invoke(target, values, jsonText)
                ui {
                    renderOutcome(outcome)
                    refreshSnapshot()
                    setBusy(false, "")
                }
            } catch (e: Throwable) {
                ui {
                    showMessage("Unexpected error: ${e.message ?: e.javaClass.name}")
                    setBusy(false, "")
                }
            }
        }
    }

    /** 后台线程执行；容器未就绪时先启动，失败返回给用户看的文本。 */
    private fun ensureStarted(module: Module, webServer: Boolean): String? {
        val snapshot = service.snapshot(module)
        if (snapshot?.state == DebugContainerService.State.READY) {
            return null
        }
        webServerHint = null
        return describeStartFailure(
            service.start(module, programArgs = DebugContainerService.programArgsFor(webServer))
        )
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
        resultArea.text = when (outcome) {
            is DebugCallOutcome.Success -> buildString {
                append("Return type: ").append(outcome.returnType ?: "?").append('\n')
                append("Result kind: ").append(outcome.kind).append('\n')
                append("Elapsed: ").append(outcome.elapsedMs).append(" ms\n\n")
                append(outcome.body)
            }

            is DebugCallOutcome.Failure -> buildString {
                append("ERROR [").append(outcome.code.code).append("] ")
                append(outcome.code.displayName).append('\n')
                append(outcome.message)
                outcome.detail?.takeIf { it.isNotBlank() }?.let {
                    append("\n\n").append(it)
                }
                if (outcome.elapsedMs > 0) {
                    append("\n\nElapsed: ").append(outcome.elapsedMs).append(" ms")
                }
            }
        }
        resultArea.caretPosition = 0
    }

    private fun showMessage(message: String) {
        resultArea.text = message
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
     * 生成头部路径展示：baseUrl 灰显、接口 path 正常色。
     *
     * 两段颜色与图一的地址栏一致：灰色表示固定的协议/host 前缀，
     * 正常色表示本次请求的接口路径；缺任一段时整段使用可用的一段。
     */
    private fun updateRequestUrl() {
        val baseUrl = target.baseUrl.orEmpty()
        val path = target.requestPath.orEmpty()
        if (baseUrl.isEmpty() && path.isEmpty()) {
            urlText = target.displayName
            urlLabel.baseText = ""
            urlLabel.pathText = urlText
            return
        }
        val (fullUrl, pathStart) = joinRequestUrl(baseUrl, path)
        urlText = fullUrl
        // 两段文本分别着色：baseUrl 灰显、path 用正常前景色，由标签自绘，不依赖 HTML
        urlLabel.baseText = fullUrl.substring(0, pathStart)
        urlLabel.pathText = fullUrl.substring(pathStart)
        // 路径解析完成后同步 tooltip：窄面板裁剪时仍可悬停查看完整 URL
        urlLabel.toolTipText = targetTooltip()
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

    /** 清空后的日志只需要渲染序号大于 [clearedSequence] 的部分。 */
    private fun visibleLogs(snapshot: DebugContainerService.Snapshot): List<String> {
        if (clearedSequence < 0L) {
            return snapshot.logs
        }
        val skip = (clearedSequence - snapshot.logStartSequence)
            .coerceIn(0L, snapshot.logs.size.toLong())
            .toInt()
        return if (skip == 0) snapshot.logs else snapshot.logs.drop(skip)
    }

    /**
     * 增量渲染日志：只追加新增行，并为每行应用 ANSI 颜色。
     *
     * 日志被清空或环形缓冲滚动导致新旧内容不再有公共前缀时退回全量重设。
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
        val document = logPane.styledDocument
        if (common < previous.size) {
            document.remove(0, document.length)
            appendLogLines(document, logs)
        } else {
            appendLogLines(document, logs.subList(common, logs.size))
        }
        renderedLogs = logs
        logPane.invalidateWidthCache()
        updateLogActions()
        if (follow && logs.isNotEmpty()) {
            scrollToBottomDeferred()
        }
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
        logPane.wrapLines = wrap
        refreshLogToolbar()
    }

    /** 切换自动跟随；重新开启时立即滚到末尾。 */
    private fun setAutoScroll(enabled: Boolean) {
        autoScroll = enabled
        if (enabled) {
            scrollToBottomDeferred()
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
     * 清空界面日志；容器缓冲区不变，只记录当前缓冲位置。
     *
     * 之后只渲染序号更新的日志，重新打开面板会重新显示缓冲区内容。
     */
    private fun clearLogs() {
        clearedSequence = module?.let { service.snapshot(it)?.logNextSequence } ?: -1L
        renderedLogs = emptyList()
        logPane.document.remove(0, logPane.document.length)
        logPane.invalidateWidthCache()
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
    private fun shouldFollowBottom(): Boolean {
        if (!autoScroll) {
            return false
        }
        if (logPane.document.length == 0) {
            return true
        }
        val viewport = logScroll.viewport
        val view = viewport.viewRect
        if (view.isEmpty) {
            return true
        }
        return view.y + view.height >= viewport.viewSize.height - JBUI.scale(BOTTOM_THRESHOLD_PX)
    }

    private fun scrollToBottomDeferred() {
        logPane.revalidate()
        SwingUtilities.invokeLater {
            if (!disposed && autoScroll) {
                scrollToBottom()
            }
        }
    }

    /**
     * 滚动到日志末尾；模型位置在布局完成后才可用，失败时退回移动插入符。
     *
     * 追加日志只跟随纵向：横向视口保持用户当前位置（默认最左），
     * 否则长行会把视口持续拉到行尾，实时打印时看不清行首。
     */
    private fun scrollToBottom() {
        val document = logPane.document
        val horizontal = logScroll.horizontalScrollBar.value
        val location = runCatching { logPane.modelToView2D(document.length) }.getOrNull()
        if (location != null) {
            logPane.scrollRectToVisible(location.bounds)
        } else {
            logPane.caretPosition = document.length
        }
        if (logScroll.horizontalScrollBar.value != horizontal) {
            logScroll.horizontalScrollBar.value = horizontal
        }
    }

    private fun appendLogLines(document: StyledDocument, lines: List<String>) {
        val fontFamily = logPane.font.family
        for (line in lines) {
            if (document.length > 0) {
                runCatching { document.insertString(document.length, "\n", null) }
            }
            AnsiLogText.insert(document, line, fontFamily)
        }
    }

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
        setStatusHint(message)
        // 执行按钮与参数开关的可用性由 refreshSnapshot 统一跟随容器状态更新
        refreshSnapshot()
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

    private fun switchArgumentMode() {
        val layout = argumentCards.layout as CardLayout
        layout.show(argumentCards, if (jsonCheckBox.isSelected) CARD_JSON else CARD_FORM)
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

    private fun codeFont(): Font {
        val scheme = runCatching { EditorColorsManager.getInstance().globalScheme }.getOrNull()
        return if (scheme != null) {
            Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
        } else {
            Font(Font.MONOSPACED, Font.PLAIN, 12)
        }
    }

    /** 日志和结果可能含中文；编辑器字体缺少中文字形时回退到 UI 字体。 */
    private fun messageFont(): Font {
        val editor = codeFont()
        return if (editor.canDisplayUpTo("日志连接断开") == -1) editor else UIUtil.getLabelFont()
    }

    override fun dispose() {
        disposed = true
        alarm.cancelAllRequests()
        Disposer.dispose(alarmDisposable)
    }

    private class ValueInput(
        val parameter: DebugParameter,
        val component: JComponent,
        val read: () -> String
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
        private const val JSON_OPTION = "JSON arguments"
        private const val WEB_SERVER_OPTION = "Web server"
        private const val JSON_OPTION_HINT = "Edit the whole argument list as a JSON object or array"
        private const val WEB_SERVER_OPTION_HINT =
            "Run the embedded web server; takes effect on the next container start"
        private val MORE_OPTIONS = listOf(JSON_OPTION, WEB_SERVER_OPTION)
        // 毛玻璃地址栏：圆角半径、顶部高光覆盖比例、全局字号增量与方法徽标缩放
        private const val GLASS_ARC = 9
        private const val GLASS_HIGHLIGHT_HEIGHT = 0.7f
        private const val URL_FONT_DELTA = 2f
        private const val METHOD_BADGE_SCALE = 1.3f

        /** 毛玻璃底色：浅色主题高透明度白色，深色主题低透明度白色提亮，不用黑色输入框底。 */
        private val GLASS_BACKGROUND = JBColor(Color(0x9CFFFFFF.toInt(), true), Color(0x1AFFFFFF, true))

        /** 顶部受光渐变（向下淡出）与 1px 玻璃边缘。 */
        private val GLASS_HIGHLIGHT = JBColor(Color(0x38FFFFFF, true), Color(0x0DFFFFFF, true))
        private val GLASS_HIGHLIGHT_END = Color(0x00FFFFFF, true)
        private val GLASS_BORDER = JBColor(Color(0x33000000, true), Color(0x2EFFFFFF, true))
        // 日志由子进程持续写入；轮询间隔太长会让启动中的日志看起来卡住。
        private const val REFRESH_INTERVAL_MILLIS = 300L
        // 距离底部小于该值时仍视为“在底部”，自动滚动继续跟随。
        private const val BOTTOM_THRESHOLD_PX = 24
    }
}
