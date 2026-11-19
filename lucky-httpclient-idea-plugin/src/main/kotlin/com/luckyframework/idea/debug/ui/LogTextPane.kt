package com.luckyframework.idea.debug.ui

import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import javax.swing.JTextPane

/**
 * 日志文本组件：支持 ANSI 样式，并可在单行/多行显示之间切换。
 *
 * [JTextPane] 默认让文本宽度跟随视口并自动换行。单行模式下通过
 * [getScrollableTracksViewportWidth] 关闭宽度跟随，并按内容宽度重写
 * [getPreferredSize]，由外层滚动面板提供横向滚动条；多行模式恢复默认换行。
 */
class LogTextPane : JTextPane() {

    /** true 为多行显示（自动换行）；false 为单行显示（横向滚动）。 */
    var wrapLines: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            revalidate()
            repaint()
        }

    override fun getScrollableTracksViewportWidth(): Boolean = wrapLines

    override fun getPreferredSize(): Dimension {
        if (wrapLines) {
            return super.getPreferredSize()
        }
        val metrics = getFontMetrics(font ?: UIUtil.getLabelFont())
        val text = document.getText(0, document.length)
        val lines = if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
        val insets = insets
        return Dimension(
            // 多留 2px 余量，避免最长行因为舍入仍然被换行
            contentWidth(text, metrics) + insets.left + insets.right + 2,
            lines * metrics.height + insets.top + insets.bottom
        )
    }

    private var cachedLength = -1
    private var cachedWidth = 0

    /** 文档内容被整体重建时调用，避免相同长度的新内容复用旧宽度。 */
    fun invalidateWidthCache() {
        cachedLength = -1
    }

    /** 最长行的像素宽度；布局会频繁调用，按文档长度缓存以避免重复测量。 */
    private fun contentWidth(text: String, metrics: java.awt.FontMetrics): Int {
        if (text.length == cachedLength) {
            return cachedWidth
        }
        var width = 0
        for (line in text.split('\n')) {
            width = maxOf(width, metrics.stringWidth(AnsiLogText.plainText(line)))
        }
        cachedLength = text.length
        cachedWidth = width
        return width
    }
}
