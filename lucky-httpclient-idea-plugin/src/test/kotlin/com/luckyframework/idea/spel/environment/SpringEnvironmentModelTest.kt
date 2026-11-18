package com.luckyframework.idea.spel.environment

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 配置值解析：系统属性 > 配置文件 > 占位符默认值，以及嵌套与循环检测。
 */
class SpringEnvironmentModelTest : BasePlatformTestCase() {

    fun testResolvesEffectiveDefinition() {
        val file = configFile("lucky.test.host=example.com\nlucky.test.port=8080\n")
        val model = modelOf(
            definition(file, "lucky.test.host", "example.com"),
            definition(file, "lucky.test.port", "8080")
        )

        assertEquals("example.com", model.resolveValue("lucky.test.host"))
        assertEquals(listOf("lucky.test.host", "lucky.test.port"), model.keys())
    }

    fun testResolvesNestedPlaceholdersInValue() {
        val file = configFile(
            """
            lucky.test.host=example.com
            lucky.test.port=8080
            lucky.test.url=http://${'$'}{lucky.test.host}:${'$'}{lucky.test.port}/api
            """.trimIndent()
        )
        val model = modelOf(
            definition(file, "lucky.test.host", "example.com"),
            definition(file, "lucky.test.port", "8080"),
            definition(file, "lucky.test.url", "http://${'$'}{lucky.test.host}:${'$'}{lucky.test.port}/api")
        )

        assertEquals("http://example.com:8080/api", model.resolveValue("lucky.test.url"))
    }

    fun testPlaceholderFallsBackToDefault() {
        val file = configFile("lucky.test.host=example.com\n")
        val model = modelOf(definition(file, "lucky.test.host", "example.com"))

        val missing = SpringPlaceholderParser.parse("\${lucky.test.missing:localhost}").single()
        assertEquals("localhost", model.resolvePlaceholder(missing))

        val nested = SpringPlaceholderParser.parse("\${lucky.test.missing:\${lucky.test.host}}").single()
        assertEquals("example.com", model.resolvePlaceholder(nested))

        val unresolved = SpringPlaceholderParser.parse("\${lucky.test.missing}").single()
        assertNull(model.resolvePlaceholder(unresolved))
    }

    fun testCircularReferenceReturnsNull() {
        val file = configFile("lucky.test.a=\${lucky.test.b}\nlucky.test.b=\${lucky.test.a}\n")
        val model = modelOf(
            definition(file, "lucky.test.a", "\${lucky.test.b}"),
            definition(file, "lucky.test.b", "\${lucky.test.a}")
        )

        assertNull(model.resolveValue("lucky.test.a"))
        assertNull(model.resolveValue("lucky.test.b"))
    }

    fun testSystemPropertyWinsOverConfiguration() {
        val file = configFile("lucky.test.host=config-host\n")
        val model = modelOf(definition(file, "lucky.test.host", "config-host"))
        System.setProperty("lucky.test.host", "system-host")
        try {
            assertEquals("system-host", model.resolveValue("lucky.test.host"))
            assertEquals("系统属性", model.resolve("lucky.test.host")?.label)
        } finally {
            System.clearProperty("lucky.test.host")
        }
    }

    private fun configFile(text: String): VirtualFile =
        myFixture.addFileToProject("src/main/resources/application.properties", text).virtualFile

    private fun definition(
        file: VirtualFile,
        key: String,
        value: String
    ): SpringPropertyDefinition = SpringPropertyDefinition(
        key = key,
        value = value,
        file = file,
        keyOffset = 0,
        valueOffset = 0,
        profile = null,
        rank = 91
    )

    private fun modelOf(vararg definitions: SpringPropertyDefinition): SpringEnvironmentModel {
        val list = definitions.toList()
        val grouped = list.groupBy { it.key }
        return SpringEnvironmentModel(
            SpringPropertySnapshot(grouped, grouped.mapValues { it.value.first() }, emptyList())
        )
    }
}
