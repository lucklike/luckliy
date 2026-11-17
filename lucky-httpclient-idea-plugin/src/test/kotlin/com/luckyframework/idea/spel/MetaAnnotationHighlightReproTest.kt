package com.luckyframework.idea.spel

import com.luckyframework.idea.spel.reference.SpelDeclaration
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 函数引用跳转位置的回归测试。
 *
 * 场景来自 starter 的 `StdHttpClient` 等注解：函数名引用必须跳到方法名，
 * 不能落在方法声明前的 Javadoc 注释上。
 */
class MetaAnnotationHighlightReproTest : SymbolFixtureTestCase() {

    /** 函数引用跳转偏移落在方法名上，不能落在 Javadoc 注释起点。 */
    fun testFunctionNavigationTargetsMethodName() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    /**
     * 获取URL
     */
    public static String getUrl() {
        return "";
    }

    @HttpRequest(func = "getUrl")
    void call() {
    }
}
"""
        )
        val literal = literalOf("getUrl")!!
        val declaration = literal.references.single().resolve() as SpelDeclaration
        val method = methodNamed("getUrl")
        val nameOffset = method.nameIdentifier!!.textRange.startOffset

        assertEquals("跳转偏移必须落在方法名上", nameOffset, declaration.textOffset)
    }
}
