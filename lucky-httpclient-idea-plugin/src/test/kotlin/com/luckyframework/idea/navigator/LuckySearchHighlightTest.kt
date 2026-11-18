package com.luckyframework.idea.navigator

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 侧边栏搜索高亮：命中区间的切分规则。
 */
class LuckySearchHighlightTest : BasePlatformTestCase() {

    fun testMatchesIgnoreCase() {
        assertEquals(listOf(3 until 7), LuckySearchHighlight.rangesOf("StdHttpClient", "http"))
    }

    fun testMatchesAllOccurrences() {
        assertEquals(
            listOf(0 until 2, 6 until 8),
            LuckySearchHighlight.rangesOf("ab-cd-ab", "AB")
        )
    }

    fun testBlankQueryMatchesNothing() {
        assertTrue(LuckySearchHighlight.rangesOf("DemoClient", "  ").isEmpty())
    }

    fun testNoMatchReturnsEmpty() {
        assertTrue(LuckySearchHighlight.rangesOf("DemoClient", "xyz").isEmpty())
    }

    /** 每个区间的长度等于搜索词长度，渲染端据此直接截取原文。 */
    fun testRangesPointAtQueryLength() {
        val ranges = LuckySearchHighlight.rangesOf("getUserById", "user")
        assertEquals(listOf(3 until 7), ranges)
        ranges.forEach { assertEquals(4, it.last - it.first + 1) }
    }
}
