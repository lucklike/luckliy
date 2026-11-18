package com.luckyframework.idea.spel.yaml

import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.completion.SpelTypeResolver
import com.luckyframework.idea.spel.highlight.SpelTextAttributes
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * YAML 键路径 → `@SpelConfig` 字段的上下文接线。
 *
 * 覆盖：`kind` 白名单门控（`NONE`/`STRUCT` 跳过，`EXPRESSION`/`MAP_EXPRESSION`
 * 保留，补全/引用/高亮三侧一致）、期望类型（`resultType` 优先于声明类型、
 * 嵌套模型、Map 条目值类型）、路径未命中保持既有行为，以及类型推导对
 * 期望类型的使用。
 */
class SpelYamlConfigContextTest : LuckyFixtureTestCase() {

    // ---------------------------------------------------------------------------------------------
    // kind 白名单门控
    // ---------------------------------------------------------------------------------------------

    fun testNonExpressionFieldSkipsContext() {
        installModels()
        val scalar = scalarOf(
            """
            lucky:
              demo:
                GiteeApi:
                  url: "#{#host}"
            """,
            "#{#host}"
        )

        assertFalse("NONE 字段不应进入表达式解析", builder.build(scalar).known)
        assertEquals("url", builder.configFieldOf(scalar)?.key)
    }

    fun testNonExpressionFieldHasNoReference() {
        installModels()
        val scalar = scalarOf(
            """
            springElImport:
              varLit:
                token: "abc"
            lucky:
              demo:
                GiteeApi:
                  url: "#{#token}"
            """,
            "#{#token}"
        )

        assertEquals(0, scalar.references.size)
    }

    fun testExpressionFieldHasReference() {
        installModels()
        val scalar = scalarOf(
            """
            springElImport:
              varLit:
                token: "abc"
            lucky:
              demo:
                GiteeApi:
                  body: "#{#token}"
            """,
            "#{#token}"
        )

        assertEquals(1, scalar.references.size)
    }

    fun testNonExpressionFieldHasNoCompletion() {
        installModels()
        myFixture.configureByText(
            "application.yml",
            """
            lucky:
              demo:
                GiteeApi:
                  url: "#{ti<caret>}"
            """.trimIndent()
        )

        myFixture.completeBasic()

        val lookups = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()
        assertFalse("NONE 字段不应有 SpEL 补全: $lookups", lookups.contains("time"))
    }

    fun testExpressionFieldHasCompletion() {
        installModels()
        myFixture.configureByText(
            "application.yml",
            """
            lucky:
              demo:
                GiteeApi:
                  body: "#{ti<caret>}"
            """.trimIndent()
        )

        myFixture.completeBasic()

        val lookups = myFixture.lookupElements?.map { it.lookupString } ?: emptyList()
        assertTrue("EXPRESSION 字段应有 SpEL 补全: $lookups", lookups.contains("time"))
    }

    fun testNonExpressionFieldHasNoHighlight() {
        installModels()
        myFixture.configureByText(
            "application.yml",
            """
            lucky:
              demo:
                GiteeApi:
                  url: "http://#{#host}/api"
            """.trimIndent()
        )

        val highlighted = myFixture.doHighlighting()
            .filter { it.forcedTextAttributesKey?.externalName?.startsWith("LUCKY_SPEL_") == true }

        assertTrue(
            "NONE 字段不应有 SpEL 高亮: ${highlighted.map { it.description }}",
            highlighted.isEmpty()
        )
    }

    fun testExpressionFieldHasHighlight() {
        installModels()
        myFixture.configureByText(
            "application.yml",
            """
            lucky:
              demo:
                GiteeApi:
                  body: "http://#{#host}/api"
            """.trimIndent()
        )

        val infos = myFixture.doHighlighting()

        assertTrue(
            "EXPRESSION 字段应有 SpEL 高亮: ${infos.map { it.forcedTextAttributesKey?.externalName }}",
            infos.any { it.forcedTextAttributesKey == SpelTextAttributes.VARIABLE }
        )
    }

    fun testStructFieldSkipsContext() {
        installModels()
        val scalar = scalarOf(
            """
            lucky:
              demo:
                GiteeApi:
                  retry-config: "#{1}"
            """,
            "#{1}"
        )

        assertFalse("STRUCT 字段本身是子配置，不应按表达式解析", builder.build(scalar).known)
    }

    /** `STRUCT` 字段内部没有对应配置模型时，层级归属不确定，不擅自跳过。 */
    fun testStructWithoutModelKeepsInnerContext() {
        installModels()
        val scalar = scalarOf(
            """
            lucky:
              demo:
                GiteeApi:
                  extra:
                    anything: "#{1}"
            """,
            "#{1}"
        )

        assertTrue(builder.build(scalar).known)
        assertEquals("extra", builder.configFieldOf(scalar)?.key)
    }

    // ---------------------------------------------------------------------------------------------
    // 期望类型
    // ---------------------------------------------------------------------------------------------

