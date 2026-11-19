package com.luckyframework.idea.debug.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
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
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTabbedPane

/**
 * 接口调试面板：参数表单 + 执行 + 结果/日志。
 *
 * 容器生命周期只使用 [DebugContainerService.start]，调用只使用
 * [DebugContainerService.invoke]；状态与日志通过只读 [DebugContainerService.snapshot]
 * 获取，用于展示进度与失败原因。
 *
 * `start` 会阻塞（ReadAction + 进程启动），因此按钮触发的所有服务调用都在
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

    private val logArea = JBTextArea().apply {
        isEditable = false
        lineWrap = false
        font = messageFont()
        border = JBUI.Borders.empty(6)
    }

    private val tabs = JTabbedPane().apply {
        addTab("Result", JBScrollPane(resultArea))
        addTab("Logs", JBScrollPane(logArea))
    }

    private val rootPanel = JPanel(BorderLayout())

    private val inputFields = LinkedHashMap<String, ValueInput>()

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
                ApplicationManager.getApplication().invokeLater {
                    if (!disposed) {
                        refreshSnapshot()
                    }
                }
            }
        }, alarmDisposable)
        setBusy(false, "")
        refreshSnapshot()
        scheduleRefresh()
    }

    /** 对话框与测试使用的根组件。 */
    val component: JComponent get() = rootPanel

    internal fun inputRowCountForTest(): Int = inputFields.size

    internal fun executeEnabledForTest(): Boolean = executeButton.isEnabled

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
        setBusy(true, "Starting container…")
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
        tabs.selectedIndex = 0
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
            return
        }
        val snapshot = service.snapshot(current)
        if (snapshot == null) {
            statusLabel.text = "STOPPED"
            statusLabel.foreground = colorOf(null)
            return
        }
        statusLabel.text = snapshot.state.name
        statusLabel.foreground = colorOf(snapshot.state)
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
        val text = snapshot.logs.joinToString("\n")
        if (text != logArea.text) {
            logArea.text = text
            if (text.isNotEmpty()) {
                logArea.caretPosition = logArea.document.length
            }
        }
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
        startButton.isEnabled = !value && module != null
        executeButton.isEnabled = !value && module != null && !hasUnsupported
        jsonCheckBox.isEnabled = !value
        webServerCheckBox.isEnabled = !value
        if (!value) {
            refreshSnapshot()
        }
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
                refreshSnapshot()
                scheduleRefresh()
            }
        }, REFRESH_INTERVAL_MILLIS)
    }

    private fun ui(block: () -> Unit) {
        if (disposed) {
            return
        }
        ApplicationManager.getApplication().invokeLater(block)
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

        private val READY_COLOR = JBColor(Color(0x2C8A46), Color(0x3FA85B))
        private val FAILED_COLOR = JBColor(Color(0xB33B33), Color(0xCC4C43))
        private val BUSY_COLOR = JBColor(Color(0xB97A0C), Color(0xD39422))
    }
}
