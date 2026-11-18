package com.luckyframework.idea.std

import com.luckyframework.idea.std.yaml.StdConfigLocator

/**
 * YAML 配置路径解析：标准路径、宽松绑定、引号、多文档与列表排除。
 */
class StdConfigLocatorTest : StdFixtureTestCase() {

    fun testStandardPath() {
        configureYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    url: https://gitee.com
            """
        )

        val nodes = StdConfigLocator.locate(myFixture.file)

        assertEquals(1, nodes.size)
        assertEquals("GiteeApi", nodes.single().configId)
    }

    fun testLooseBindingForFixedSegments() {
        configureYaml(
            """
            LUCKY:
              httpClient:
                standard_client_configs:
                  GiteeApi:
                    url: https://gitee.com
            """
        )

        assertEquals("GiteeApi", StdConfigLocator.locate(myFixture.file).single().configId)
    }

    fun testQuotedKeys() {
        configureYaml(
            """
            "lucky":
              "http-client":
                "standard-client-configs":
                  "GiteeApi":
                    url: https://gitee.com
            """
        )

        assertEquals("GiteeApi", StdConfigLocator.locate(myFixture.file).single().configId)
    }

    fun testMultipleDocuments() {
        configureYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  ApiA:
                    url: https://a.example.com
            ---
            lucky:
              http-client:
                standard-client-configs:
                  ApiB:
                    url: https://b.example.com
            """
        )

        val configIds = StdConfigLocator.locate(myFixture.file).map { it.configId }

        assertEquals(listOf("ApiA", "ApiB"), configIds)
    }

    fun testBaseUrlIsExtracted() {
        configureYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    url: "https://gitee.com/api"
                    method-configs:
                      getUserInfo:
                        path: /user
            """
        )

        val node = StdConfigLocator.locate(myFixture.file).single()

        assertEquals("https://gitee.com/api", node.baseUrl)
    }

    fun testBaseUrlWithoutUrlAttributeIsNull() {
        configureYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    service: GiteeService
            """
        )

        assertNull(StdConfigLocator.locate(myFixture.file).single().baseUrl)
    }

    fun testMethodConfigs() {
        configureYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    method-configs:
                      getUserInfo:
                        path: /user
                      "list-repos":
                        path: /repos
            """
        )

        val node = StdConfigLocator.locate(myFixture.file).single()

        assertEquals(setOf("getUserInfo", "list-repos"), node.methodConfigs.keys)
        assertEquals("/user", node.methodPaths["getUserInfo"])
        assertEquals("/repos", node.methodPaths["list-repos"])
    }

    fun testNonStandardPathIsIgnored() {
        configureYaml(
            """
            other:
              standard-client-configs:
                GiteeApi:
                  url: https://gitee.com
            """
        )

        assertTrue(StdConfigLocator.locate(myFixture.file).isEmpty())
    }

    fun testSequenceItemIsIgnored() {
        configureYaml(
            """
            list:
              - lucky:
                  http-client:
                    standard-client-configs:
                      GiteeApi:
                        url: https://gitee.com
            """
        )

        assertTrue(StdConfigLocator.locate(myFixture.file).isEmpty())
    }

    fun testEmptyConfigNodeIsCounted() {
        configureYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
            """
        )

        assertEquals("GiteeApi", StdConfigLocator.locate(myFixture.file).single().configId)
    }

    private fun configureYaml(text: String) {
        myFixture.configureByText("config.yml", text.trimIndent())
    }
}
