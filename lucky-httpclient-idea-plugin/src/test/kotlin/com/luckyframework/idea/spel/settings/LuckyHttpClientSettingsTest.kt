package com.luckyframework.idea.spel.settings

import com.luckyframework.idea.spel.LuckyFixtureTestCase

class LuckyHttpClientSettingsTest : LuckyFixtureTestCase() {

    override fun tearDown() {
        try {
            LuckyHttpClientSettings.getInstance().isEnabled = true
        } finally {
            super.tearDown()
        }
    }

    fun testEnabledByDefault() {
        assertTrue(LuckyHttpClientSettings.getInstance().isEnabled)
    }

    fun testDisableStopsAnnotatorWithoutTouchingSources() {
        myFixture.configureByText("Demo.java", "class Demo { String s = \"a#{#x}b\"; }")
        val open = myFixture.file.text.indexOf("#{")
        val before = myFixture.file.text

        val settings = LuckyHttpClientSettings.getInstance()

        assertTrue(myFixture.doHighlighting().any { it.startOffset == open && it.endOffset == open + 2 })

        settings.isEnabled = false
        com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project).restart()

        val infos = myFixture.doHighlighting()
        assertFalse(infos.any { it.startOffset == open && it.endOffset == open + 2 })
        assertEquals(before, myFixture.file.text)

        settings.isEnabled = true
        com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project).restart()
        assertTrue(myFixture.doHighlighting().any { it.startOffset == open && it.endOffset == open + 2 })
    }
}
