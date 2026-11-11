package com.luckyframework.idea.spel.symbol

import com.luckyframework.idea.spel.metadata.AnnotationMetadataLoader
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 6.5 生命周期表与阶段过滤。
 */
class LifecycleTableTest : SymbolFixtureTestCase() {

    fun testAnnotationStageComesFromMetadata() {
        val condition = AnnotationMetadataLoader.load()
            .attribute("com.luckyframework.httpclient.proxy.annotations.Retryable", "condition")
        assertNotNull(condition)
        assertEquals(SpelLifecycle.RETRY, condition!!.lifecycle)

        val url = AnnotationMetadataLoader.load()
            .attribute("com.luckyframework.httpclient.proxy.annotations.HttpRequest", "url")
        assertNotNull(url)
        assertEquals(SpelLifecycle.REQUEST_INIT, url!!.lifecycle)
    }

    fun testBuiltinVariableStages() {
        assertEquals(SpelLifecycle.REQUEST_INIT, LifecycleTable.availableFrom("\$url\$"))
        assertEquals(SpelLifecycle.RESPONSE_INIT, LifecycleTable.availableFrom("\$body\$"))
        assertEquals(SpelLifecycle.THROWABLE, LifecycleTable.availableFrom("\$err\$"))
        assertEquals(SpelLifecycle.THROWABLE, LifecycleTable.availableFrom("\$rootErr\$"))
        assertEquals(SpelLifecycle.METHOD_RESULT, LifecycleTable.availableFrom("\$result\$"))
    }

    fun testStageComparison() {
        assertFalse(LifecycleTable.isAvailable(SpelLifecycle.REQUEST_INIT, SpelLifecycle.RESPONSE_INIT))
        assertTrue(LifecycleTable.isAvailable(SpelLifecycle.RESPONSE_INIT, SpelLifecycle.RESPONSE_INIT))
        assertTrue(LifecycleTable.isAvailable(SpelLifecycle.RETRY, SpelLifecycle.RESPONSE_INIT))
        assertFalse(LifecycleTable.isAvailable(SpelLifecycle.RESPONSE, SpelLifecycle.THROWABLE))
        // 方法级注解与未确认属性不参与过滤
        assertTrue(LifecycleTable.isAvailable(SpelLifecycle.METHOD, SpelLifecycle.THROWABLE))
        assertTrue(LifecycleTable.isAvailable(SpelLifecycle.UNKNOWN, SpelLifecycle.METHOD_RESULT))
    }

    fun testResponseVariableIsStageUnavailableInRequestStage() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.HttpRequest;

class Api {
    @HttpRequest(url = "#{x}")
    void fetch() {
    }
}
"""
        )
        val context = newBuilder(
            builtin = listOf(
                symbol("\$body\$", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.BUILTIN, "Object"),
                symbol("\$url\$", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.BUILTIN, "String")
            )
        ).build(literalOf("#{x}"))

        assertEquals(SpelLifecycle.REQUEST_INIT, context.stage)
        assertEquals(SpelVisibility.STAGE_UNAVAILABLE, context.find("\$body\$").single().visibility)
        assertEquals(SpelVisibility.AVAILABLE, context.find("\$url\$").single().visibility)
        // 「阶段不可用」是可见但当前阶段不可用，不能被当成未解析符号
        assertEquals(1, context.find("\$body\$").size)
    }

    fun testResponseVariableIsAvailableInRetryStage() {
        configure(
            """
package p;

import com.luckyframework.httpclient.proxy.annotations.Retryable;

class Api {
    @Retryable(condition = "#{x}")
    void retry() {
    }
}
"""
        )
        val context = newBuilder(
            builtin = listOf(
                symbol("\$status\$", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.BUILTIN, "Integer"),
                symbol("\$err\$", SpelSymbolKind.ROOT_VARIABLE, SpelSymbolOrigin.BUILTIN, "Throwable")
            )
        ).build(literalOf("#{x}"))

        assertEquals(SpelLifecycle.RETRY, context.stage)
        assertEquals(SpelVisibility.AVAILABLE, context.find("\$status\$").single().visibility)
        assertEquals(SpelVisibility.STAGE_UNAVAILABLE, context.find("\$err\$").single().visibility)
    }
}
