package com.luckyframework.idea.spel.symbol

/**
 * 6.1 符号模型：6 种 kind 与 7 种来源，均由上下文构建器真实产出。
 */
class SpelSymbolTest : SymbolFixtureTestCase() {

    fun testAllKindsAreDeclared() {
        assertEquals(
            setOf("VARIABLE", "ROOT_VARIABLE", "FUNCTION", "NAMESPACE", "TYPE", "BEAN"),
            SpelSymbolKind.values().map { it.name }.toSet()
        )
        assertEquals(
            setOf(
                "PARAMETER",
                "METHOD_IMPORT",
                "CLASS_IMPORT",
                "CLASS_STATIC",
                "BUILTIN",
                "GLOBAL",
                "UNKNOWN"
            ),
            SpelSymbolOrigin.values().map { it.name }.toSet()
        )
    }

    fun testContextProducesAllOriginLayers() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(rootLit = "user=admin")
class Api {

    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{x}")
    void query(String host) {
    }
}
"""
        )
        val context = newBuilder(
            builtin = listOf(symbol("\$status\$", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.BUILTIN, "Integer")),
            global = listOf(symbol("gvar", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.GLOBAL))
        ).build(literalOf("#{x}"))

        assertTrue(context.known)
        assertEquals(SpelSymbolOrigin.PARAMETER, context.find("host", SpelSymbolKind.VARIABLE).single().origin)

        val user = context.find("user", SpelSymbolKind.ROOT_VARIABLE).single()
        assertEquals(SpelSymbolOrigin.CLASS_IMPORT, user.origin)
        assertNotNull(user.declaration)
        assertNotNull(user.sourceRange)

        val md5 = context.find("md5", SpelSymbolKind.FUNCTION).single()
        assertEquals(SpelSymbolOrigin.METHOD_IMPORT, md5.origin)
        assertEquals("String", md5.type)
        assertEquals("crypto", md5.namespace)
        assertEquals("md5(String)", md5.signature)

        assertEquals(SpelSymbolOrigin.METHOD_IMPORT, context.find("crypto", SpelSymbolKind.NAMESPACE).single().origin)
        assertEquals(SpelSymbolOrigin.BUILTIN, context.find("\$status\$").single().origin)
        assertEquals(SpelSymbolOrigin.GLOBAL, context.find("gvar").single().origin)
    }

    fun testFunctionRulesFollowClassStaticElement() {
        configure(
            """
package p;

import com.example.MyUtils;
import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(MyUtils.class)
    @HttpRequest(url = "#{x}")
    void query() {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))
        val names = context.ofKind(SpelSymbolKind.FUNCTION).map { it.name }.toSet()

        assertTrue("md5" in names)
        assertTrue("sha" in names)
        assertFalse("sha256" in names)
        assertFalse("filtered" in names)
        assertFalse("hidden" in names)
        assertFalse("instanceMethod" in names)
    }

    fun testStandaloneTypeAndBeanKinds() {
        assertEquals(SpelSymbolKind.TYPE, SpelSymbol("java.util.List", SpelSymbolKind.TYPE).kind)
        assertEquals(SpelSymbolKind.BEAN, SpelSymbol("@userMapper", SpelSymbolKind.BEAN).kind)
    }
}
