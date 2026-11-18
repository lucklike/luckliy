package com.luckyframework.idea.spel.metadata

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.yaml.SpelYamlConfigPath
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * `@SpelConfig` 元数据读取与配置节点映射。
 *
 * 覆盖：prefix/aliases/scope/naming 读取，字段级 key/kind/resultType/keySpel/expandable，
 * naming 推导，继承字段合并，占位符/别名/嵌套模型/Map 实例键的路径匹配，以及 YAML 键路径收集。
 */
class SpelConfigMetadataTest : LuckyFixtureTestCase() {

    // ---------------------------------------------------------------------------------------------
    // @SpelConfig 读取
    // ---------------------------------------------------------------------------------------------

    fun testReadsConfigMetadata() {
        installModels()

        val config = configOf("demo.ApiConfig")

        assertEquals("lucky.api", config.prefix)
        assertEquals(listOf("lucky.api-alias"), config.aliases)
        assertEquals("KEBAB_CASE", config.naming)
        assertEquals("BOTH", config.scope)
        assertEquals("接口配置", config.description)
        assertFalse(config.nested)
    }

    fun testNestedConfigHasEmptyPrefix() {
        installModels()

        val config = configOf("demo.RetryConfiguration")

        assertTrue(config.nested)
        assertEquals("", config.prefix)
        assertEquals(listOf("retry-config"), config.aliases)
    }

    fun testReadsAttributeLevelFields() {
        installModels()
        val model = modelOf("demo.ApiConfig")

        val url = model.field("url")!!
        assertEquals(SpelValueKind.NONE, url.attribute.kind)
        assertEquals("java.lang.String", url.attribute.resultType)
        assertFalse(url.attribute.keySpel)
        assertFalse(url.attribute.expandable)

        val headers = model.field("headers")!!
        assertEquals(SpelValueKind.MAP_EXPRESSION, headers.attribute.kind)
        assertTrue(headers.attribute.keySpel)
        assertTrue(headers.attribute.expandable)
        assertEquals("java.util.Map", headers.declaredType)
        assertEquals("java.lang.Object", headers.valueType)

        val retry = model.field("retry-config")!!
        assertEquals(SpelValueKind.STRUCT, retry.attribute.kind)
        assertEquals("demo.RetryConfiguration", retry.attribute.resultType)
        assertEquals("demo.RetryConfiguration", retry.valueType)
    }

    // ---------------------------------------------------------------------------------------------
    // key 推导与继承
    // ---------------------------------------------------------------------------------------------

    fun testKeyFallsBackToNaming() {
        installModels()
        val model = modelOf("demo.ApiConfig")

        // 显式 key 优先：baseUrl 声明为 url
        assertNotNull(model.field("url"))
        assertNull(model.field("baseUrl"))
        assertNull(model.field("base-url"))
        // 未声明 key 的字段按 KEBAB_CASE 推导
        assertNotNull(model.field("connect-timeout"))
        assertNull(model.field("connectTimeout"))
    }

    fun testSnakeAndCamelNaming() {
        installModels()
        val models = models()

        assertNotNull(models.first { it.fqName == "demo.SnakeConfig" }.field("connect_timeout"))
        assertNotNull(models.first { it.fqName == "demo.CamelConfig" }.field("connectTimeout"))
    }

    fun testInheritedFieldsAreMerged() {
        installModels()
        val model = modelOf("demo.ChildConfig")

        assertNotNull(model.field("child"))
        assertNotNull(model.field("parent"))
    }

    // ---------------------------------------------------------------------------------------------
    // 路径匹配
    // ---------------------------------------------------------------------------------------------

    fun testResolvesPrimaryPrefixPath() {
        installModels()

        val match = resolver().resolve(listOf("lucky", "api", "url"))!!

        assertEquals("demo.ApiConfig", match.model.fqName)
        assertEquals("url", match.field.key)
        assertTrue(match.remaining.isEmpty())
    }

    fun testResolvesAliasPath() {
        installModels()

        val match = resolver().resolve(listOf("lucky", "api-alias", "url"))!!

        assertEquals("url", match.field.key)
    }

