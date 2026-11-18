package com.luckyframework.idea.spel.environment

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 自动收起只针对覆盖完整 `${...}` 的折叠区域，不能误伤 import、Javadoc 等平台折叠。
 */
class FoldingAutoCollapseTest : BasePlatformTestCase() {

    fun testMatchesPlaceholderRegions() {
        assertTrue(FoldingAutoCollapse.isPlaceholderFoldRegion("\${server.port}", "8888"))
        assertTrue(FoldingAutoCollapse.isPlaceholderFoldRegion("\${server.port:8080}", "8080"))
        assertTrue(FoldingAutoCollapse.isPlaceholderFoldRegion("\${a:\${b}}", "nested"))
    }

    fun testRejectsOtherRegions() {
        assertFalse(FoldingAutoCollapse.isPlaceholderFoldRegion("import ...", "..."))
        assertFalse(FoldingAutoCollapse.isPlaceholderFoldRegion("/** 通用翻译 */", "/**...*/"))
        assertFalse(FoldingAutoCollapse.isPlaceholderFoldRegion("\${server.port}", null))
        assertFalse(FoldingAutoCollapse.isPlaceholderFoldRegion("\${server.port}", ""))
        assertFalse(FoldingAutoCollapse.isPlaceholderFoldRegion("{server.port}", "8888"))
    }
}
