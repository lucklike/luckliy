package com.luckyframework.idea.spel.yaml

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.extensions.Extensions
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.inspection.SpelConfigTypeMismatchInspection

/**
 * `@SpelConfig` 字段期望类型与 YAML 表达式结果类型的兼容性检查。
 *
 * 覆盖：布尔/数值字段的确定冲突（数值→布尔、布尔→数值）、数值字段的
 * 安全组合（数值运算、字符串内容未知）、String 字段放宽、模板拼接跳过、
 * Map 条目按 value 类型判定、NONE 字段与未命中路径跳过、嵌套模型，
 * 以及检查在 `yaml-spel-support.xml` 的注册。
 */
class SpelConfigTypeMismatchTest : LuckyFixtureTestCase() {

    fun testBooleanFieldWithNumberExpressionWarns() {
        val infos = incompatibilitiesOf(
            """
            lucky:
              demo:
                GiteeApi:
                  enable: "#{1 + 2}"
            """
        )

        assertEquals(1, infos.size)
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
        assertTrue(infos.first().description!!.contains("boolean"))
    }

    fun testNumberFieldWithBooleanExpressionWarns() {
        val infos = incompatibilitiesOf(
            """
            lucky:
              demo:
                GiteeApi:
                  count: "#{true}"
            """
        )

        assertEquals(1, infos.size)
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
    }

    /** 数值字段 + 数值表达式：同一类别，转换一定成功。 */
    fun testNumberFieldWithNumberExpressionPasses() {
        assertTrue(
            incompatibilitiesOf(
                """
                lucky:
                  demo:
                    GiteeApi:
                      count: "#{1 + 2}"
                """
            ).isEmpty()
        )
    }

    /** 比较表达式的结果一定是 boolean。 */
    fun testBooleanFieldWithBooleanExpressionPasses() {
        assertTrue(
            incompatibilitiesOf(
                """
                lucky:
                  demo:
                    GiteeApi:
                      enable: "#{1 + 2 == 3}"
                """
            ).isEmpty()
        )
    }

    /** String 字段靠 String.valueOf 兜底，任何结果都能转换。 */
    fun testStringFieldWithAnyExpressionPasses() {
        assertTrue(
            incompatibilitiesOf(
                """
                lucky:
                  demo:
                    GiteeApi:
                      body: "#{1 + 2}"
                """
            ).isEmpty()
        )
    }

    /** String 结果的内容未知："123" 可以解析成数值。 */
    fun testNumberFieldWithStringExpressionPasses() {
        assertTrue(
            incompatibilitiesOf(
                """
                lucky:
                  demo:
                    GiteeApi:
                      count: "#{'123'}"
                """
            ).isEmpty()
        )
    }

    /** 未知符号推导不出类型，resultType 回退为期望类型，不应误报。 */
    fun testUnknownExpressionPasses() {
        assertTrue(
            incompatibilitiesOf(
                """
                lucky:
                  demo:
                    GiteeApi:
                      count: "#{#missing}"
                """
            ).isEmpty()
        )
    }

    /** 模板拼接的结果是 String，内容无法静态确定，跳过检查。 */
    fun testTemplateConcatenationPasses() {
        assertTrue(
            incompatibilitiesOf(
                """
                lucky:
                  demo:
                    GiteeApi:
                      count: "count-#{1}"
                """
            ).isEmpty()
        )
    }

    /** 多个片段同样是 String 拼接，跳过检查。 */
    fun testMultipleFragmentsPass() {
        assertTrue(
            incompatibilitiesOf(
                """
                lucky:
                  demo:
                    GiteeApi:
                      count: "#{1}#{2}"
                """
            ).isEmpty()
        )
    }

    /** Map 条目的期望类型是 value 类型（String），布尔结果也能转换。 */
    fun testMapEntryUsesValueType() {
        assertTrue(
            incompatibilitiesOf(
                """
                lucky:
                  demo:
                    GiteeApi:
                      headers:
                        token: "#{true}"
                """
            ).isEmpty()
        )
    }

    /** `kind = NONE` 的字段不参与表达式解析，不做类型判定。 */
    fun testNonExpressionFieldIsSkipped() {
        assertTrue(
            incompatibilitiesOf(
                """
                lucky:
                  demo:
                    GiteeApi:
                      url: "#{1 + 2}"
                """
            ).isEmpty()
        )
    }

    /** 路径未命中任何配置模型时没有期望类型，不做判定。 */
    fun testUnmatchedPathIsSkipped() {
        assertTrue(
            incompatibilitiesOf(
                """
                other:
                  key: "#{1 + 2}"
                """
            ).isEmpty()
        )
    }

    /** 嵌套模型字段：`retry-config.count` 的 resultType 是 Integer。 */
    fun testNestedModelFieldIsChecked() {
        val infos = incompatibilitiesOf(
            """
            lucky:
              demo:
                GiteeApi:
                  retry-config:
                    count: "#{true}"
            """
        )

        assertEquals(1, infos.size)
        assertEquals(HighlightSeverity.WARNING, infos.first().severity)
    }

    fun testRegisteredInYamlSupportXml() {
        val extensions = Extensions.getExtensions(LocalInspectionEP.LOCAL_INSPECTION).toList()
        val inspection = extensions.firstOrNull {
            it.implementationClass == SpelConfigTypeMismatchInspection::class.java.name
        }

        assertNotNull("yaml-spel-support.xml 未注册配置值类型检查", inspection)
        assertEquals("WARNING", inspection!!.level)
        assertTrue(inspection.enabledByDefault)
    }

    // ---------------------------------------------------------------------------------------------
    // 夹具
    // ---------------------------------------------------------------------------------------------

    private fun incompatibilitiesOf(yamlText: String): List<HighlightInfo> {
        installModels()
        myFixture.configureByText("application.yml", yamlText.trimIndent())
        myFixture.enableInspections(SpelConfigTypeMismatchInspection())
        return myFixture.doHighlighting()
            .filter { it.description?.contains("不兼容") == true }
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

        /** MockJdk 不含 java.lang/java.util 类，补齐配置模型与类型推导用到的类型。 */
        val JDK_STUBS = mapOf(
            "java/lang/Object.java" to """
                package java.lang;

                public class Object {
                }
            """,
            "java/lang/CharSequence.java" to """
                package java.lang;

                public interface CharSequence {
                }
            """,
            "java/lang/String.java" to """
                package java.lang;

                public final class String implements CharSequence {
                }
            """,
            "java/lang/Integer.java" to """
                package java.lang;

                public final class Integer extends Number {
                }
            """,
            "java/lang/Number.java" to """
                package java.lang;

                public abstract class Number {
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

                    @SpelAttribute(key = "enable", kind = SpelKind.EXPRESSION)
                    private boolean enable;

                    @SpelAttribute(key = "count", kind = SpelKind.EXPRESSION)
                    private int count;

                    @SpelAttribute(key = "retry-config", kind = SpelKind.STRUCT)
                    private RetryConfig retryConfig;
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
