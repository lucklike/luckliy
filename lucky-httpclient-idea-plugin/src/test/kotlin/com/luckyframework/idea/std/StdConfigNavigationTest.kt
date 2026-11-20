package com.luckyframework.idea.std

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.std.yaml.StdYamlLineMarkerProvider
import com.luckyframework.idea.std.yaml.StdYamlReferenceProvider
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * 双向跳转与行标记：YAML 键引用、configId 字面量 Ctrl+Click、行标记开关。
 */
class StdConfigNavigationTest : StdFixtureTestCase() {

    fun testConfigIdReferenceResolvesToInterface() {
        addJava(
            "GiteeApi.java",
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
            }
            """
        )
        configureYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  Git<caret>eeApi:
                    url: https://gitee.com
            """
        )

        val reference = referenceAtCaret()

        assertNotNull(reference)
        val target = reference!!.resolve()
        assertTrue(target is PsiClass)
        assertEquals("GiteeApi", (target as PsiClass).name)
    }

    fun testApiIdReferenceResolvesToMethod() {
        addJava(
            "GiteeApi.java",
            """
            import com.luckyframework.httpclient.proxy.configapi.Api;
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {

                @Api("user-info")
                void getUserInfo();
            }
            """
        )
        configureYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    method-configs:
                      user-<caret>info:
                        path: /user
            """
        )

        val reference = referenceAtCaret()

        assertNotNull(reference)
        val target = reference!!.resolve()
        assertTrue(target is PsiMethod)
        assertEquals("getUserInfo", (target as PsiMethod).name)
    }

    fun testConfigIdLiteralGotoDeclaration() {
        addYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    url: https://gitee.com
            """
        )
        configureJava(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "<caret>GiteeApi")
            public interface GiteeApi {
            }
            """
        )

        val targets = StdConfigGotoDeclarationHandler().getGotoDeclarationTargets(
            myFixture.file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNotNull(targets)
        assertEquals(1, targets!!.size)
        assertEquals("GiteeApi", targets.single().text.substringBefore(':'))
    }

    fun testOtherAttributeDoesNotNavigate() {
        addYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    url: https://gitee.com
            """
        )
        configureJava(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi", path = "<caret>/api")
            public interface GiteeApi {
            }
            """
        )

        val targets = StdConfigGotoDeclarationHandler().getGotoDeclarationTargets(
            myFixture.file.findElementAt(myFixture.caretOffset),
            myFixture.caretOffset,
            myFixture.editor
        )

        assertNull(targets)
    }

    fun testLineMarkerForConfiguredInterface() {
        addYaml(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    url: https://gitee.com
            """
        )
        val psiClass = configureClass(
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
            }
            """
        )

        val marker = lineMarkerOf(psiClass.nameIdentifier!!)

        assertNotNull(marker)
    }

    fun testMissingConfigMarkerRespectsSetting() {
        val settings = LuckyHttpClientSettings.getInstance()
        val previous = settings.stdConfigMissingHint
        try {
            settings.stdConfigMissingHint = true
            val psiClass = configureClass(
                """
                import io.github.lucklike.httpclient.std.StdHttpClient;

                @StdHttpClient(configId = "NoConfigApi")
                public interface NoConfigApi {
                }
                """
            )
            assertNotNull(lineMarkerOf(psiClass.nameIdentifier!!))

            settings.stdConfigMissingHint = false
            assertNull(lineMarkerOf(psiClass.nameIdentifier!!))
        } finally {
            settings.stdConfigMissingHint = previous
        }
    }

    fun testPlainClassHasNoMarker() {
        val psiClass = configureClass(
            """
            public class PlainClass {
            }
            """
        )

        assertNull(lineMarkerOf(psiClass.nameIdentifier!!))
    }

    fun testYamlConfigMarkerNavigatesBackToInterface() {
        addJava(
            "GiteeApi.java",
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
            }
            """
        )
        val provider = StdYamlLineMarkerProvider()
        val (keyValue, keyLeaf) = yamlKeyAtCaret(
            """
            lucky:
              http-client:
                standard-client-configs:
                  Git<caret>eeApi:
                    url: https://gitee.com
            """
        )

        assertNotNull(yamlLineMarkerOf(keyLeaf))
        val navigation = provider.markerOf(keyValue)
        assertNotNull(navigation)
        val target = navigation!!.targets.single()
        assertTrue(target is PsiClass)
        assertEquals("GiteeApi", (target as PsiClass).name)
        assertTrue(navigation.tooltip.contains("GiteeApi"))
    }

    fun testYamlQuotedConfigMarkerNavigatesBackToInterface() {
        addJava(
            "GiteeApi.java",
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
            }
            """
        )
        val provider = StdYamlLineMarkerProvider()
        val (keyValue, keyLeaf) = yamlKeyAtCaret(
            """
            lucky:
              http-client:
                standard-client-configs:
                  "Git<caret>eeApi":
                    url: https://gitee.com
            """
        )

        assertNotNull(yamlLineMarkerOf(keyLeaf))
        val target = provider.markerOf(keyValue)!!.targets.single()
        assertEquals("GiteeApi", (target as PsiClass).name)
    }

    fun testYamlMethodMarkerNavigatesBackToMethod() {
        addJava(
            "GiteeApi.java",
            """
            import com.luckyframework.httpclient.proxy.configapi.Api;
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {

                @Api("user-info")
                void getUserInfo();
            }
            """
        )
        val provider = StdYamlLineMarkerProvider()
        val (keyValue, keyLeaf) = yamlKeyAtCaret(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    method-configs:
                      user-<caret>info:
                        path: /user
            """
        )

        assertNotNull(yamlLineMarkerOf(keyLeaf))
        val target = provider.markerOf(keyValue)!!.targets.single()
        assertTrue(target is PsiMethod)
        assertEquals("getUserInfo", (target as PsiMethod).name)
    }

    fun testYamlMissingConfigMarkerUsesWarningIcon() {
        val settings = LuckyHttpClientSettings.getInstance()
        val previous = settings.stdConfigMissingHint
        try {
            settings.stdConfigMissingHint = true
            val provider = StdYamlLineMarkerProvider()
            val (keyValue, keyLeaf) = yamlKeyAtCaret(
                """
                lucky:
                  http-client:
                    standard-client-configs:
                      NoJava<caret>Api:
                        url: https://example.com
                """
            )

            val marker = yamlLineMarkerOf(keyLeaf)
            assertNotNull(marker)
            assertSame(LuckyIcons.ConfigMissing, marker!!.icon)
            val result = provider.markerOf(keyValue)
            assertNotNull(result)
            assertTrue(result!!.missing)
            assertTrue(result.tooltip.contains("NoJavaApi"))
            assertTrue(result.tooltip.contains("未找到"))
        } finally {
            settings.stdConfigMissingHint = previous
        }
    }

    fun testYamlMissingMarkerRespectsSetting() {
        val settings = LuckyHttpClientSettings.getInstance()
        val previous = settings.stdConfigMissingHint
        try {
            settings.stdConfigMissingHint = false
            val provider = StdYamlLineMarkerProvider()
            val (_, keyLeaf) = yamlKeyAtCaret(
                """
                lucky:
                  http-client:
                    standard-client-configs:
                      NoJava<caret>Api:
                        url: https://example.com
                """
            )

            assertNull(yamlLineMarkerOf(keyLeaf))
        } finally {
            settings.stdConfigMissingHint = previous
        }
    }

    fun testYamlMissingMethodMarker() {
        addJava(
            "GiteeApi.java",
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
            }
            """
        )
        val settings = LuckyHttpClientSettings.getInstance()
        val previous = settings.stdConfigMissingHint
        try {
            settings.stdConfigMissingHint = true
            val provider = StdYamlLineMarkerProvider()
            val (keyValue, keyLeaf) = yamlKeyAtCaret(
                """
                lucky:
                  http-client:
                    standard-client-configs:
                      GiteeApi:
                        method-configs:
                          missing-<caret>api:
                            path: /user
                """
            )

            val marker = yamlLineMarkerOf(keyLeaf)
            assertNotNull(marker)
            assertSame(LuckyIcons.ConfigMissing, marker!!.icon)
            val result = provider.markerOf(keyValue)
            assertNotNull(result)
            assertTrue(result!!.missing)
            assertTrue(result.tooltip.contains("missing-api"))
            assertTrue(result.tooltip.contains("未找到接口方法"))
        } finally {
            settings.stdConfigMissingHint = previous
        }
    }

    fun testYamlMarkerAbsentWhenPluginDisabled() {
        addJava(
            "GiteeApi.java",
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
            }
            """
        )
        val settings = LuckyHttpClientSettings.getInstance()
        val previous = settings.isEnabled
        try {
            settings.isEnabled = false
            val provider = StdYamlLineMarkerProvider()
            val (keyValue, keyLeaf) = yamlKeyAtCaret(
                """
                lucky:
                  http-client:
                    standard-client-configs:
                      Git<caret>eeApi:
                        url: https://gitee.com
                """
            )

            assertNull(yamlLineMarkerOf(keyLeaf))
            assertNull(provider.markerOf(keyValue))
        } finally {
            settings.isEnabled = previous
        }
    }

    fun testYamlMarkerAbsentForValueKey() {
        addJava(
            "GiteeApi.java",
            """
            import io.github.lucklike.httpclient.std.StdHttpClient;

            @StdHttpClient(configId = "GiteeApi")
            public interface GiteeApi {
            }
            """
        )
        val provider = StdYamlLineMarkerProvider()
        val (_, valueLeaf) = yamlKeyAtCaret(
            """
            lucky:
              http-client:
                standard-client-configs:
                  GiteeApi:
                    u<caret>rl: https://gitee.com
            """
        )

        assertNull(yamlLineMarkerOf(valueLeaf))
    }

    private fun addYaml(text: String) {
        myFixture.addFileToProject("config.yml", text.trimIndent())
    }

    /**
     * 通过编辑器创建 Java 文件，确保注解索引能立即看到新增的接口；
     * 后续的 YAML 用例会把当前文件切回去。
     */
    private fun addJava(name: String, text: String) {
        myFixture.configureByText(name, text.trimIndent())
    }

    private fun configureYaml(text: String) {
        myFixture.configureByText("config.yml", text.trimIndent())
    }

    private fun configureJava(text: String) {
        myFixture.configureByText("Api.java", text.trimIndent())
    }

    private fun configureClass(text: String): PsiClass {
        configureJava(text)
        return (myFixture.file as PsiJavaFile).classes.single()
    }

    /**
     * 光标处的 YAML 键值对与其首个叶子。
     *
     * 平台只把叶子节点传给 `collectSlowLineMarkers`，行标记只对键的首个叶子生效。
     */
    private fun yamlKeyAtCaret(text: String): Pair<YAMLKeyValue, PsiElement> {
        configureYaml(text)
        val leaf = myFixture.file.findElementAt(myFixture.caretOffset)
        assertNotNull(leaf)
        val keyValue = PsiTreeUtil.getParentOfType(leaf, YAMLKeyValue::class.java, false)
        assertNotNull(keyValue)
        return keyValue!! to leaf!!
    }

    /**
     * 测试容器不加载插件的 `META-INF/plugin.xml`，引用贡献者注册由生产配置保证；
     * 这里直接调用提供者验证 YAML 键到 Java 元素的解析结果。
     */
    private fun referenceAtCaret(): PsiReference? {
        val element = myFixture.file.findElementAt(myFixture.caretOffset) ?: return null
        val keyValue = PsiTreeUtil.getParentOfType(element, YAMLKeyValue::class.java, false)
            ?: return null
        return StdYamlReferenceProvider()
            .getReferencesByElement(keyValue, ProcessingContext())
            .singleOrNull()
    }
}