    fun testPlaceholderIsCaptured() {
        installModels()

        val match = resolver().resolve(listOf("lucky", "client", "GiteeApi", "url"))!!

        assertEquals(mapOf("configId" to "GiteeApi"), match.placeholders)
        assertEquals("url", match.field.key)
    }

    fun testAliasPlaceholdersAreResolved() {
        installModels()

        val match = resolver()
            .resolve(listOf("lucky", "client", "GiteeApi", "methods", "findUser", "url"))!!

        assertEquals(mapOf("configId" to "GiteeApi", "apiId" to "findUser"), match.placeholders)
        assertEquals("url", match.field.key)
    }

    fun testMapInstanceKeyIsSkipped() {
        installModels()

        val match = resolver()
            .resolve(listOf("lucky", "client", "GiteeApi", "method-configs", "findUser", "url"))!!

        assertEquals("url", match.field.key)
        assertEquals("demo.ClientConfig", match.model.fqName)
    }

    fun testNestedModelFieldIsResolved() {
        installModels()

        val match = resolver().resolve(listOf("lucky", "api", "retry-config", "count"))!!

        assertEquals("demo.RetryConfiguration", match.model.fqName)
        assertEquals("count", match.field.key)
        assertEquals(SpelValueKind.NONE, match.field.attribute.kind)
    }

    fun testMapValueKeepsRemainingSegments() {
        installModels()

        val match = resolver().resolve(listOf("lucky", "api", "headers", "token"))!!

        assertEquals("headers", match.field.key)
        assertEquals(listOf("token"), match.remaining)
    }

    fun testUnknownPathReturnsNull() {
        installModels()
        val resolver = resolver()

        assertNull(resolver.resolve(listOf("other", "key")))
        // 只有前缀、没有字段段
        assertNull(resolver.resolve(listOf("lucky", "api")))
        assertNull(resolver.resolve(emptyList()))
    }

    // ---------------------------------------------------------------------------------------------
    // YAML 键路径
    // ---------------------------------------------------------------------------------------------

    fun testYamlPathSkipsSequenceItems() {
        myFixture.configureByText(
            "application.yml",
            """
            lucky:
              api:
                conditions:
                  - body: "#{#x}"
            """.trimIndent()
        )

        val scalar = scalarWith("#{#x}")

        assertEquals(listOf("lucky", "api", "conditions", "body"), SpelYamlConfigPath.of(scalar))
    }

    fun testYamlPathUnwrapsQuotedKeys() {
        myFixture.configureByText(
            "application.yml",
            """
            lucky:
              "api":
                url: "#{#x}"
            """.trimIndent()
        )

        val scalar = scalarWith("#{#x}")

        assertEquals(listOf("lucky", "api", "url"), SpelYamlConfigPath.of(scalar))
    }

    // ---------------------------------------------------------------------------------------------
    // 夹具
    // ---------------------------------------------------------------------------------------------

    private fun configOf(fqName: String): SpelConfigMetadata =
        SpelMetaAnnotationReader().readConfig(classOf(fqName))!!

    private fun models(): List<SpelConfigModel> = SpelConfigIndex.getInstance(project).models()

    private fun modelOf(fqName: String): SpelConfigModel =
        models().firstOrNull { it.fqName == fqName } ?: error("未找到配置模型：$fqName")

    private fun resolver(): SpelConfigPathResolver = SpelConfigPathResolver(models())

    private fun classOf(fqName: String): PsiClass =
        JavaPsiFacade.getInstance(project)
            .findClass(fqName, GlobalSearchScope.allScope(project))
            ?: error("未找到类：$fqName")

    private fun scalarWith(text: String): YAMLScalar {
        val file = myFixture.file as YAMLFile
        return PsiTreeUtil.findChildrenOfType(file, YAMLScalar::class.java)
            .first { it.text.contains(text) }
    }

    private fun installModels() {
        JDK_STUBS.forEach { (path, text) ->
            myFixture.addFileToProject(path, text.trimIndent())
        }
        MODELS.forEach { (path, text) ->
            myFixture.addFileToProject(path, text.trimIndent())
        }
    }

