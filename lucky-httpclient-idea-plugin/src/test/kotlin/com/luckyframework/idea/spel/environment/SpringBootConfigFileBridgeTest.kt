package com.luckyframework.idea.spel.environment

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Ultimate 桥接的可选性：社区版测试环境没有 Spring Boot 插件，
 * 桥接必须安全降级，不能影响内置解析。
 */
class SpringBootConfigFileBridgeTest : BasePlatformTestCase() {

    fun testBridgeIsUnavailableWithoutSpringBootPlugin() {
        assertFalse(SpringBootConfigFileBridge.isAvailable)

        val file = myFixture.addFileToProject(
            "application.yml",
            "server:\n  port: 8080\n"
        ).virtualFile

        assertNull(SpringBootConfigFileBridge.parse(project, file))
    }

    /** 桥接不可用时，索引仍由内置解析器完整产出键。 */
    fun testIndexStillWorksWhenBridgeIsUnavailable() {
        myFixture.addFileToProject("application.yml", "server:\n  port: 8080\n")

        val snapshot = SpringPropertyIndex().snapshot(myFixture.module)

        assertEquals("8080", snapshot.effective["server.port"]?.value)
    }

    /** 社区版没有 Spring Boot 元数据：候选继续使用内置样式，不能因反射失败。 */
    fun testMetadataBridgeIsUnavailableWithoutSpringBootPlugin() {
        assertFalse(SpringBootConfigKeyMetadataBridge.isAvailable)
        assertNull(SpringBootConfigKeyMetadataBridge.metadataFor(myFixture.module, "server.port"))

        myFixture.addFileToProject("application.yml", "server:\n  port: 8080\n")
        val model = SpringEnvironmentModel(
            SpringPropertyIndex().snapshot(myFixture.module),
            myFixture.module
        )

        assertNull(model.metadataOf("server.port"))
    }
}
