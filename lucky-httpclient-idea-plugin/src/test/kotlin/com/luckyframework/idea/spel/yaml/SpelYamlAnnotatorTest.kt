package com.luckyframework.idea.spel.yaml

import com.intellij.lang.annotation.HighlightSeverity
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.highlight.SpelTextAttributes

class SpelYamlAnnotatorTest : LuckyFixtureTestCase() {

    fun testHighlightsVariableInQuotedValue() {
        myFixture.configureByText("config.yml", "url: \"http://#{#host}/api\"\n")

        val infos = myFixture.doHighlighting()
        assertTrue(
            "未找到变量高亮: ${infos.map { it.forcedTextAttributesKey?.externalName }}",
            infos.any { it.forcedTextAttributesKey == SpelTextAttributes.VARIABLE }
        )
    }

    fun testHighlightsUnclosedExpressionAsError() {
        myFixture.configureByText("config.yml", "url: \"http://#{#host\"\n")

        val errors = myFixture.doHighlighting().filter { it.description?.contains("未闭合") == true }
        assertTrue(errors.isNotEmpty())
    }

    fun testCommentIsNotHighlighted() {
        myFixture.configureByText("config.yml", "# 示例：#{#host}\n")

        val infos = myFixture.doHighlighting()
            .filter { it.forcedTextAttributesKey?.externalName?.startsWith("LUCKY_SPEL_") == true }
        assertTrue(infos.isEmpty())
    }

    /**
     * yml 中 SpEL 写在双引号标量内，字面量里的 `#{` 只是普通文本：
     * `#{'#{Hello World}'}` 只求值一次，内层不能当作子片段报语法错误。
     */
    fun testLiteralInsideStringIsNotError() {
        myFixture.configureByText("config.yml", "url: \"#{'#{Hello World}'}\"\n")

        val errors = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.ERROR }
        assertTrue(
            "不应报语法错误: ${errors.map { it.description }}",
            errors.isEmpty()
        )
    }

    /** 双引号标量中的转义双引号字符串内出现 `#{` 同样不产生子片段。 */
    fun testDoubleQuotedLiteralInsideStringIsNotError() {
        myFixture.configureByText(
            "config.yml",
            "url: \"#{T(String).format(\\\"#{x}\\\")}\"\n"
        )

        val errors = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.ERROR }
        assertTrue(
            "不应报语法错误: ${errors.map { it.description }}",
            errors.isEmpty()
        )
    }
}
