package com.luckyframework.idea.spel.symbol

import com.luckyframework.idea.spel.LuckyFixtureTestCase

/**
 * `@SpelSymbol` 变量类型取值链：`typeExpression` → `type` class 字面量 → 字段声明类型。
 *
 * 无信息类型（注解默认的 `Object`、声明为 `Class<?>`）不代表变量在运行期的真实
 * 类型，必须保留为 null；否则 `#{$req$.}` 会把 `Class` 的 `getName` 等方法当成
 * 变量成员展示，误导用户。
 */
class SpelSymbolTypeFallbackTest : LuckyFixtureTestCase() {

    fun testClassLiteralTypeIsUsed() {
        declare(
            """
            @SpelSymbol(type = CustomRequest.class, from = Lifecycle.REQUEST)
            public static final Object requestVar = "custom_request";
            """
        )
        assertEquals("CustomRequest", typeOf("custom_request"))
    }

    fun testTypeExpressionWins() {
        declare(
            """
            @SpelSymbol(typeExpression = "java.lang.Integer", type = CustomRequest.class, from = Lifecycle.REQUEST)
            public static final Object mixedVar = "mixed_var";
            """
        )
        assertEquals("Integer", typeOf("mixed_var"))
    }

    fun testObjectDefaultIsUndetermined() {
        declare(
            """
            @SpelSymbol(from = Lifecycle.REQUEST)
            public static final Object plainVar = "plain_var";
            """
        )
        assertNull("Object 声明类型无信息，应为 null", typeOf("plain_var"))
    }

    fun testClassDeclarationTypeIsUninformative() {
        declare(
            """
            @SpelSymbol(name = "class_var", from = Lifecycle.REQUEST)
            public static final Class<?> classVarHolder = Object.class;
            """
        )
        assertNull("Class<?> 声明类型无信息，应为 null", typeOf("class_var"))
    }

    fun testStringDeclarationTypeIsKept() {
        declare(
            """
            @SpelSymbol(from = Lifecycle.REQUEST)
            public static final String textVar = "text_var";
            """
        )
        assertEquals("String", typeOf("text_var"))
    }

    private fun typeOf(name: String): String? =
        SpelRuntimeSymbols.variables(project).first { it.name == name }.type

    private fun declare(field: String) {
        myFixture.addFileToProject(
            "com/luckyframework/fixture/SymbolTypeProbe.java",
            """
            package com.luckyframework.fixture;

            import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
            import com.luckyframework.httpclient.proxy.spel.meta.SpelSymbol;

            public class SymbolTypeProbe {

                public static class CustomRequest {
                }

                $field
            }
            """
        )
    }
}
