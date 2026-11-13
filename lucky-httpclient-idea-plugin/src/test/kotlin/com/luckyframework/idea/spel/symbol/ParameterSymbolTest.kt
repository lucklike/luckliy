package com.luckyframework.idea.spel.symbol

/**
 * 6.2 方法参数八种命名与 `@Param` 覆盖。
 *
 * 所有参数形式都注册为 Root 变量（裸名称引用），不是普通变量。
 */
class ParameterSymbolTest : SymbolFixtureTestCase() {

    fun testThreeParametersProduceTwentyFourSymbols() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.reflect.Param;

class Api {
    @HttpRequest(url = "#{x}")
    void query(@Param("keyword") String q, int page, String userId) {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))
        val variables = context.ofKind(SpelSymbolKind.ROOT_VARIABLE)

        assertEquals(24, variables.size)
        assertTrue(variables.all { it.origin == SpelSymbolOrigin.PARAMETER })

        val firstNames = variables.take(6).map { it.name }
        assertEquals(
            listOf("keyword", "\$keyword", "\$0", "\$\$0", "keyword::type", "\$0::type"),
            firstNames
        )
        assertTrue(variables.take(4).all { it.type == "String" })
        // `name::type`/`$n::type` 的值是参数类型对象，不是参数值类型
        assertTrue(
            "`::type` 的值类型应为 ResolvableType: ${variables.take(6).map { it.type }}",
            variables.take(6).drop(4).all { it.type == "ResolvableType" }
        )
        // 参数上下文对象：`$pc$n`/`$pc$name`
        assertEquals(listOf("\$pc\$0", "\$pc\$keyword"), variables.take(8).drop(6).map { it.name })
        assertTrue(variables.take(8).drop(6).all { it.type == "ParameterContext" })
        assertTrue(variables.none { it.name == "q" })

        val pageNames = variables.filter { it.type == "int" }.map { it.name }
        assertEquals(listOf("page", "\$page", "\$1", "\$\$1"), pageNames)
        assertNotNull(context.find("userId", SpelSymbolKind.ROOT_VARIABLE).single().declaration)
    }

    /** 八种命名都带运行期语义描述，区分最终值/原始值/类型/上下文。 */
    fun testParameterSymbolsCarryDescriptions() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{x}")
    void query(String keyword, int page) {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))
        val variables = context.ofKind(SpelSymbolKind.ROOT_VARIABLE)
        assertTrue(variables.all { !it.description.isNullOrBlank() })
        assertEquals("参数 keyword 的最终值", variables.first { it.name == "keyword" }.description)
        assertEquals("参数 keyword 的原始值", variables.first { it.name == "\$keyword" }.description)
        assertEquals("第 0 个参数的最终值", variables.first { it.name == "\$0" }.description)
        assertEquals("第 0 个参数的原始值", variables.first { it.name == "\$\$0" }.description)
        assertEquals("参数 keyword 的类型", variables.first { it.name == "keyword::type" }.description)
        assertEquals("第 0 个参数的类型", variables.first { it.name == "\$0::type" }.description)
        assertEquals("ResolvableType", variables.first { it.name == "\$0::type" }.type)
        assertEquals("第 0 个参数的参数上下文", variables.first { it.name == "\$pc\$0" }.description)
        assertEquals("参数 keyword 的参数上下文", variables.first { it.name == "\$pc\$keyword" }.description)
        assertEquals("ParameterContext", variables.first { it.name == "\$pc\$0" }.type)
    }

    fun testCompiledNameUsedWhenParamHasNoValue() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.reflect.Param;

class Api {
    @HttpRequest(url = "#{x}")
    void query(@Param String keyword, String page) {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))
        val names = context.ofKind(SpelSymbolKind.ROOT_VARIABLE).map { it.name }.toSet()

        assertTrue("keyword" in names)
        assertTrue("\$keyword" in names)
        assertTrue("\$pc\$keyword" in names)
        assertTrue("page" in names)
    }

    /**
     * 注入名取的是合并后的 `@Param#value`：`@Query("req")` 的 `value`
     * 经 `@DynamicParam#name` 桥接到 `@Param#value`，最终变量名是 `req`。
     */
    fun testQueryAnnotationRenamesParameterVariables() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.annotations.Query;

class Api {
    @HttpRequest(url = "#{x}")
    void query(@Query("req") String request) {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))
        val names = context.ofKind(SpelSymbolKind.ROOT_VARIABLE).map { it.name }

        assertTrue("缺少 req: \$names", "req" in names)
        assertTrue("缺少 \$req: \$names", "\$req" in names)
        assertTrue("缺少 req::type: \$names", "req::type" in names)
        assertTrue("缺少 \$pc\$req: \$names", "\$pc\$req" in names)
        assertFalse("不应出现未覆盖的 request: \$names", "request" in names)
    }
}
