package com.luckyframework.idea.spel.evaluator

import com.luckyframework.idea.spel.intention.SpelEvaluateIntention
import com.luckyframework.idea.spel.symbol.SymbolFixtureTestCase

/**
 * 15.4：后台受控求值，成功/失败/超时都不修改源文件。
 */
class SpelEvaluationFixtureTest : SymbolFixtureTestCase() {

    fun testSuccessfulEvaluationKeepsSourceUnchanged() {
        configure(source("http://localhost/#{1 + 2}"))
        val before = myFixture.file.text

        val result = SpelEvaluator(SpringTestSupport.engine()).evaluate("#{1 + 2}")

        assertEquals(EvaluatorResult.Success("3", "Integer"), result)
        assertEquals("求值不应修改源文件", before, myFixture.file.text)
    }

    fun testFailedEvaluationKeepsSourceUnchanged() {
        configure(source("http://localhost/#{#fn(1,}"))
        val before = myFixture.file.text

        val result = SpelEvaluator(SpringTestSupport.engine()).evaluate("#{#fn(1,}")

        assertTrue("应求值失败: $result", result is EvaluatorResult.Failure)
        assertEquals("失败不应修改源文件", before, myFixture.file.text)
    }

    fun testTimeoutKeepsSourceUnchanged() {
        configure(source("http://localhost/#{1 + 2}"))
        val before = myFixture.file.text
        val engine = EvaluatorEngine { _, _ ->
            Thread.sleep(10_000)
            null
        }

        val result = SpelEvaluator(engine, timeoutMillis = 200).evaluate("#{1 + 2}")

        assertTrue("应超时: $result", result is EvaluatorResult.Timeout)
        assertEquals("超时不应修改源文件", before, myFixture.file.text)
    }

    fun testIntentionAvailableInsideFragment() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "http://localhost/#{1 + <caret>2}")
    void call() {
    }
}
"""
        )

        assertTrue(
            "片段内应提供求值意图",
            SpelEvaluateIntention().isAvailable(myFixture.project, myFixture.editor, myFixture.file)
        )
    }

    fun testIntentionHiddenOutsideFragment() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "plain <caret>text")
    void call() {
    }
}
"""
        )

        assertFalse(
            "普通字符串不应提供求值意图",
            SpelEvaluateIntention().isAvailable(myFixture.project, myFixture.editor, myFixture.file)
        )
    }

    fun testIntentionHiddenOutsideJavaLiteral() {
        myFixture.configureByText(
            "api.txt",
            "plain #{1 + 2} <caret>"
        )

        assertFalse(
            "非 Java 文件不应提供求值意图",
            SpelEvaluateIntention().isAvailable(myFixture.project, myFixture.editor, myFixture.file)
        )
    }

    private fun source(url: String): String = """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "$url")
    void call() {
    }
}
"""
}