    fun testExpressionFieldSuppliesExpectedType() {
        installModels()
        val scalar = scalarOf(
            """
            lucky:
              demo:
                GiteeApi:
                  body: "#{1}"
            """,
            "#{1}"
        )

        assertEquals("java.lang.String", builder.build(scalar).expectedType?.canonicalText)
    }

    fun testExplicitResultTypeWinsOverDeclaredType() {
        installModels()
        val scalar = scalarOf(
            """
            lucky:
              demo:
                GiteeApi:
                  limit: "#{1}"
            """,
            "#{1}"
        )

        // 字段声明为 long，但 resultType 显式声明为 Integer
        assertEquals("java.lang.Integer", builder.build(scalar).expectedType?.canonicalText)
    }

    fun testNestedModelFieldIsReached() {
        installModels()
        val scalar = scalarOf(
            """
            lucky:
              demo:
                GiteeApi:
                  retry-config:
                    count: "#{1}"
            """,
            "#{1}"
        )

        val context = builder.build(scalar)

        assertTrue(context.known)
        assertEquals("count", builder.configFieldOf(scalar)?.key)
        assertEquals("java.lang.Integer", context.expectedType?.canonicalText)
    }

    fun testMapEntryUsesValueTypeAsExpectedType() {
        installModels()
        val scalar = scalarOf(
            """
            lucky:
              demo:
                GiteeApi:
                  headers:
                    token: "#{1}"
            """,
            "#{1}"
        )

        val context = builder.build(scalar)

        assertTrue(context.known)
        assertEquals("headers", builder.configFieldOf(scalar)?.key)
        assertEquals("java.lang.String", context.expectedType?.canonicalText)
    }

    fun testUnmatchedPathKeepsContext() {
        installModels()
        val scalar = scalarOf(
            """
            other:
              key: "#{1}"
            """,
            "#{1}"
        )

        val context = builder.build(scalar)

        assertTrue("未命中配置模型时保持原有行为", context.known)
        assertNull(context.expectedType)
    }

    // ---------------------------------------------------------------------------------------------
    // 类型推断
    // ---------------------------------------------------------------------------------------------

    fun testResultTypeFallsBackToExpectedType() {
        installModels()
        val scalar = scalarOf(
            """
            lucky:
              demo:
                GiteeApi:
                  limit: "#{#missing}"
            """,
            "#{#missing}"
        )
        val context = builder.build(scalar)

        val result = SpelTypeResolver(project, context).resultType("#{#missing}")

        assertEquals("java.lang.Integer", result?.canonicalText)
    }

    fun testResultTypeKeepsInferredType() {
        installModels()
        val scalar = scalarOf(
            """
            lucky:
              demo:
                GiteeApi:
                  limit: "#{'abc'}"
            """,
            "#{'abc'}"
        )
        val context = builder.build(scalar)

        val result = SpelTypeResolver(project, context).resultType("'abc'")

        assertEquals("java.lang.String", result?.canonicalText)
    }

    // ---------------------------------------------------------------------------------------------
    // 夹具
    // ---------------------------------------------------------------------------------------------

    private val builder = SpelYamlContextBuilder()

    private fun scalarOf(yamlText: String, marker: String): YAMLScalar {
        myFixture.configureByText("application.yml", yamlText.trimIndent())
        return PsiTreeUtil.findChildrenOfType(myFixture.file, YAMLScalar::class.java)
            .first { it.text.contains(marker) }
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

        /** MockJdk 不含 java.lang/java.util 类，补齐配置模型用到的类型。 */
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
            "java/util/Map.java" to """
                package java.util;

                public interface Map<K, V> {
                }
            """
        )

        val MODELS = mapOf(
            "demo/DemoConfig.java" to """
                package demo;

                import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

                import java.util.Map;

                @SpelConfig(prefix = "lucky.demo.{configId}")
                public class DemoConfig {

                    @SpelAttribute(key = "url", kind = SpelKind.NONE)
                    private String url;

                    @SpelAttribute(key = "body", kind = SpelKind.EXPRESSION)
                    private String body;

                    @SpelAttribute(key = "headers", kind = SpelKind.MAP_EXPRESSION, keySpel = true)
                    private Map<String, String> headers;

                    @SpelAttribute(key = "limit", kind = SpelKind.EXPRESSION, resultType = Integer.class)
                    private long limit;

                    @SpelAttribute(key = "retry-config", kind = SpelKind.STRUCT)
                    private RetryConfig retryConfig;

                    @SpelAttribute(key = "extra", kind = SpelKind.STRUCT)
                    private Object extra;
                }
            """,
            "demo/RetryConfig.java" to """
                package demo;

                import com.luckyframework.httpclient.proxy.spel.meta.SpelAttribute;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelConfig;
                import com.luckyframework.httpclient.proxy.spel.meta.SpelKind;

                @SpelConfig(prefix = "", aliases = {"retry-config"})
                public class RetryConfig {

                    @SpelAttribute(key = "count", kind = SpelKind.TEMPLATE, resultType = Integer.class)
                    private int count;
                }
            """
        )
    }
}
