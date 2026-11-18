package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.luckyframework.idea.spel.highlight.SpelTextAttributes

/**
 * YAML 值中 `${key}` 的着色（YAML 插件存在时加载）。
 */
class SpringPlaceholderYamlAnnotatorTest : SpringEnvironmentFixtureTestCase() {

    fun testHighlightsPlaceholderInYamlValue() {
        myFixture.configureByText(
            "config.yml",
            "url: \"http://\${api.host:localhost}/x\"\n"
        )
        val text = myFixture.file.text
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("api.host"), SpelTextAttributes.SPRING_PLACEHOLDER)
        assertKeyAt(infos, text.indexOf("localhost"), SpelTextAttributes.SPRING_PLACEHOLDER_DEFAULT)
    }

    /** YAML 键本身不是值位置，不参与占位符着色。 */
    fun testKeyIsNotHighlighted() {
        myFixture.configureByText(
            "config.yml",
            "\${api.host}: value\n"
        )

        val infos = myFixture.doHighlighting()
            .filter { it.forcedTextAttributesKey?.externalName?.startsWith("LUCKY_SPRING_") == true }
        assertTrue("YAML 键不应出现占位符着色: ${infos.size}", infos.isEmpty())
    }

    /** YAML 值中的未定义键同样给出警告。 */
    fun testUndefinedKeyInYamlValueIsWarning() {
        myFixture.configureByText(
            "application.yml",
            "url: \"http://\${api.missing}/x\"\n"
        )

        val warnings = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.WARNING }
        assertTrue(
            "未找到未定义键警告: ${warnings.map { it.description }}",
            warnings.any { it.description?.contains("未定义的配置键") == true }
        )
    }

    private fun assertKeyAt(
        infos: List<HighlightInfo>,
        offset: Int,
        expected: TextAttributesKey
    ) {
        assertTrue(
            "offset $offset 未找到 $expected 高亮: ${infos.filter { it.startOffset == offset }.map { it.forcedTextAttributesKey }}",
            infos.any { it.startOffset == offset && it.forcedTextAttributesKey == expected }
        )
    }
}
