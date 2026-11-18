package com.luckyframework.idea.spel.environment

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
