package com.luckyframework.idea.debug.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.JBColor
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
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.datatransfer.StringSelection
import javax.swing.Action
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
 * 面板不注册到 plugin.xml；下一批接入导航栏时通过 [open] 打开对话框。
 */
class DebugNavigatorPanel(
    private val project: Project,
    private val target: DebugTarget
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

    private val statusLabel = JBLabel("STOPPED").apply {
        font = font.deriveFont(Font.BOLD)
        foreground = JBColor.GRAY
    }

    private val progressLabel = JBLabel("").apply {
        foreground = UIUtil.getLabelDisabledForeground()
    }

    private val startButton = JButton("Start Container").apply {
        toolTipText = "Start or restart the debug container for this module"
        addActionListener { startContainer() }
    }

    private val stopButton = JButton("Stop Container").apply {
        toolTipText = "Stop the running debug container for this module"
        addActionListener { stopContainer() }
    }

    private val executeButton = JButton("Execute").apply {
        toolTipText = "Start the container if needed, then invoke the selected method"
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

    /** Logs 单行/多行切换；默认单行，长日志横向滚动。 */
    private val logWrapCheckBox = JCheckBox("Wrap lines", false).apply {
        toolTipText = "Wrap long log lines; disabled keeps one line per entry and scrolls horizontally"
        addActionListener { logPane.wrapLines = isSelected }
    }

    /** 自动跟随最新日志；用户向上滚动查看历史时会暂停，回到底部后恢复。 */
    private val autoScrollCheckBox = JCheckBox("Auto scroll", true).apply {
        toolTipText = "Follow the newest log line; scrolling up pauses following until you return to the bottom"
        addActionListener { if (isSelected) scrollToBottomDeferred() }
    }

    private val copyLogsButton = JButton("Copy Logs").apply {
        toolTipText = "Copy the displayed log lines without ANSI color escapes"
        addActionListener { copyLogs() }
        isEnabled = false
    }

    private val clearLogsButton = JButton("Clear Logs").apply {
        toolTipText = "Clear the log view only; the container log buffer is kept"
        addActionListener { clearLogs() }
        isEnabled = false
    }

    private val logScroll = JBScrollPane(logPane)

    private val logsPanel = JPanel(BorderLayout()).apply {
        val options = JPanel(FlowLayout(FlowLayout.LEFT, 4, 2)).apply {
            add(logWrapCheckBox)
            add(autoScrollCheckBox)
            add(copyLogsButton)
            add(clearLogsButton)
        }
        add(options, BorderLayout.NORTH)
        add(logScroll, BorderLayout.CENTER)
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
        service.addListener({ changed ->
            if (changed.name == target.moduleName) {
                scheduleSnapshotRefresh()
            }
        }, alarmDisposable)
        setBusy(false, "")
        scheduleRefresh()
    }

    /** 对话框与测试使用的根组件。 */
    val component: JComponent get() = rootPanel

    internal fun inputRowCountForTest(): Int = inputFields.size

    internal fun executeEnabledForTest(): Boolean = executeButton.isEnabled

    internal fun startButtonTextForTest(): String = startButton.text

    internal fun startButtonEnabledForTest(): Boolean = startButton.isEnabled

    internal fun stopButtonEnabledForTest(): Boolean = stopButton.isEnabled

    internal fun renderLogsForTest(logs: List<String>) = renderLogs(logs)

    internal fun logTextForTest(): String = logPane.text

    internal fun logWrapEnabledForTest(): Boolean = logWrapCheckBox.isSelected

    internal fun logWrapLinesForTest(): Boolean = logPane.wrapLines

    internal fun logTracksViewportWidthForTest(): Boolean = logPane.scrollableTracksViewportWidth

    internal fun toggleLogWrapForTest() {
        logWrapCheckBox.doClick()
    }

    internal fun autoScrollEnabledForTest(): Boolean = autoScrollCheckBox.isSelected

    internal fun toggleAutoScrollForTest() {
        autoScrollCheckBox.doClick()
    }

    internal fun copyableLogsForTest(): String = plainLogs()

    internal fun copyLogsEnabledForTest(): Boolean = copyLogsButton.isEnabled

    internal fun clearLogsForTest() = clearLogs()

    internal fun applyStateForTest(state: DebugContainerService.State?) {
        syncControls(state)
    }

    internal fun isJsonModeForTest(): Boolean = jsonCheckBox.isSelected

    internal fun isWebServerEnabledForTest(): Boolean = webServerCheckBox.isSelected

    internal fun switchJsonModeForTest(selected: Boolean) {
        jsonCheckBox.isSelected = selected
        switchArgumentMode()
    }

    private fun buildLayout() {
        val buttons = JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).apply {
            add(webServerCheckBox)
            add(startButton)
            add(stopButton)
            add(executeButton)
            add(jsonCheckBox)
        }
        val statusRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            add(statusLabel)
            add(progressLabel)
        }
        val toolbar = JPanel(BorderLayout()).apply {
            add(statusRow, BorderLayout.WEST)
            add(buttons, BorderLayout.EAST)
        }
        val header = JPanel(BorderLayout()).apply {
            add(targetLabel, BorderLayout.NORTH)
            add(toolbar, BorderLayout.SOUTH)
            border = JBUI.Borders.empty(6, 8, 2, 8)
        }

        val arguments = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Arguments")
            add(argumentCards, BorderLayout.CENTER)
        }

        val splitter = OnePixelSplitter(false, 0.42f).apply {
            firstComponent = arguments
            secondComponent = tabs
        }

        rootPanel.add(header, BorderLayout.NORTH)
        rootPanel.add(splitter, BorderLayout.CENTER)
        rootPanel.preferredSize = Dimension(JBUI.scale(980), JBUI.scale(640))
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

    private fun startContainer() {
        if (busy) {
            return
        }
        val current = module
        if (current == null) {
            showMessage("Module not found: ${target.moduleName}")
            return
        }
        setBusy(true, "Compiling and starting container…")
        // 启动期间切到 Logs，子进程的 Spring 日志会随轮询实时出现。
        tabs.selectedIndex = 1
        webServerHint = null
        val webServer = webServerCheckBox.isSelected
        AppExecutorUtil.getAppExecutorService().execute {
            val result = runCatching {
                service.start(current, programArgs = DebugContainerService.programArgsFor(webServer))
            }
                .getOrElse { DebugContainerService.StartResult.Failed(it.message ?: it.javaClass.name, emptyList()) }
            ui {
                renderStartResult(result)
                setBusy(false, "")
            }
        }
    }

    /** 停止容器：`stop` 会等待子进程退出，必须在后台线程执行。 */
    private fun stopContainer() {
        if (busy) {
            return
        }
        val current = module
        if (current == null) {
            showMessage("Module not found: ${target.moduleName}")
            return
        }
        setBusy(true, "Stopping container…")
        AppExecutorUtil.getAppExecutorService().execute {
            val error = runCatching { service.stop(current) }.exceptionOrNull()
            ui {
                if (error != null) {
                    showMessage("Failed to stop container: ${error.message ?: error.javaClass.name}")
                }
                setBusy(false, "")
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

    private fun renderStartResult(result: DebugContainerService.StartResult) {
        when (result) {
            is DebugContainerService.StartResult.Ready -> {
                val snapshot = result.snapshot
                resultArea.text = buildString {
                    append("Container ready\n")
                    append("Module: ").append(snapshot.moduleName).append('\n')
                    append("Main class: ").append(snapshot.mainClass ?: "?").append('\n')
                    result.snapshot.info?.let { info ->
                        append("JDK: ").append(info.jdkVersion).append('\n')
                        append("Launcher: ").append(info.launcherVersion)
                        append(" (protocol ").append(info.protocolVersion).append(")\n")
                        append("Proxy factory bean: ")
                        append(if (info.beanPresent) "present" else "missing").append('\n')
                        append("PID: ").append(info.pid).append("  port: ").append(info.port)
                    }
                }
            }

            else -> resultArea.text = describeStartFailure(result)
        }
        tabs.selectedIndex = 0
        refreshSnapshot()
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
            statusLabel.text = "NO MODULE"
            statusLabel.foreground = colorOf(null)
            syncControls(null)
            return
        }
        val snapshot = service.snapshot(current)
        if (snapshot == null) {
            statusLabel.text = "STOPPED"
            statusLabel.foreground = colorOf(null)
            syncControls(null)
            return
        }
        statusLabel.text = snapshot.state.name
        statusLabel.foreground = colorOf(snapshot.state)
        syncControls(snapshot.state)
        // 启动失败可能发生在后台线程；即使 start() 尚未返回，也要先撤掉
        // “Starting container…”，让用户知道失败已经发生。
        progressLabel.text = when {
            snapshot.state == DebugContainerService.State.FAILED ->
                "Container failed to start — see Result / Logs"
            busy -> busyMessage
            else -> webServerHint ?: if (snapshot.autoRestartSuspended) {
                "Auto restart suspended after repeated failures. Start Container to retry."
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

    private fun updateLogActions() {
        copyLogsButton.isEnabled = renderedLogs.isNotEmpty()
        clearLogsButton.isEnabled = renderedLogs.isNotEmpty()
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
        if (!autoScrollCheckBox.isSelected) {
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
            if (!disposed && autoScrollCheckBox.isSelected) {
                scrollToBottom()
            }
        }
    }

    /** 滚动到日志末尾；模型位置在布局完成后才可用，失败时退回移动插入符。 */
    private fun scrollToBottom() {
        val document = logPane.document
        val location = runCatching { logPane.modelToView2D(document.length) }.getOrNull()
        if (location != null) {
            logPane.scrollRectToVisible(location.bounds)
        } else {
            logPane.caretPosition = document.length
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

    private fun colorOf(state: DebugContainerService.State?): Color = when (state) {
        DebugContainerService.State.READY -> READY_COLOR
        DebugContainerService.State.FAILED,
        DebugContainerService.State.SUSPENDED -> FAILED_COLOR
        DebugContainerService.State.STARTING,
        DebugContainerService.State.RESTARTING,
        DebugContainerService.State.STOPPING -> BUSY_COLOR
        else -> JBColor.GRAY
    }

    private fun setBusy(value: Boolean, message: String) {
        busy = value
        busyMessage = message
        progressLabel.text = message
        // 按钮文案与可用性由 refreshSnapshot 统一跟随容器状态更新
        refreshSnapshot()
    }

    /**
     * 让按钮文案与可用性实时跟随容器状态。
     *
     * 容器状态由后台线程推进（STARTING → READY/FAILED），按钮也必须随之更新，
     * 否则启动成功后界面仍显示 “Start Container”，看起来像没有启动成功。
     */
    private fun syncControls(state: DebugContainerService.State?) {
        val transitioning = state == DebugContainerService.State.STARTING ||
            state == DebugContainerService.State.RESTARTING ||
            state == DebugContainerService.State.STOPPING
        startButton.text = when (state) {
            DebugContainerService.State.READY -> "Restart Container"
            DebugContainerService.State.STARTING -> "Starting…"
            DebugContainerService.State.RESTARTING -> "Restarting…"
            DebugContainerService.State.STOPPING -> "Stopping…"
            else -> if (busy) "Starting…" else "Start Container"
        }
        startButton.toolTipText = if (state == DebugContainerService.State.READY) {
            "Restart the running debug container for this module"
        } else {
            "Start the debug container for this module"
        }
        startButton.isEnabled = !busy && module != null && !transitioning
        stopButton.isEnabled = !busy && module != null && state == DebugContainerService.State.READY
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
     * 大量刷新任务淹没。面板处于模态对话框内，必须使用 [ModalityState.any()]，
     * 否则启动/执行期间界面不会更新。
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
        }, ModalityState.any())
    }

    private fun ui(block: () -> Unit) {
        if (disposed) {
            return
        }
        // 模态对话框打开期间 NON_MODAL 的 runnable 不会执行，启动完成后的
        // 结果渲染会被一直挂起；这里必须使用 ModalityState.any()。
        ApplicationManager.getApplication().invokeLater({
            if (!disposed) {
                block()
            }
        }, ModalityState.any())
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

    private class DebugNavigatorDialog(
        project: Project,
        private val target: DebugTarget
    ) : DialogWrapper(project) {

        private val panel = DebugNavigatorPanel(project, target)

        init {
            title = "Lucky Debug: ${target.className}.${target.methodName}"
            super.init()
        }

        override fun createCenterPanel(): JComponent = panel.component

        override fun createActions(): Array<Action> = arrayOf(cancelAction)

        override fun dispose() {
            Disposer.dispose(panel)
            super.dispose()
        }
    }

    companion object {

        /** 打开调试对话框；调用方应已在 read action 中构建 [DebugTarget]。 */
        fun open(project: Project, target: DebugTarget) {
            DebugNavigatorDialog(project, target).show()
        }

        private const val CARD_FORM = "form"
        private const val CARD_JSON = "json"
        // 日志由子进程持续写入；轮询间隔太长会让启动中的日志看起来卡住。
        private const val REFRESH_INTERVAL_MILLIS = 300L
        // 距离底部小于该值时仍视为“在底部”，自动滚动继续跟随。
        private const val BOTTOM_THRESHOLD_PX = 24

        private val READY_COLOR = JBColor(Color(0x2C8A46), Color(0x3FA85B))
        private val FAILED_COLOR = JBColor(Color(0xB33B33), Color(0xCC4C43))
        private val BUSY_COLOR = JBColor(Color(0xB97A0C), Color(0xD39422))
    }
}
