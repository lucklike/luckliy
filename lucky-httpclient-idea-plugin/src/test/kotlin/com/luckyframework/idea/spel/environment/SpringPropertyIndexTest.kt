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

    /** `spring.config.import` 导入的任意命名 yaml 参与索引，且覆盖导入者的同名键。 */
    fun testImportedYamlIsIndexedAndOverridesImporter() {
        addConfig(
            "application.yml",
            """
            spring:
              config:
                import: "classpath:extra.yml"
            lucky:
              test:
                host: application-host
                port: 80
            """.trimIndent()
        )
        addConfig(
            "extra.yml",
            """
            lucky:
              test:
                host: imported-host
                timeout: 30
            """.trimIndent()
        )

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("imported-host", snapshot.effective["lucky.test.host"]?.value)
        assertEquals("30", snapshot.effective["lucky.test.timeout"]?.value)
        assertEquals("80", snapshot.effective["lucky.test.port"]?.value)
        assertEquals("extra.yml", snapshot.definitionsOf("lucky.test.host").first().fileName)
    }

    /** YAML 列表形式与 `optional:` 前缀；无前缀位置按 classpath 处理。 */
    fun testYamlListImportWithOptionalPrefix() {
        addConfig(
            "application.yml",
            """
            spring:
              config:
                import:
                  - optional:classpath:first.properties
                  - second.yml
            """.trimIndent()
        )
        addConfig("first.properties", "lucky.test.first=1\n")
        addConfig("second.yml", "lucky:\n  test:\n    second: 2\n")

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("1", snapshot.effective["lucky.test.first"]?.value)
        assertEquals("2", snapshot.effective["lucky.test.second"]?.value)
    }

    /** properties 的 `spring.config.import[n]` 列表形式与逗号分隔形式。 */
    fun testPropertiesImportSyntax() {
        addConfig(
            "application.properties",
            "spring.config.import[0]=first.properties\n" +
                "spring.config.import=csv.properties, optional:missing.yml\n"
        )
        addConfig("first.properties", "lucky.test.first=1\n")
        addConfig("csv.properties", "lucky.test.csv=2\n")

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("1", snapshot.effective["lucky.test.first"]?.value)
        assertEquals("2", snapshot.effective["lucky.test.csv"]?.value)
    }

    /** 导入文件可以继续声明 import，深度优先加载，越晚加载的越优先。 */
    fun testImportedFileCanImportAnother() {
        addConfig("application.properties", "spring.config.import=first.properties\n")
        addConfig(
            "first.properties",
            "spring.config.import=second.properties\nlucky.test.order=first\n"
        )
        addConfig("second.properties", "lucky.test.order=second\nlucky.test.deep=deep\n")

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("second", snapshot.effective["lucky.test.order"]?.value)
        assertEquals("deep", snapshot.effective["lucky.test.deep"]?.value)
    }

    /** 互相导入不会死循环，定义只出现一次。 */
    fun testImportCycleIsSafe() {
        addConfig("application.properties", "spring.config.import=a.properties\n")
        addConfig("a.properties", "spring.config.import=b.properties\nlucky.test.a=1\n")
        addConfig("b.properties", "spring.config.import=a.properties\nlucky.test.b=2\n")

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("1", snapshot.effective["lucky.test.a"]?.value)
        assertEquals("2", snapshot.effective["lucky.test.b"]?.value)
        assertEquals(1, snapshot.definitionsOf("lucky.test.a").size)
    }

    /** 未激活 profile 的文件不会被加载，它声明的 import 也不生效。 */
    fun testInactiveProfileImportIsNotLoaded() {
        addConfig("application.properties", "spring.profiles.active=dev\n")
        addConfig("application-prod.properties", "spring.config.import=prod-extra.properties\n")
        addConfig("prod-extra.properties", "lucky.test.host=prod-host\n")

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertNull(snapshot.effective["lucky.test.host"])
        assertTrue(snapshot.definitionsOf("lucky.test.host").isEmpty())
    }

    /** profile 文件声明的 import 生效，且与 profile 阶段一致。 */
    fun testProfileFileImportIsLoaded() {
        addConfig("application.properties", "spring.profiles.active=dev\n")
        addConfig("application-dev.properties", "spring.config.import=dev-extra.properties\n")
        addConfig("dev-extra.properties", "lucky.test.host=dev-host\n")

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("dev-host", snapshot.effective["lucky.test.host"]?.value)
        assertNotNull(snapshot.effective["lucky.test.host"]?.rank)
    }

    /** `file:` 相对位置基于项目/模块内容根解析。 */
    fun testFileImportRelativeToContentRoot() {
        addConfig("application.properties", "spring.config.import=optional:file:./config/extra.properties\n")
        myFixture.addFileToProject("config/extra.properties", "lucky.test.host=file-host\n")

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("file-host", snapshot.effective["lucky.test.host"]?.value)
    }

    /** 找不到的导入位置与特殊 scheme 静默跳过。 */
    fun testMissingImportAndNonFileSchemeAreIgnored() {
        addConfig(
            "application.properties",
            "spring.config.import=missing.yml,configtree:/etc/config/,https://example.com/a.yml\n" +
                "lucky.test.host=ok\n"
        )

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("ok", snapshot.effective["lucky.test.host"]?.value)
    }

    private fun addConfig(name: String, text: String) {
        myFixture.addFileToProject("src/main/resources/$name", text)
    }
}
