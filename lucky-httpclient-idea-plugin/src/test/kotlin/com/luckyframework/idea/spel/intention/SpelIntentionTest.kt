package com.luckyframework.idea.spel.intention

import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 12.6：包裹与取消包裹 SpEL 表达式。
 */
class SpelIntentionTest : SymbolFixtureTestCase() {

    fun testWrapSelection() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "http://<selection>#time()</selection>")
    void call() {
    }
}
"""
        )
        myFixture.launchAction(SpelWrapIntention())

        assertTrue(
            "应包裹为 SpEL 片段: ${myFixture.file.text}",
            myFixture.file.text.contains("http://#{#time()}")
        )
    }

    fun testUnwrapSelection() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "<selection>#{#time()}</selection>")
    void call() {
    }
}
"""
        )
        myFixture.launchAction(SpelUnwrapIntention())

        val text = myFixture.file.text
        assertTrue("应还原为普通表达式: $text", text.contains("\"#time()\""))
        assertFalse("不应保留 SpEL 包裹: $text", text.contains("#{#time()}"))
    }
}
