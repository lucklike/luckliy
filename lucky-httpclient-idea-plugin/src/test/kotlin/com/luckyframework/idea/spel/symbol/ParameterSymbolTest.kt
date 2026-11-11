package com.luckyframework.idea.spel.symbol

/**
 * 6.2 方法参数六种命名与 `@Param` 覆盖。
 */
class ParameterSymbolTest : SymbolFixtureTestCase() {

    fun testThreeParametersProduceEighteenSymbols() {
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
        val variables = context.ofKind(SpelSymbolKind.VARIABLE)

        assertEquals(18, variables.size)
        assertTrue(variables.all { it.origin == SpelSymbolOrigin.PARAMETER })

        val firstNames = variables.take(6).map { it.name }
        assertEquals(
            listOf("keyword", "\$keyword", "\$0", "\$\$0", "keyword::type", "\$0::type"),
            firstNames
        )
        assertTrue(variables.take(6).all { it.type == "String" })
        assertTrue(variables.none { it.name == "q" })

        val pageNames = variables.filter { it.type == "int" }.map { it.name }
        assertEquals(listOf("page", "\$page", "\$1", "\$\$1", "page::type", "\$1::type"), pageNames)
        assertNotNull(context.find("userId", SpelSymbolKind.VARIABLE).single().declaration)
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
        val names = context.ofKind(SpelSymbolKind.VARIABLE).map { it.name }.toSet()

        assertTrue("keyword" in names)
        assertTrue("\$keyword" in names)
        assertTrue("page" in names)
    }
}
