package com.luckyframework.idea.spel.environment

import com.intellij.psi.ElementManipulators
import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 自动收起只针对本插件生成的折叠区域（`${...}` 与 `@Env` 注入名），
 * 不能误伤 import、Javadoc 等平台折叠。
 */
class FoldingAutoCollapseTest : SymbolFixtureTestCase() {

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

    /** `@Env("server.port")` 的注入名区域：重开文件后需要被自动收起。 */
    fun testMatchesEnvInjectionRegion() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.Env;

class Api {
    static void call(@Env("server.port") Integer port) {
    }
}
"""
        )
        val literal = literalOf("server.port")
        val injection = SpelAnnotationResolver().resolveInjection(literal)
        val range = ElementManipulators.getValueTextRange(literal).shiftRight(literal.textRange.startOffset)

        assertTrue(FoldingAutoCollapse.isEnvInjectionFoldRegion(literal, injection, range))
        assertFalse(
            "只覆盖部分名字不应匹配",
            FoldingAutoCollapse.isEnvInjectionFoldRegion(
                literal, injection, TextRange(range.startOffset + 1, range.endOffset)
            )
        )
    }

    /** 非 ENV 注入名（`@Rar`）不是 env 折叠区域。 */
    fun testRejectsNonEnvInjectionRegion() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.spel.Rar;

class Api {
    static void call(@Rar("a") Integer a) {
    }
}
"""
        )
        val literal = literalOf("a")
        val injection = SpelAnnotationResolver().resolveInjection(literal)
        val range = ElementManipulators.getValueTextRange(literal).shiftRight(literal.textRange.startOffset)

        assertFalse(FoldingAutoCollapse.isEnvInjectionFoldRegion(literal, injection, range))
    }
}