    private companion object {

        /** MockJdk 不含 java.lang/java.util 类，补齐元数据读取用到的类型。 */
        val JDK_STUBS = mapOf(
            "java/lang/Object.java" to """
                package java.lang;

                public class Object {
                }
            """,
            "java/lang/String.java" to """
                package java.lang;

                public final class String {
                }
            """,
            "java/lang/Integer.java" to """
                package java.lang;

                public final class Integer {
                }
            """,
            "java/lang/Boolean.java" to """
                package java.lang;

                public final class Boolean {
                }
            """,
            "java/util/Map.java" to """
                package java.util;

                public interface Map<K, V> {
                }
            """
        )

        val MODELS = mapOf(
            "demo/ApiConfig.java" to """
                package demo;

                import com.luckyframework.httpclient.proxy.spel.meta.ConfigScope;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelNaming;

                import java.util.Map;

                @SpelConfig(prefix = "lucky.api", aliases = {"lucky.api-alias"}, scope = ConfigScope.BOTH,
                            naming = SpelNaming.KEBAB_CASE, description = "接口配置")
                public class ApiConfig {

                    @SpelAttribute(key = "url", kind = SpelKind.NONE, resultType = String.class)
                    private String baseUrl;

                    @SpelAttribute(key = "headers", kind = SpelKind.MAP_EXPRESSION, keySpel = true, expandable = true)
                    private Map<String, Object> headers;

                    @SpelAttribute(kind = SpelKind.NONE)
                    private String connectTimeout;

                    @SpelAttribute(key = "retry-config", kind = SpelKind.STRUCT, resultType = RetryConfiguration.class)
                    private RetryConfiguration retryConfig;
                }
            """,
            "demo/RetryConfiguration.java" to """
                package demo;

                import com.luckyframework.httpclient.proxy.spel.meta.ConfigScope;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

                @SpelConfig(prefix = "", aliases = {"retry-config"}, scope = ConfigScope.BOTH, description = "重试配置")
                public class RetryConfiguration {

                    @SpelAttribute(key = "count", kind = SpelKind.NONE, resultType = Integer.class)
                    private int count;

                    @SpelAttribute(key = "condition", kind = SpelKind.CONDITION, resultType = Boolean.class)
                    private String condition;
                }
            """,
            "demo/ClientConfig.java" to """
                package demo;

                import com.luckyframework.httpclient.proxy.spel.meta.ConfigScope;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

                import java.util.Map;

                @SpelConfig(prefix = "lucky.client.{configId}",
                            aliases = {"lucky.client.{configId}.methods.{apiId}"}, scope = ConfigScope.BOTH)
                public class ClientConfig {

                    @SpelAttribute(key = "url", kind = SpelKind.NONE, resultType = String.class)
                    private String url;

                    @SpelAttribute(key = "method-configs", kind = SpelKind.STRUCT)
                    private Map<String, ClientConfig> methodConfigs;
                }
            """,
            "demo/ParentConfig.java" to """
                package demo;

                import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

                @SpelConfig(prefix = "lucky.parent")
                public class ParentConfig {

                    @SpelAttribute(key = "parent", kind = SpelKind.NONE)
                    private String parent;
                }
            """,
            "demo/ChildConfig.java" to """
                package demo;

                import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

                @SpelConfig(prefix = "lucky.child")
                public class ChildConfig extends ParentConfig {

                    @SpelAttribute(key = "child", kind = SpelKind.NONE)
                    private String child;
                }
            """,
            "demo/SnakeConfig.java" to """
                package demo;

                import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelNaming;

                @SpelConfig(prefix = "lucky.snake", naming = SpelNaming.SNAKE_CASE)
                public class SnakeConfig {

                    @SpelAttribute(kind = SpelKind.NONE)
                    private String connectTimeout;
                }
            """,
            "demo/CamelConfig.java" to """
                package demo;

                import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelNaming;

                @SpelConfig(prefix = "lucky.camel", naming = SpelNaming.CAMEL_CASE)
                public class CamelConfig {

                    @SpelAttribute(kind = SpelKind.NONE)
                    private String connect_timeout;
                }
            """
        )
    }
}
