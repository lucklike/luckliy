package com.luckyframework.idea.debug.ui

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil

class LogTextPaneTest : BasePlatformTestCase() {

    fun `test defaults to single line display`() {
        val pane = LogTextPane()

        assertFalse(pane.wrapLines)
        assertFalse(pane.scrollableTracksViewportWidth)
    }

    fun `test wrap toggle switches viewport tracking`() {
        val pane = LogTextPane()

        pane.wrapLines = true
        assertTrue(pane.wrapLines)
        assertTrue(pane.scrollableTracksViewportWidth)

        pane.wrapLines = false
        assertFalse(pane.wrapLines)
        assertFalse(pane.scrollableTracksViewportWidth)
    }

    fun `test single line preferred width follows longest line`() {
        val pane = LogTextPane()
        pane.font = UIUtil.getLabelFont()
        val longest = "x".repeat(200)
        pane.document.insertString(0, "short\n$longest", null)

        val width = pane.preferredSize.width
        val metrics = pane.getFontMetrics(pane.font)

        assertTrue(width >= metrics.stringWidth(longest))
    }
}
