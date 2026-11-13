package com.luckyframework.idea.spel.symbol

/**
 * 6.4 符号优先级：方法参数 > 方法级导入 > 类级导入 > 内置 > 全局。
 *
 * 方法参数是 Root 变量（裸名称），与方法级 `@SpELImport(var = ...)` 的普通变量
 * 分属两个空间，同名也不冲突，分别按书写形式解析。
 */
class SymbolPrecedenceTest : SymbolFixtureTestCase() {

    fun testParameterAndMethodImportCoexistByForm() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

class Api {
    @SpELImport(var = "token=methodToken")
    @HttpRequest(url = "#{x}")
    void query(String token) {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))

        assertEquals(SpelSymbolOrigin.PARAMETER, context.rootVariables("token").single().origin)
        assertEquals(SpelSymbolOrigin.METHOD_IMPORT, context.variables("token").single().origin)
    }

    fun testMethodImportBeatsClassImport() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(root = "base=classBase")
class Api {
    @SpELImport(root = "base=methodBase")
    @HttpRequest(url = "#{x}")
    void query() {
    }
}
"""
        )
        val context = newBuilder().build(literalOf("#{x}"))

        assertEquals(SpelSymbolOrigin.METHOD_IMPORT, context.find("base").single().origin)
    }

    fun testClassImportBeatsBuiltinAndBuiltinBeatsGlobal() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;
import com.luckyframework.httpclient.proxy.spel.SpELImport;

@SpELImport(rootLit = "shared=classShared")
class Api {
    @HttpRequest(url = "#{x}")
    void query() {
    }
}
"""
        )
        val context = newBuilder(
            builtin = listOf(
                symbol("shared", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.BUILTIN),
                symbol("\$url\$", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.BUILTIN)
            ),
            global = listOf(
                symbol("\$url\$", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.GLOBAL),
                symbol("gvar", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.GLOBAL)
            )
        ).build(literalOf("#{x}"))

        assertEquals(SpelSymbolOrigin.CLASS_IMPORT, context.find("shared").single().origin)
        assertEquals(SpelSymbolOrigin.BUILTIN, context.find("\$url\$").single().origin)
        assertEquals(SpelSymbolOrigin.GLOBAL, context.find("gvar").single().origin)
    }
}
