package com.luckyframework.idea.spel.intention

import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 13.3：提取表达式为 `@SpELImport` 变量。
 */
class SpelExtractVariableTest : SymbolFixtureTestCase() {

    fun testExtractWholeExpressionContent() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{<selection>#time()</selection>}")
    void call() {
    }
}
"""
        )
        myFixture.launchAction(SpelExtractVariableIntention())

        val text = myFixture.file.text
        assertTrue("应插入 @SpELImport: $text", text.contains("@SpELImport"))
        assertTrue("应保留原表达式: $text", text.contains("t=#{#time()}"))
        assertTrue("原位置应换成变量引用: $text", text.contains("\"#{#t}\""))
    }

    fun testExtractPartOfExpression() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{'a' + <selection>#time()</selection>}")
    void call() {
    }
}
"""
        )
        myFixture.launchAction(SpelExtractVariableIntention())

        val text = myFixture.file.text
        assertTrue("应保留原表达式: $text", text.contains("t=#{#time()}"))
        assertTrue("部分选区应替换为 #t: $text", text.contains("#{'a' + #t}"))
    }

    fun testExtractAvoidsExistingVariableName() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{<selection>#time()</selection>}")
    void call(String t) {
    }
}
"""
        )
        myFixture.launchAction(SpelExtractVariableIntention())

        val text = myFixture.file.text
        assertTrue("应避开已存在的 t: $text", text.contains("t1=#{#time()}"))
        assertTrue("原位置应换成 #t1: $text", text.contains("\"#{#t1}\""))
    }
}
