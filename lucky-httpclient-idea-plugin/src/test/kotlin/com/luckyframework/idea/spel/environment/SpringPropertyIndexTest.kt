package com.luckyframework.idea.spel.environment

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Spring 配置文件索引：profile 优先级、properties/yaml 扁平化与未激活文件保留。
 */
class SpringPropertyIndexTest : BasePlatformTestCase() {

    fun testActiveProfileOverridesDefaultFile() {
        addConfig(
            "application.properties",
            "lucky.test.host=default-host\nspring.profiles.active=dev\n"
        )
        addConfig("application-dev.properties", "lucky.test.host=dev-host\n")
        addConfig(
            "application.yml",
            """
            lucky:
              test:
                port: 9090
            """.trimIndent()
        )

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("dev-host", snapshot.effective["lucky.test.host"]?.value)
        assertEquals("9090", snapshot.effective["lucky.test.port"]?.value)
        assertEquals(listOf("dev"), snapshot.activeProfiles)
        // 默认文件与 profile 文件的定义都保留，供跳转选择
        assertEquals(2, snapshot.definitionsOf("lucky.test.host").size)
        assertEquals("application-dev.properties", snapshot.definitionsOf("lucky.test.host")[0].fileName)
    }

    fun testInactiveProfileDoesNotAffectEffectiveValue() {
        addConfig("application.properties", "lucky.test.host=default-host\n")
        addConfig("application-prod.properties", "lucky.test.host=prod-host\n")

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("default-host", snapshot.effective["lucky.test.host"]?.value)
        assertNotNull(snapshot.effective["lucky.test.host"]?.rank)
        assertEquals(2, snapshot.definitionsOf("lucky.test.host").size)
        assertTrue(snapshot.definitionsOf("lucky.test.host").any { it.rank == null })
    }

    fun testPropertiesSeparatorsAndYamlComments() {
        addConfig(
            "application.properties",
            """
            lucky.test.a=1
            lucky.test.b: 2
            lucky.test.c 3
            """.trimIndent()
        )
        addConfig(
            "application.yml",
            """
            lucky:
              test:
                name: "my app" # 行尾注释
                empty:
            """.trimIndent()
        )

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("1", snapshot.effective["lucky.test.a"]?.value)
        assertEquals("2", snapshot.effective["lucky.test.b"]?.value)
        assertEquals("3", snapshot.effective["lucky.test.c"]?.value)
        assertEquals("my app", snapshot.effective["lucky.test.name"]?.value)
        assertFalse(snapshot.keys().contains("lucky.test.empty"))
    }

    fun testBootstrapAndApplicationPriority() {
        addConfig("bootstrap.properties", "lucky.test.host=bootstrap-host\n")
        addConfig("application.properties", "lucky.test.host=application-host\n")

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("application-host", snapshot.effective["lucky.test.host"]?.value)
        assertEquals(2, snapshot.definitionsOf("lucky.test.host").size)
    }

    private fun addConfig(name: String, text: String) {
        myFixture.addFileToProject("src/main/resources/$name", text)
    }
}
