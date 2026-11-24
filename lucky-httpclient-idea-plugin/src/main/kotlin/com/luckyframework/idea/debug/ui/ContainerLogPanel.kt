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
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.luckyframework.idea.debug.container.DebugContainerService
import java.awt.BorderLayout
import java.awt.datatransfer.StringSelection
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 容器日志面板：只展示子进程日志，不绑定具体接口方法。
 *
 * 从项目行打开，随容器状态/日志变化增量刷新；导航栏把每个模块的日志
 * 作为一个独立标签页，[embedded] 为 true 时标题与关闭由标签页头承担。
 * 日志区右侧提供与调试页 Logs 一致的图标条：单行/多行切换、自动跟随、复制。
 */
class ContainerLogPanel(
    private val project: Project,
    val moduleName: String,
    private val onClose: () -> Unit = {},
    /** 嵌入标签页时隐藏自带标题行，避免与标签页头重复。 */
    embedded: Boolean = false
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

    /** 日志渲染直接复用平台控制台，样式与 Run 控制台一致。 */
    private val logView = ConsoleLogView(project)

    /** 单行/多行切换；默认单行横向滚动，选中后自动换行。 */
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
        .apply { setTargetComponent(logView.component) }

    private val logsPanel = JPanel(BorderLayout()).apply {
        add(logView.component, BorderLayout.CENTER)
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
        isVisible = !embedded
    }

    /** 标签页与测试使用的根组件。 */
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
     * 增量渲染日志：只往平台控制台追加新增行，ANSI 颜色由控制台解码。
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
        // 空提示也算旧内容：previous 为空时文档里可能有提示，需要先清掉
        val full = previous.isEmpty() || common < previous.size
        if (full) {
            logView.clear()
        }
        if (logs.isEmpty()) {
            logView.printPlain(EMPTY_HINT)
        } else {
            logView.printLines(if (full) logs else logs.subList(common, logs.size), follow)
        }
        renderedLogs = logs
        updateLogActions()
    }

    /** 插入日志前判断视图是否在底部附近；用户向上滚动或关闭自动跟随后不再强制跟随。 */
    private fun shouldFollowBottom(): Boolean = autoScroll && logView.isAtBottom()

    /** 切换单行/多行显示，并让图标条的选中态跟上当前状态。 */
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

    private fun commonPrefixLength(first: List<String>, second: List<String>): Int {
        val limit = minOf(first.size, second.size)
        var index = 0
        while (index < limit && first[index] == second[index]) {
            index++
        }
        return index
    }

    override fun dispose() {
        disposed = true
        Disposer.dispose(listenerDisposable)
        logView.dispose()
    }

    private companion object {
        private const val EMPTY_HINT = "No container logs yet."
    }
}
