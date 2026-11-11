package com.luckyframework.idea.spel.evaluator

import junit.framework.TestCase

/**
 * 15.2：白名单 TypeLocator 与黑名单 MethodResolver。
 */
class EvaluatorSecurityTest : TestCase() {

    fun testAllowsPureTypes() {
        assertTrue(EvaluatorPolicy.isTypeAllowed("java.util.List"))
        assertTrue(EvaluatorPolicy.isTypeAllowed("java.lang.String"))
        assertTrue(EvaluatorPolicy.isTypeAllowed("java.math.BigDecimal"))
        assertTrue(EvaluatorPolicy.isTypeAllowed("java.time.Duration"))
        assertTrue(EvaluatorPolicy.isTypeAllowed("com.luckyframework.httpclient.proxy.spel.MethodSpace"))
    }

    fun testDeniesDangerousTypes() {
        assertFalse(EvaluatorPolicy.isTypeAllowed("java.io.File"))
        assertFalse(EvaluatorPolicy.isTypeAllowed("java.nio.file.Files"))
        assertFalse(EvaluatorPolicy.isTypeAllowed("java.net.URL"))
        assertFalse(EvaluatorPolicy.isTypeAllowed("java.lang.Runtime"))
        assertFalse(EvaluatorPolicy.isTypeAllowed("java.lang.reflect.Method"))
        assertFalse(EvaluatorPolicy.isTypeAllowed("javax.script.ScriptEngineManager"))
    }

    fun testDenialMessages() {
        assertEquals("文件访问被禁止", EvaluatorPolicy.typeDenialMessage("java.io.File"))
        assertEquals("文件访问被禁止", EvaluatorPolicy.typeDenialMessage("java.nio.file.Files"))
        assertEquals("网络访问被禁止", EvaluatorPolicy.typeDenialMessage("java.net.URL"))
        assertEquals("进程操作被禁止", EvaluatorPolicy.typeDenialMessage("java.lang.Runtime"))
        assertEquals("反射操作被禁止", EvaluatorPolicy.typeDenialMessage("java.lang.reflect.Method"))
    }

    fun testDeniesDangerousMethods() {
        assertFalse(EvaluatorPolicy.isMethodAllowed("java.lang.String", "exec"))
        assertFalse(EvaluatorPolicy.isMethodAllowed("java.lang.System", "getenv"))
        assertFalse(EvaluatorPolicy.isMethodAllowed("java.io.File", "delete"))
        assertTrue(EvaluatorPolicy.isMethodAllowed("java.util.List", "size"))
    }

    fun testFileAccessIsForbidden() {
        assertForbidden(evaluator().evaluate("#{T(java.io.File).separator}"), "文件访问被禁止")
    }

    fun testNetworkAccessIsForbidden() {
        assertForbidden(evaluator().evaluate("#{T(java.net.URL)}"), "网络访问被禁止")
    }

    fun testProcessAccessIsForbidden() {
        assertForbidden(evaluator().evaluate("#{T(java.lang.Runtime).getRuntime()}"), "进程操作被禁止")
    }

    fun testReflectionAccessIsForbidden() {
        assertForbidden(evaluator().evaluate("#{T(java.lang.reflect.Method)}"), "反射操作被禁止")
    }

    fun testAllowedTypeIsEvaluated() {
        val result = evaluator().evaluate("#{T(java.lang.Math).max(1, 5)}")
        assertEquals(EvaluatorResult.Success("5", "Integer"), result)
    }

    fun testAllowedCollectionOperations() {
        val result = evaluator().evaluate("#{T(java.util.Arrays).asList(1, 2, 3).size()}")
        assertEquals(EvaluatorResult.Success("3", "Integer"), result)
    }

    private fun assertForbidden(result: EvaluatorResult, reason: String) {
        assertTrue("应被拒绝: $result", result is EvaluatorResult.Forbidden)
        val forbidden = result as EvaluatorResult.Forbidden
        assertTrue("拒绝原因应包含「$reason」: ${forbidden.reason}", forbidden.reason.contains(reason))
    }

    private fun evaluator(): SpelEvaluator = SpelEvaluator(SpringTestSupport.engine())
}
