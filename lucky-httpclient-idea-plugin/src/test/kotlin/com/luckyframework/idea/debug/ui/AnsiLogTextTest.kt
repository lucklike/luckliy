package com.luckyframework.idea.debug.ui

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class AnsiLogTextTest : BasePlatformTestCase() {

    fun `test strips sgr color and reset`() {
        assertEquals("hello world", AnsiLogText.plainText("\u001B[1;35mhello\u001B[0m world"))
    }

    fun `test strips extended color sequences`() {
        assertEquals("red", AnsiLogText.plainText("\u001B[38;5;196mred"))
        assertEquals("text", AnsiLogText.plainText("\u001B[38;2;1;2;3mtext"))
    }

    fun `test strips non sgr control sequences`() {
        assertEquals("plain", AnsiLogText.plainText("\u001B[2Kplain"))
    }

    fun `test keeps unterminated sequence as plain text`() {
        assertEquals("\u001B[1;35", AnsiLogText.plainText("\u001B[1;35"))
    }

    fun `test keeps text without escapes`() {
        assertEquals("plain text", AnsiLogText.plainText("plain text"))
    }
}
