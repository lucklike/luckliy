package com.luckyframework.idea.spel.evaluator

import junit.framework.TestCase

/**
 * 15.3/15.4：结果展示、模拟变量、失败提示与超时。
 */
class SpelEvaluatorTest : TestCase() {

    fun testArithmeticExpression() {
        val result = evaluator().evaluate("#{1 + 2}")

        assertEquals(EvaluatorResult.Success("3", "Integer"), result)
        assertEquals("结果：3（Integer）", SpelEvaluationFormatter.format(result))
    }

    fun testProvidedVariable() {
        val result = evaluator().evaluate("#{#name + '!'}", mapOf("name" to "abc"))

        assertEquals(EvaluatorResult.Success("\"abc!\"", "String"), result)
    }

    fun testMissingVariableStopsEvaluation() {
        val result = evaluator().evaluate("#{#missing + 1}")

        assertEquals(EvaluatorResult.MissingVariable("#missing"), result)
        assertEquals("变量 #missing 未提供", SpelEvaluationFormatter.format(result))
    }

    fun testReferencedVariablesIgnoreReservedNames() {
        val names = SpelEvaluationInput.referencedVariables("#root.id + #name + #name")

        assertEquals(listOf("name"), names)
    }

    fun testValueParsing() {
        val values = SpelEvaluationInput.parseValues(
            """
            #name = "abc"
            count=42
            ratio=1.5
            enabled=true
            nothing=null
            """.trimIndent()
        )

        assertEquals("abc", values["name"])
        assertEquals(42L, values["count"])
        assertEquals(1.5, values["ratio"])
        assertEquals(true, values["enabled"])
        assertTrue(values.containsKey("nothing"))
        assertNull(values["nothing"])
    }

    fun testSyntaxErrorReportsFailure() {
        val result = evaluator().evaluate("#{#fn(1,}")

        assertTrue("应求值失败: $result", result is EvaluatorResult.Failure)
        val message = (result as EvaluatorResult.Failure).message
        assertTrue("应指出括号不配平: $message", message.contains("不配平"))
    }

    fun testStringLiteralIsScanned() {
        val result = evaluator().evaluate("\"v=#{1 + 2}\"")

        assertEquals(EvaluatorResult.Success("3", "Integer"), result)
    }

    fun testTimeoutStopsLongEvaluation() {
        val engine = EvaluatorEngine { _, _ ->
            Thread.sleep(10_000)
            null
        }
        val started = System.currentTimeMillis()
        val result = SpelEvaluator(engine, timeoutMillis = 200).evaluate("#{1 + 1}")
        val elapsed = System.currentTimeMillis() - started

        assertTrue("应超时: $result", result is EvaluatorResult.Timeout)
        assertEquals("求值超时（1 秒）", SpelEvaluationFormatter.format(result))
        assertTrue("应在超时后立即返回: ${elapsed}ms", elapsed < 5_000)
    }

    fun testUnavailableMessage() {
        val result = SpelEvaluationFormatter.format(EvaluatorResult.Unavailable("缺少 spring-expression"))

        assertEquals("无法求值：缺少 spring-expression", result)
    }

    private fun evaluator(): SpelEvaluator = SpelEvaluator(SpringTestSupport.engine())
}
