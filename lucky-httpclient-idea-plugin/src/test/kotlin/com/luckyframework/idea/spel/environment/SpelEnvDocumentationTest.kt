package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.documentation.DocumentationManager
import com.luckyframework.idea.spel.documentation.SpelDocumentationProvider
import com.luckyframework.idea.spel.reference.SpelDeclaration

/**
 * `@Env` 注入名的悬停文档。
 *
 * 环境配置键不是 SpEL 符号：键未命中配置时不能提示「未解析的 SpEL 符号」，
 * 带默认值时还应说明运行期会回退默认值。
 */
class SpelEnvDocumentationTest : SpringEnvironmentFixtureTestCase() {

    fun testUndefinedKeyWithDefaultShowsDefaultValue() {
        configureEnv("server.missi<caret>ng:45")

        val text = documentationAtCaret()

        assertTrue("应展示配置键名: $text", text.contains("server.missing"))
        assertTrue("应展示默认值: $text", text.contains("45"))
        assertFalse("不应提示未解析的 SpEL 符号: $text", text.contains("未解析的 SpEL 符号"))
    }

    fun testUndefinedKeyWithoutDefaultExplainsMissing() {
        configureEnv("server.missi<caret>ng")

        val text = documentationAtCaret()

        assertTrue("应说明未提供默认值: $text", text.contains("未提供默认值"))
        assertFalse("不应提示未解析的 SpEL 符号: $text", text.contains("未解析的 SpEL 符号"))
    }

    fun testDefinedKeyIsNotReportedAsUnresolved() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "server.port=8888\n"
        )
        configureEnv("server.p<caret>ort")

        val text = documentationAtCaret()

        assertTrue("应展示配置键名: $text", text.contains("server.port"))
        assertFalse("不应提示未解析的 SpEL 符号: $text", text.contains("未解析的 SpEL 符号"))
    }

    private fun configureEnv(name: String) {
        myFixture.configureByText(
            "EnvDemo.java",
            """
class EnvDemo {
    static void call(@com.luckyframework.httpclient.proxy.spel.Env("$name") Integer port) {
    }
}
"""
        )
    }

    private fun documentationAtCaret(): String {
        myFixture.doHighlighting()
        val provider = SpelDocumentationProvider()
        val element = provider.getCustomDocumentationElement(
            myFixture.editor,
            myFixture.file,
            null,
            myFixture.caretOffset
        )
        assertNotNull("未找到 SpEL 文档元素", element)
        val documentation = if (element is SpelDeclaration) {
            provider.generateDoc(element, null)
        } else {
            DocumentationManager.getProviderFromElement(element!!)?.generateDoc(element, null)
        }
        assertNotNull("文档为空", documentation)
        return documentation!!
    }
}
