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
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.luckyframework.idea.debug.container.DebugContainerService
import java.awt.BorderLayout
import java.awt.Font
import java.awt.datatransfer.StringSelection
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.text.StyledDocument

/**
 * 容器日志面板：只展示子进程日志，不绑定具体接口方法。
 *
 * 容器启动失败时从项目行打开，之后随容器状态/日志变化增量刷新；
 * 与调试面板共用导航树下方的分栏，二者互斥。日志区右侧提供与调试面板
 * Logs 页一致的图标条：单行/多行切换、自动跟随、复制。
 */
class ContainerLogPanel(
    project: Project,
    val moduleName: String,
    private val onClose: () -> Unit
) : Disposable {

    private val module: Module? = ModuleManager.getInstance(project).findModuleByName(moduleName)

    private val service: DebugContainerService = DebugContainerService.getInstance(project)

    /** 合并日志高频追加触发的刷新：EDT 队列里最多同时有一个待执行的刷新。 */
    private val refreshPending = AtomicBoolean(false)

    /** 已渲染的日志快照，用于增量追加。 */
    private var renderedLogs: List<String> = emptyList()

    /** 自动跟随最新日志；用户向上滚动查看历史或关闭勾选时暂停。 */
    private var autoScroll = true

    /** 复制是否可用；日志非空时由 [updateLogActions] 打开。 */
    private var logActionsEnabled = false

    @Volatile
    private var disposed = false

    private val logPane = LogTextPane().apply {
        isEditable = false
        // 默认单行横向滚动，长行可用右侧图标条切换为多行
        wrapLines = false
        font = messageFont()
        background = UIUtil.getTextFieldBackground()
        foreground = UIUtil.getLabelForeground()
        border = JBUI.Borders.empty(6)
    }

    private val logScroll = JBScrollPane(logPane).apply {
        // Flip 是 JBScrollPane 布局支持的镜像开关：纵向滚动条移到左侧，右侧留给图标列
        putClientProperty(JBScrollPane.Flip::class.java, JBScrollPane.Flip.HORIZONTAL)
    }

    /** 单行/多行切换；默认单行横向滚动，选中后自动换行。 */
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

    /** 复制当前显示的日志，不带 ANSI 颜色转义。 */
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

    /** 日志区右侧的图标条：平台工具栏渲染为紧凑图标，无按钮边框。 */
    private val logToolbar = ActionManager.getInstance()
        .createActionToolbar(
            "LuckyHttpClient.ContainerLogs",
            DefaultActionGroup().apply {
                add(wrapLogsAction)
                add(autoScrollAction)
                add(copyLogsAction)
            },
            false
        )
        .apply { setTargetComponent(logPane) }

    private val logsPanel = JPanel(BorderLayout()).apply {
        add(logScroll, BorderLayout.CENTER)
        add(logToolbar.component, BorderLayout.EAST)
    }

    private val closeButton = JButton(AllIcons.Actions.Close).apply {
        toolTipText = "Hide the container logs"
        isBorderPainted = false
        isContentAreaFilled = false
        isFocusable = false
        border = JBUI.Borders.empty(2)
        addActionListener { onClose() }
    }

    private val header = JPanel(BorderLayout()).apply {
        border = JBUI.Borders.empty(6, 8, 2, 8)
        add(JBLabel("Container Logs — $moduleName"), BorderLayout.CENTER)
        add(closeButton, BorderLayout.EAST)
    }

    /** 工具窗口底部与测试使用的根组件。 */
    val component: JComponent = JPanel(BorderLayout()).apply {
        add(header, BorderLayout.NORTH)
        add(logsPanel, BorderLayout.CENTER)
    }

    /** 监听器挂内部 Disposable：面板由导航栏手动释放，不依赖 Disposer 树。 */
    private val listenerDisposable = Disposer.newDisposable("lucky.container.logs")

    init {
        renderLogs(currentLogs())
        // 首次同步图标条状态，日志为空时复制保持禁用
        refreshLogToolbar()
        if (module != null) {
            service.addListener({ changed ->
                if (changed.name == moduleName) {
                    scheduleRefresh()
                }
            }, listenerDisposable)
        }
    }

    /** 当前应展示的日志：直接取容器日志缓冲的快照。 */
    private fun currentLogs(): List<String> =
        module?.let { service.snapshot(it)?.logs } ?: emptyList()

    private fun scheduleRefresh() {
        if (disposed || !refreshPending.compareAndSet(false, true)) {
            return
        }
        ApplicationManager.getApplication().invokeLater({
            refreshPending.set(false)
            if (!disposed) {
                renderLogs(currentLogs())
            }
        }, ModalityState.nonModal())
    }

    /**
     * 增量渲染日志：只追加新增行，并为每行应用 ANSI 颜色。
     *
     * 环形缓冲滚动导致新旧内容不再有公共前缀时退回全量重设；
     * 是否跟随到底部在插入前判断，用户向上滚动查看历史时保持当前位置。
     */
    private fun renderLogs(logs: List<String>) {
        if (logs == renderedLogs) {
            updateLogActions()
            return
        }
        val follow = shouldFollowBottom()
        val previous = renderedLogs
        val common = if (previous.isEmpty() || logs.isEmpty()) 0 else commonPrefixLength(previous, logs)
        val document = logPane.styledDocument
        // 空提示也算旧内容：previous 为空但文档里有提示时需要先清掉
        if (document.length > 0 && (previous.isEmpty() || common < previous.size)) {
            document.remove(0, document.length)
        }
        if (logs.isEmpty()) {
            document.insertString(0, EMPTY_HINT, null)
        } else {
            appendLines(document, if (common < previous.size) logs else logs.subList(common, logs.size))
        }
        renderedLogs = logs
        logPane.invalidateWidthCache()
        updateLogActions()
        if (follow && logs.isNotEmpty()) {
            scrollToBottom()
        }
    }

    /** 插入日志前判断视图是否在底部附近；用户向上滚动或关闭自动跟随后不再强制跟随。 */
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

    /**
     * 滚动到日志末尾；模型位置在布局完成后才可用，失败时退回移动插入符。
     * 只跟随纵向：横向视口保持用户当前位置，否则长行会把视口持续拉到行尾。
     */
    private fun scrollToBottom() {
        logPane.revalidate()
        SwingUtilities.invokeLater {
            if (disposed || !autoScroll) {
                return@invokeLater
            }
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
    }

    /** 切换单行/多行显示，并让图标条的选中态跟上当前状态。 */
    private fun setLogWrap(wrap: Boolean) {
        logPane.wrapLines = wrap
        refreshLogToolbar()
    }

    /** 切换自动跟随；重新开启时立即滚到末尾。 */
    private fun setAutoScroll(enabled: Boolean) {
        autoScroll = enabled
        if (enabled) {
            scrollToBottom()
        }
        refreshLogToolbar()
    }

    /** 复制当前显示的日志；不带 ANSI 颜色转义。 */
    private fun copyLogs() {
        val text = plainLogs()
        if (text.isEmpty()) {
            return
        }
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    /** 界面当前日志的纯文本。 */
    private fun plainLogs(): String =
        renderedLogs.joinToString("\n") { AnsiLogText.plainText(it) }

    /** 日志增删后同步复制图标是否可用；图标条由平台工具栏绘制。 */
    private fun updateLogActions() {
        val enabled = renderedLogs.isNotEmpty()
        if (enabled == logActionsEnabled) {
            return
        }
        logActionsEnabled = enabled
        refreshLogToolbar()
    }

    /** 工具栏刷新限定在 EDT；测试线直接渲染日志时推迟到 EDT 执行。 */
    private fun refreshLogToolbar() {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            logToolbar.updateActionsAsync()
        } else {
            application.invokeLater({ logToolbar.updateActionsAsync() }, ModalityState.nonModal())
        }
    }

    private fun appendLines(document: StyledDocument, lines: List<String>) {
        val fontFamily = logPane.font.family
        for (line in lines) {
            if (document.length > 0) {
                runCatching { document.insertString(document.length, "\n", null) }
            }
            AnsiLogText.insert(document, line, fontFamily)
        }
    }

    private fun commonPrefixLength(first: List<String>, second: List<String>): Int {
        val limit = minOf(first.size, second.size)
        var index = 0
        while (index < limit && first[index] == second[index]) {
            index++
        }
        return index
    }

    /** 日志字体：优先编辑器等宽字体，不能显示中文时退回界面字体。 */
    private fun messageFont(): Font {
        val scheme = runCatching { EditorColorsManager.getInstance().globalScheme }.getOrNull()
        val editor = if (scheme != null) {
            Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
        } else {
            Font(Font.MONOSPACED, Font.PLAIN, 12)
        }
        return if (editor.canDisplayUpTo("日志连接断开") == -1) editor else UIUtil.getLabelFont()
    }

    override fun dispose() {
        disposed = true
        Disposer.dispose(listenerDisposable)
    }

    private companion object {
        /** 距底部多少像素内仍视为「跟随最新日志」。 */
        private const val BOTTOM_THRESHOLD_PX = 8

        private const val EMPTY_HINT = "No container logs yet."
    }
}
