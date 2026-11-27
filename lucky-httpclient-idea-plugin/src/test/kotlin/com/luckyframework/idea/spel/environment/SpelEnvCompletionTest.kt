package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.lookup.LookupElementPresentation

/**
 * `@Env("key")` 的配置键补全。
 *
 * 与原生 `@Value("${...}")` 对齐：只列出键名包含输入的键（不区分大小写），
 * `spring.config.import` 导入配置里的键同样参与匹配；候选展示 `key=value`
 * 与来源文件。
 */
class SpelEnvCompletionTest : SpringEnvironmentFixtureTestCase() {

    /** 不包含输入的键不能出现在候选里（对齐原生 `@Value` 的包含匹配）。 */
    fun testFiltersKeysWithoutInput() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
server:
  port: 9999
lucky:
  service:
    name: demo
spring:
  config:
    import: "classpath:httpclient-configuration.yml"
""".trimIndent()
        )
        myFixture.addFileToProject(
            "src/main/resources/httpclient-configuration.yml",
            """
lucky:
  http-client:
    standard-client-configs:
      read-timeout: 30000
""".trimIndent()
        )
        configureEnv("ser<caret>")

        myFixture.completeBasic()
        val lookup = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertTrue("缺少 server.port: $lookup", lookup.contains("server.port"))
        assertTrue(
            "不含 ser 的导入键不应出现: $lookup",
            lookup.none { it.startsWith("lucky.http-client") }
        )
    }

    /** 键名中间包含输入也匹配：`service` 包含 `ser`，与原生 `@Value` 一致。 */
    fun testMatchesSubstringInsideKey() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            """
server:
  port: 9999
lucky:
  service:
    name: demo
""".trimIndent()
        )
        configureEnv("ser<caret>")

        myFixture.completeBasic()
        val lookup = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertTrue("缺少 server.port: $lookup", lookup.contains("server.port"))
        assertTrue("lucky.service.name 应匹配 ser: $lookup", lookup.contains("lucky.service.name"))
    }

    /** 候选展示 `key=value` 与来源文件，与 `${key}` 补全一致。 */
    fun testLookupShowsValueAndFile() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            "server:\n  port: 9999\n  port-extra: 9000\n"
        )
        configureEnv("server.port<caret>")

        myFixture.completeBasic()
        val element = myFixture.lookupElements?.firstOrNull { it.lookupString == "server.port" }
            ?: error("未提供 server.port 候选")
        val presentation = LookupElementPresentation.renderElement(element)

        assertEquals("server.port", presentation.itemText)
        assertEquals("=9999", presentation.tailText)
        assertEquals("application.yml", presentation.typeText)
        assertNotNull("候选缺少配置文件图标", presentation.typeIcon)
    }

    /** `key:default` 的默认值区域不是键名，不再补全配置键。 */
    fun testNoKeyCompletionInDefaultValue() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            "server:\n  port: 9999\n"
        )
        configureEnv("server.port:<caret>")

        myFixture.completeBasic()
        val lookup = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()

        assertFalse("默认值区域不应补全键: $lookup", lookup.contains("server.port"))
    }

    private fun configureEnv(valueWithCaret: String) {
        myFixture.configureByText(
            "EnvCompletionDemo.java",
            """
class EnvCompletionDemo {
    static void call(@com.luckyframework.httpclient.proxy.spel.Env("$valueWithCaret") Integer port) {
    }
}
"""
        )
    }
}
