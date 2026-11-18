package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.application.ApplicationManager

/**
 * `${key}` 的解析值预览：默认折叠，展开后回到原表达式。
 */
class SpringPlaceholderFoldingTest : SpringEnvironmentFixtureTestCase() {

    fun testFoldsResolvedValue() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\n"
        )
        configureHttpRequest("http://\${api.host}/x")

        val descriptors = SpringPlaceholderFoldingBuilder()
            .buildFoldRegions(myFixture.file.node, myFixture.editor.document)

        assertEquals(1, descriptors.size)
        assertEquals("example.com", descriptors[0].placeholderText)
        assertTrue(SpringPlaceholderFoldingBuilder().isCollapsedByDefault(myFixture.file.node))
    }

    /** Spring `@Value` 的 `${key}` 同样默认折叠为配置值。 */
    fun testFoldsSpringValue() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "server.port=8888\n"
        )
        configureValue("\${server.port}")

        val descriptors = SpringPlaceholderFoldingBuilder()
            .buildFoldRegions(myFixture.file.node, myFixture.editor.document)

        assertEquals(1, descriptors.size)
        assertEquals("8888", descriptors[0].placeholderText)
    }

    /** `@StdHttpClient#path` 经 `@AliasFor` 映射到 `@HttpClient#path`，同样折叠。 */
    fun testFoldsStdHttpClientPath() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "server.port=8888\n"
        )
        myFixture.configureByText(
            "DemoApi.java",
            """
@io.github.lucklike.httpclient.std.StdHttpClient(path = "${'$'}{server.port}")
interface DemoApi {
}
"""
        )

        val descriptors = SpringPlaceholderFoldingBuilder()
            .buildFoldRegions(myFixture.file.node, myFixture.editor.document)

        assertEquals(1, descriptors.size)
        assertEquals("8888", descriptors[0].placeholderText)
    }

    /** 未导入的 `@Value` 按简单名识别，与运行期注解完全匹配。 */
    fun testFoldsUnresolvedValueAnnotation() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "server.port=8888\n"
        )
        myFixture.configureByText(
            "DemoValue.java",
            """
class DemoValue {
    @Value("${'$'}{server.port}")
    private String port;
}
"""
        )

        val descriptors = SpringPlaceholderFoldingBuilder()
            .buildFoldRegions(myFixture.file.node, myFixture.editor.document)

        assertEquals(1, descriptors.size)
        assertEquals("8888", descriptors[0].placeholderText)
    }

    /** 普通字符串里的 `${}` 不是配置引用，不折叠。 */
    fun testPlainStringIsNotFolded() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "server.port=8888\n"
        )
        myFixture.configureByText(
            "Plain.java",
            """
class Plain {
    private String text = "port=${'$'}{server.port}";
}
"""
        )

        val descriptors = SpringPlaceholderFoldingBuilder()
            .buildFoldRegions(myFixture.file.node, myFixture.editor.document)

        assertTrue(descriptors.isEmpty())
    }

    /** 用户项目的配置形态：`application.yml` 中的嵌套键。 */
    fun testFoldsValueFromNestedYaml() {
        myFixture.addFileToProject(
            "src/main/resources/application.yml",
            "server:\n  port: 8888\n"
        )
        configureValue("\${server.port}")

        val descriptors = SpringPlaceholderFoldingBuilder()
            .buildFoldRegions(myFixture.file.node, myFixture.editor.document)

        assertEquals(1, descriptors.size)
        assertEquals("8888", descriptors[0].placeholderText)
    }

    /** 平台接受折叠描述符：编辑器折叠模型中出现 8888 占位文本。 */
    fun testFoldingRegionIsAppliedByEditor() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "server.port=8888\n"
        )
        myFixture.configureByText(
            "DemoApi.java",
            """
@io.github.lucklike.httpclient.std.StdHttpClient(path = "${'$'}{server.port}")
interface DemoApi {
}
"""
        )
        val editor = myFixture.editor
        ApplicationManager.getApplication().invokeAndWait {
            CodeFoldingManager.getInstance(project).updateFoldRegions(editor)
        }
        val placeholders = editor.foldingModel.allFoldRegions.map { it.placeholderText }
        assertTrue("regions=$placeholders", placeholders.contains("8888"))
    }

    /** 未解析的键不折叠，避免把不完整的结果当成确定值。 */
    fun testUnresolvedPlaceholderIsNotFolded() {
        configureHttpRequest("http://\${api.missing}/x")

        val descriptors = SpringPlaceholderFoldingBuilder()
            .buildFoldRegions(myFixture.file.node, myFixture.editor.document)

        assertTrue(descriptors.isEmpty())
    }

    /** 设置页关闭预览后不再产生折叠区域。 */
    fun testPreviewCanBeDisabled() {
        myFixture.addFileToProject(
            "src/main/resources/application.properties",
            "api.host=example.com\n"
        )
        configureHttpRequest("http://\${api.host}/x")
        val settings = com.luckyframework.idea.spel.settings.LuckyHttpClientSettings.getInstance()
        settings.isEnvironmentPreviewEnabled = false
        try {
            val descriptors = SpringPlaceholderFoldingBuilder()
                .buildFoldRegions(myFixture.file.node, myFixture.editor.document)
            assertTrue(descriptors.isEmpty())
        } finally {
            settings.isEnvironmentPreviewEnabled = true
        }
    }
}
