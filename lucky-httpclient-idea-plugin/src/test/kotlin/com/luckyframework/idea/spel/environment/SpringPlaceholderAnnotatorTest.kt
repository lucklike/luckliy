package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.highlight.SpelTextAttributes

/**
 * Java 注解属性中 `${key}` 的着色与语法提示。
 */
class SpringPlaceholderAnnotatorTest : SpringEnvironmentFixtureTestCase() {

    fun testHighlightsKeyDefaultAndDelimiters() {
        val text = configureHttpRequest("http://\${api.host:localhost}/x")
        val infos = myFixture.doHighlighting()

        assertKeyAt(infos, text.indexOf("api.host"), SpelTextAttributes.SPRING_PLACEHOLDER)
        assertKeyAt(infos, text.indexOf("localhost"), SpelTextAttributes.SPRING_PLACEHOLDER_DEFAULT)
        assertKeyAt(infos, text.indexOf("\${"), SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER)
        assertKeyAt(infos, text.indexOf(":localhost"), SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER)
        assertKeyAt(infos, text.indexOf("}"), SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER)
    }

    /** 环境变量占位符不能被 SpEL 着色接管，两套颜色必须互斥。 */
    fun testSpelColorsDoNotCoverPlaceholder() {
        val text = configureHttpRequest("http://\${api.host}/x")
        val keyOffset = text.indexOf("api.host")
        val infos = myFixture.doHighlighting()
            .filter { it.startOffset == keyOffset }

        assertTrue(
            "环境变量键被 SpEL 着色覆盖: ${infos.map { it.forcedTextAttributesKey }}",
            infos.none { it.forcedTextAttributesKey?.externalName?.startsWith("LUCKY_SPEL_") == true }
        )
    }

    fun testUnclosedPlaceholderIsError() {
        configureHttpRequest("http://\${api.host")

        assertErrorContains("未闭合")
    }

    fun testEmptyKeyIsError() {
        configureHttpRequest("http://\${}")

        assertErrorContains("不能为空")
    }

    /** 不存在的键且没配默认值时，运行期无法解析，给出警告。 */
    fun testUndefinedKeyWithoutDefaultIsWarning() {
        configureHttpRequest("http://\${api.missing}/x")

        assertWarningContains("未定义的配置键")
    }

    /** 有默认值时键缺失是正常用法，不应报警。 */
    fun testUndefinedKeyWithDefaultIsNotWarned() {
        configureHttpRequest("http://\${api.missing:localhost}/x")

        assertNoWarningContains("未定义的配置键")
    }

    fun testDefinedKeyIsNotWarned() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\n"
        )
        configureHttpRequest("http://\${api.host}/x")

        assertNoWarningContains("未定义的配置键")
    }

    /** 父级配置节点本身不是完整键，单独引用时 Spring 解析不到值。 */
    fun testParentKeyIsIncompleteWarning() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "spring.datasource.url=jdbc:mysql://localhost/db\n"
        )
        configureHttpRequest("http://\${spring.datasource}/x")

        assertWarningContains("不是完整的配置键")
    }

    /** 未激活 profile 文件里存在的键在项目中真实存在，不按未定义处理。 */
    fun testInactiveProfileKeyIsNotWarned() {
        myFixture.addFileToProject(
            "src/main/resources/application-prod.properties",
            "api.host=prod-host\n"
        )
        configureHttpRequest("http://\${api.host}/x")

        assertNoWarningContains("未定义的配置键")
    }

    /** 未标注 `@SpelAttribute` 的普通字符串不经过 SpringSpELConvert，保持原文。 */
    fun testPlainLiteralIsNotHighlighted() {
        myFixture.configureByText(
            "Demo.java",
            """
class Demo {
    String url = "http://${'$'}{api.host}";
}
"""
        )

        val infos = myFixture.doHighlighting()
            .filter { it.forcedTextAttributesKey?.externalName?.startsWith("LUCKY_SPRING_") == true }
        assertTrue("普通属性不应出现占位符着色: ${infos.size}", infos.isEmpty())
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

    private fun assertErrorContains(message: String) {
        val errors = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.ERROR }
        assertTrue(
            "未找到包含「$message」的错误: ${errors.map { it.description }}",
            errors.any { it.description?.contains(message) == true }
        )
    }

    private fun assertWarningContains(message: String) {
        val warnings = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.WARNING }
        assertTrue(
            "未找到包含「$message」的警告: ${warnings.map { it.description }}",
            warnings.any { it.description?.contains(message) == true }
        )
    }

    private fun assertNoWarningContains(message: String) {
        val warnings = myFixture.doHighlighting().filter { it.severity == HighlightSeverity.WARNING }
        assertFalse(
            "不应出现包含「$message」的警告: ${warnings.map { it.description }}",
            warnings.any { it.description?.contains(message) == true }
        )
    }
}

/** 没有 starter 依赖时，即使出现 `${...}` 也不应激活任何能力。 */
class SpringPlaceholderInactiveTest : LuckyFixtureTestCase() {

    fun testNoHighlightWithoutStarter() {
        myFixture.configureByText(
            "Demo.java",
            """
class Demo {
    @com.luckyframework.httpclient.proxy.annotations.HttpRequest(url = "http://${'$'}{api.host}")
    void call() {
    }
}
"""
        )

        val infos = myFixture.doHighlighting()
            .filter { it.forcedTextAttributesKey?.externalName?.startsWith("LUCKY_SPRING_") == true }
        assertTrue("缺少 starter 时不应激活: ${infos.size}", infos.isEmpty())
    }
}
