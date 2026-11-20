package com.luckyframework.idea.std

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings

/**
 * 「生成 StdHttpClient 配置」：最小化结构写入、已有内容保留、幂等性，
 * 以及缺失行标记与意图动作的入口。
 */
class StdConfigGenerateTest : StdFixtureTestCase() {

    fun testGenerateIntoEmptyYaml() {
        val psiClass = configureClass(GITEE_API)
        val yaml = myFixture.addFileToProject("application.yml", "")
        val index = StdHttpClientIndex.getInstance(project)
        assertTrue(index.entryOf(psiClass)!!.configNodes.isEmpty())

        assertTrue(StdConfigGenerator.generate(yaml, "GiteeApi"))

        assertTrue(yaml.text.contains("standard-client-configs:"))
        assertTrue(yaml.text.contains("GiteeApi:"))
        assertTrue(yaml.text.contains("url: https://example.com"))
        val entry = index.entryOf(psiClass)!!
        assertEquals(1, entry.configNodes.size)
        assertEquals("GiteeApi", entry.configNodes.single().configId)
    }

    fun testGenerateKeepsExistingContent() {
        val psiClass = configureClass(GITEE_API)
        val yaml = myFixture.addFileToProject("application.yml", "server:\n  port: 8080\n")

        assertTrue(StdConfigGenerator.generate(yaml, "GiteeApi"))

        assertTrue(yaml.text.contains("port: 8080"))
        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass)!!
        assertEquals(1, entry.configNodes.size)
    }

    fun testGenerateMergesIntoExistingLuckyBlock() {
        val psiClass = configureClass(GITEE_API)
        val yaml = myFixture.addFileToProject(
            "application.yml",
            """
            lucky:
              http-client:
                other: 1
            """.trimIndent()
        )

        assertTrue(StdConfigGenerator.generate(yaml, "GiteeApi"))

        assertTrue(yaml.text.contains("other: 1"))
        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass)!!
        assertEquals(1, entry.configNodes.size)
        assertEquals("GiteeApi", entry.configNodes.single().configId)
    }

    fun testGenerateRespectsFourSpaceIndent() {
        val psiClass = configureClass(GITEE_API)
        val yaml = myFixture.addFileToProject(
            "application.yml",
            "lucky:\n    http-client:\n        other: 1\n"
        )

        assertTrue(StdConfigGenerator.generate(yaml, "GiteeApi"))

        assertTrue(yaml.text.contains("        standard-client-configs:"))
        assertTrue(yaml.text.contains("            GiteeApi:"))
        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass)!!
        assertEquals(1, entry.configNodes.size)
    }

    fun testGenerateFillsEmptyLuckyKey() {
        val psiClass = configureClass(GITEE_API)
        val yaml = myFixture.addFileToProject("application.yml", "lucky:\n")

        assertTrue(StdConfigGenerator.generate(yaml, "GiteeApi"))

        assertTrue(yaml.text.contains("  http-client:"))
        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass)!!
        assertEquals(1, entry.configNodes.size)
        assertEquals("GiteeApi", entry.configNodes.single().configId)
    }

    fun testGenerateIsIdempotent() {
        configureClass(GITEE_API)
        val yaml = myFixture.addFileToProject("application.yml", "")

        assertTrue(StdConfigGenerator.generate(yaml, "GiteeApi"))
        assertFalse(StdConfigGenerator.generate(yaml, "GiteeApi"))

        assertEquals(1, Regex("standard-client-configs:").findAll(yaml.text).count())
        assertEquals(1, Regex("GiteeApi:").findAll(yaml.text).count())
    }

    fun testMissingConfigMarkerOffersGenerateAction() {
        val settings = LuckyHttpClientSettings.getInstance()
        val previous = settings.stdConfigMissingHint
        try {
            settings.stdConfigMissingHint = true
            val psiClass = configureClass(GITEE_API)

            val marker = lineMarkerOf(psiClass.nameIdentifier!!)

            assertNotNull(marker)
            assertSame(LuckyIcons.ConfigMissing, marker!!.icon)
            assertNotNull(marker.navigationHandler)
            assertTrue(marker.lineMarkerTooltip.contains("点击生成"))
        } finally {
            settings.stdConfigMissingHint = previous
        }
    }

    fun testConfiguredMarkerHasNoGenerateAction() {
        configureClass(GITEE_API)
        myFixture.addFileToProject("application.yml", CONFIGURED_YAML)
        val psiClass = (myFixture.file as PsiJavaFile).classes.single()

        val marker = lineMarkerOf(psiClass.nameIdentifier!!)

        assertNotNull(marker)
        assertSame(LuckyIcons.Config, marker!!.icon)
        assertFalse(marker.lineMarkerTooltip.contains("点击生成"))
    }

    fun testIntentionAvailableForInterfaceWithoutConfig() {
        val psiClass = configureClass(GITEE_API)
        myFixture.editor.caretModel.moveToOffset(psiClass.nameIdentifier!!.textRange.startOffset + 1)

        val intention = StdGenerateConfigIntention()

        assertTrue(intention.isAvailable(project, myFixture.editor, myFixture.file))
    }

    fun testIntentionUnavailableWhenConfigExists() {
        myFixture.addFileToProject("application.yml", CONFIGURED_YAML)
        val psiClass = configureClass(GITEE_API)
        myFixture.editor.caretModel.moveToOffset(psiClass.nameIdentifier!!.textRange.startOffset + 1)

        val intention = StdGenerateConfigIntention()

        assertFalse(intention.isAvailable(project, myFixture.editor, myFixture.file))
    }

    fun testGeneratedFileIsCandidate() {
        val yaml = myFixture.addFileToProject("application.yml", "")
        myFixture.addFileToProject("other.yml", "")

        val candidates = StdConfigGenerator.candidates(project)

        assertEquals(yaml.virtualFile, candidates.first().virtualFile)
    }

    private fun configureClass(text: String): PsiClass {
        myFixture.configureByText("GiteeApi.java", text.trimIndent())
        return (myFixture.file as PsiJavaFile).classes.single()
    }

    private companion object {
        const val GITEE_API = """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
            }
        """

        const val CONFIGURED_YAML = """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    url: https://gitee.com
        """
    }
}
