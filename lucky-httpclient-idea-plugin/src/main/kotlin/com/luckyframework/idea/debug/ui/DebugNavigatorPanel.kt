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
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.Alarm
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
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities
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
 */
class DebugNavigatorPanel(
    private val project: Project,
    val target: DebugTarget,
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

    private val targetLabel = JBLabel(headerHtml())

    /** 收起按钮：由导航栏把面板从工具窗口下半部分移除。 */
    private val closeButton = JButton(AllIcons.Actions.Close).apply {
        toolTipText = "Hide the debug panel"
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusable = false
        border = JBUI.Borders.empty(2)
        isVisible = onClose != null
        addActionListener { onClose?.invoke() }
    }

    private val progressLabel = JBLabel("").apply {
        foreground = UIUtil.getLabelDisabledForeground()
    }

    /** 执行当前接口方法：容器未就绪时先启动；图标无边框，保持面板紧凑。 */
    private val executeButton = JButton(AllIcons.Actions.Execute).apply {
        toolTipText = "Start the container if needed, then invoke the selected method"
        accessibleContext.accessibleName = "Execute"
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusable = false
        margin = JBUI.insets(2)
        addActionListener { execute() }
    }

    private val jsonCheckBox = JCheckBox("JSON arguments").apply {
        toolTipText = "Edit the whole argument list as a JSON object or array"
        addActionListener { switchArgumentMode() }
    }

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
        // 容器状态与启动/停止已归属导航树项目行，这里只保留执行入口与参数开关
        val optionsRow = JPanel(BorderLayout()).apply {
            add(
                JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
                    add(jsonCheckBox)
                    add(webServerCheckBox)
                },
                BorderLayout.CENTER
            )
            add(executeButton, BorderLayout.EAST)
        }
        val titleRow = JPanel(BorderLayout()).apply {
            add(targetLabel, BorderLayout.CENTER)
            add(closeButton, BorderLayout.EAST)
        }
        val header = JPanel(BorderLayout()).apply {
            add(titleRow, BorderLayout.NORTH)
            add(progressLabel, BorderLayout.CENTER)
            add(optionsRow, BorderLayout.SOUTH)
            border = JBUI.Borders.empty(6, 8, 2, 8)
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

    private fun refreshSnapshot() {
        val current = module
        if (current == null) {
            progressLabel.text = "Module not found: ${target.moduleName}"
            syncControls(null)
            return
        }
        val snapshot = service.snapshot(current)
        if (snapshot == null) {
            // 容器状态归属导航树项目行，面板只展示进度与失败提示
            progressLabel.text = if (busy) busyMessage else ""
            syncControls(null)
            return
        }
        syncControls(snapshot.state)
        // 启动失败可能发生在后台线程；即使 start() 尚未返回，也要先撤掉
        // “Starting container…”，让用户知道失败已经发生。
        progressLabel.text = when {
            snapshot.state == DebugContainerService.State.FAILED ->
                "Container failed to start — see Result / Logs"
            busy -> busyMessage
            else -> webServerHint ?: if (snapshot.autoRestartSuspended) {
                "Auto restart suspended after repeated failures. Use the project action to retry."
            } else {
                ""
            }
        }
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
        progressLabel.text = message
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

    private fun headerHtml(): String {
        val className = StringUtil.escapeXmlEntities(target.className)
        val methodName = StringUtil.escapeXmlEntities(target.methodName)
        val moduleName = StringUtil.escapeXmlEntities(target.moduleName)
        val signature = StringUtil.escapeXmlEntities(target.signature)
        return "<html><b>$className</b>#$methodName<br/>" +
            "<font color='gray'>$moduleName · $signature</font></html>"
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

    companion object {

        private const val CARD_FORM = "form"
        private const val CARD_JSON = "json"
        // 日志由子进程持续写入；轮询间隔太长会让启动中的日志看起来卡住。
        private const val REFRESH_INTERVAL_MILLIS = 300L
        // 距离底部小于该值时仍视为“在底部”，自动滚动继续跟随。
        private const val BOTTOM_THRESHOLD_PX = 24
    }
}
