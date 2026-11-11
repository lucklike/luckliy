package com.luckyframework.idea.spel.yaml

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
}
