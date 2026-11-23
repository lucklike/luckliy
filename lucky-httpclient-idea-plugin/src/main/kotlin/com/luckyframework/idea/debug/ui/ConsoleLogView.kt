package com.luckyframework.idea.debug.ui

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Rectangle
import javax.swing.JComponent

/**
 * 日志视图：直接复用平台控制台组件（与 Run 工具窗口同一套渲染）。
 *
 * 字体、前景/背景色、行距与缩进全部来自编辑器 color scheme；
 * ANSI 颜色由平台 [AnsiEscapeDecoder] 解码成控制台内容类型，
 * 与内置控制台使用同一套 Console Colors 色板，不再自行配色。
 */
internal class ConsoleLogView(project: Project) {

    private val console = ConsoleViewImpl(project, true)

    /** 平台 ANSI 解码器；清空后重建，避免继承上一轮未闭合的转义序列。 */
    private var decoder = AnsiEscapeDecoder()

    private val acceptor = object : AnsiEscapeDecoder.ColoredTextAcceptor {
        override fun coloredTextAvailable(text: String, attributes: Key<*>) {
            val type = ConsoleViewContentType.getConsoleViewType(attributes)
                ?: ConsoleViewContentType.NORMAL_OUTPUT
            console.print(text, type)
        }
    }

    /** 恢复滚动位置要等平台的延迟 flush 落盘后再执行。 */
    private val disposeGuard = Disposer.newDisposable("lucky.console.log")

    private val scrollAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, disposeGuard)

    @Volatile
    private var disposed = false

    /** 控制台滚动区；右侧留给操作图标条。 */
    val component: JComponent
        get() = console.component

    /** 平台控制台内部滚动面板；滚动条由控制台自己摆放在右侧。 */
    val scrollPane: JBScrollPane?
        get() = console.editor?.contentComponent?.let {
            UIUtil.findComponentOfType(it, JBScrollPane::class.java)
        }

    /** 编辑器 scheme 的默认背景；与内置控制台一致。 */
    val editorBackground: Color?
        get() = console.editor?.colorsScheme?.defaultBackground

    /** 日志使用的编辑器字体名；与控制台同一等宽字体。 */
    val editorFontFamily: String?
        get() = console.editor?.colorsScheme?.editorFontName

    /** 单行/多行显示：控制台默认单行，由编辑器软换行开关切换。 */
    var wrapLines: Boolean
        get() = console.editor?.settings?.isUseSoftWraps ?: false
        set(value) {
            val editor = console.editor ?: return
            if (editor.settings.isUseSoftWraps != value) {
                editor.settings.isUseSoftWraps = value
            }
        }

    /** 当前视口纵向偏移；关闭自动跟随时用于恢复位置。 */
    fun verticalScrollOffset(): Int = console.editor?.scrollingModel?.verticalScrollOffset ?: 0

    /**
     * 追加日志行。
     *
     * [follow] 为 true 时请求控制台滚到末尾；为 false 时记录打印前的
     * 视口位置，等平台延迟 flush 完成后恢复，避免新日志把视口拉走。
     */
    fun printLines(lines: List<String>, follow: Boolean) {
        if (lines.isEmpty()) {
            return
        }
        val offset = if (follow) -1 else verticalScrollOffset()
        for (line in lines) {
            decoder.escapeText(line + "\n", ProcessOutputType.STDOUT, acceptor)
        }
        if (follow) {
            console.requestScrollingToEnd()
        } else if (offset >= 0) {
            // 控制台的 flush 是延迟调度，恢复动作必须排在它之后
            scrollAlarm.addRequest({ restoreVerticalScroll(offset) }, RESTORE_SCROLL_DELAY_MS)
        }
    }

    /** 输出一段不带 ANSI 样式的普通文本；空日志提示使用。 */
    fun printPlain(text: String) {
        console.print(text, ConsoleViewContentType.NORMAL_OUTPUT)
    }

    /**
     * 清空当前显示。
     *
     * 平台 [ConsoleViewImpl.clear] 只是把 CLEAR 排进 flush 队列，随后立即
     * 追加新日志时新旧内容会交错；这里同步清空文档，保证增量渲染的顺序。
     */
    fun clear() {
        console.clear()
        val document = console.editor?.document
        if (document != null && document.textLength > 0) {
            ApplicationManager.getApplication().runWriteAction {
                document.setText("")
            }
        }
        decoder = AnsiEscapeDecoder()
    }

    fun requestScrollingToEnd() = console.requestScrollingToEnd()

    /** 当前文档文本；复制与测试使用。 */
    fun text(): String {
        val application = ApplicationManager.getApplication()
        if (application.isDispatchThread) {
            console.flushDeferredText()
            return console.editor?.document?.text ?: ""
        }
        // flushDeferredText 只允许在 EDT 调用；测试线程读文本时切过去执行
        var value = ""
        application.invokeAndWait {
            console.flushDeferredText()
            value = console.editor?.document?.text ?: ""
        }
        return value
    }

    fun isAtBottom(): Boolean {
        val editor = console.editor ?: return true
        if (editor.document.textLength == 0) {
            return true
        }
        val visible: Rectangle = editor.scrollingModel.visibleArea
        if (visible.isEmpty) {
            return true
        }
        return visible.y + visible.height >=
            editor.contentComponent.height - JBUI.scale(BOTTOM_THRESHOLD_PX)
    }

    fun dispose() {
        if (disposed) {
            return
        }
        disposed = true
        scrollAlarm.cancelAllRequests()
        Disposer.dispose(disposeGuard)
        // ConsoleViewImpl 构造时以自身为 parent 注册内部 Alarm，平台会把它隐式挂到
        // ROOT_DISPOSABLE 下；直接调用 console.dispose() 只执行释放逻辑，不会从
        // Disposer 树中摘除节点，退出 IDE 时会报 "registered as a child of
        // ROOT_DISPOSABLE but wasn't disposed"。必须走 Disposer 释放，由它级联
        // 子节点并从树中移除。
        Disposer.dispose(console)
    }

    private fun restoreVerticalScroll(offset: Int) {
        if (disposed) {
            return
        }
        console.editor?.scrollingModel?.scrollVertically(offset)
    }

    private companion object {
        /** 距底部多少像素内仍视为停留在末尾。 */
        private const val BOTTOM_THRESHOLD_PX = 24

        /** 平台控制台默认 flush 延迟为 25ms，恢复滚动位置留出余量。 */
        private const val RESTORE_SCROLL_DELAY_MS = 60
    }
}
