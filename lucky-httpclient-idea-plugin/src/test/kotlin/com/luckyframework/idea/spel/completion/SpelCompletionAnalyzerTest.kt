package com.luckyframework.idea.spel.completion

import junit.framework.TestCase

/**
 * 补全位置判定：关闭候选列表后继续输入（如 `#{md}` 中补 `5`）仍要识别为 Root 位置，
 * 而表达式闭合之后不再触发。
 */
class SpelCompletionAnalyzerTest : TestCase() {

    private val analyzer = SpelCompletionAnalyzer()

    fun testRootWordGrowsAfterLookupClosed() {
        // 光标在 `5` 之后、`}` 之前：`#{md}` 补成 `#{md5}` 后仍应给出 Root 候选
        val site = analyzer.analyze("\"#{md5}\"", 6)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.ROOT, site!!.mode)
        assertEquals("md5", site.word)
    }

    fun testRootWordBeforeClosingBrace() {
        // 光标在 `d` 之后、`}` 之前
        val site = analyzer.analyze("\"#{md}\"", 5)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.ROOT, site!!.mode)
        assertEquals("md", site.word)
    }

    fun testClosedExpressionHasNoCompletion() {
        // 光标在 `}` 之后：表达式已闭合，不再补全
        assertNull(analyzer.analyze("\"#{md}\"", 6))
    }

    fun testMemberAfterDot() {
        val site = analyzer.analyze("\"#{file.}\"", 8)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.MEMBER, site!!.mode)
        assertEquals("file", site.qualifier)
    }

    fun testVariableAfterHash() {
        val site = analyzer.analyze("\"#{#\"", 4)
        assertNotNull(site)
        assertEquals(SpelCompletionMode.VARIABLE, site!!.mode)
    }
}
