package com.luckyframework.idea.debug.ui

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.Color

class AnsiLogTextTest : BasePlatformTestCase() {

    fun `test parses sgr color and reset`() {
        val segments = AnsiLogText.parse("\u001B[1;35mhello\u001B[0m world")

        assertEquals(2, segments.size)
        assertEquals("hello", segments[0].text)
        assertTrue(segments[0].style.bold)
        assertNotNull(segments[0].style.foreground)
        assertEquals(" world", segments[1].text)
        assertEquals(AnsiLogText.Style(), segments[1].style)
    }

    fun `test parses 256 color palette`() {
        val segments = AnsiLogText.parse("\u001B[38;5;196mred")

        assertEquals(1, segments.size)
        assertEquals(Color(255, 0, 0), segments.single().style.foreground)
    }

    fun `test parses true color`() {
        val segments = AnsiLogText.parse("\u001B[38;2;1;2;3mtext")

        assertEquals(1, segments.size)
        assertEquals(Color(1, 2, 3), segments.single().style.foreground)
    }

    fun `test drops non sgr control sequences`() {
        val segments = AnsiLogText.parse("\u001B[2Kplain")

        assertEquals(1, segments.size)
        assertEquals("plain", segments.single().text)
        assertEquals(AnsiLogText.Style(), segments.single().style)
    }

    fun `test keeps unterminated sequence as plain text`() {
        val segments = AnsiLogText.parse("\u001B[1;35")

        assertEquals(1, segments.size)
        assertEquals("\u001B[1;35", segments.single().text)
    }

    fun `test applies underline and background`() {
        val segments = AnsiLogText.parse("\u001B[4;41mhighlight")

        assertEquals(1, segments.size)
        assertTrue(segments.single().style.underline)
        assertNotNull(segments.single().style.background)
    }
}
