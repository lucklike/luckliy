package com.luckyframework.idea.spel.metadata

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 抽查人工确认过的高频注解属性，防止生成脚本或手工修改把值种类/生命周期改错。
 */
class AnnotationMetadataGoldenTest : BasePlatformTestCase() {

    private val table get() = AnnotationMetadataLoader.load()

    fun testHighFrequencyAttributes() {
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.HttpRequest", "url",
            SpelValueKind.TEMPLATE, SpelLifecycle.REQUEST_INIT
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.HttpRequest", "value",
            SpelValueKind.TEMPLATE, SpelLifecycle.REQUEST_INIT
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.ServerAddress", "url",
            SpelValueKind.TEMPLATE, SpelLifecycle.REQUEST_INIT
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.ServerAddress", "path",
            SpelValueKind.TEMPLATE, SpelLifecycle.REQUEST_INIT
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.Retryable", "condition",
            SpelValueKind.IF_CONDITION, SpelLifecycle.RETRY
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.Retryable", "enable",
            SpelValueKind.IF_CONDITION, SpelLifecycle.RETRY
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.RetryMeta", "enable",
            SpelValueKind.IF_CONDITION, SpelLifecycle.RETRY
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.spel.If", "condition",
            SpelValueKind.IF_CONDITION, SpelLifecycle.METHOD
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.Condition", "assertion",
            SpelValueKind.IF_CONDITION, SpelLifecycle.UNKNOWN
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.Condition", "enable",
            SpelValueKind.IF_CONDITION, SpelLifecycle.UNKNOWN
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.StaticQuery", "condition",
            SpelValueKind.IF_CONDITION, SpelLifecycle.REQUEST_INIT
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.spel.hook.Hook", "enable",
            SpelValueKind.IF_CONDITION, SpelLifecycle.UNKNOWN
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.AutoRedirect", "condition",
            SpelValueKind.IF_CONDITION, SpelLifecycle.RESPONSE
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.PrintLog", "reqCondition",
            SpelValueKind.IF_CONDITION, SpelLifecycle.REQUEST_INIT
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.annotations.PrintLog", "respCondition",
            SpelValueKind.IF_CONDITION, SpelLifecycle.RESPONSE
        )
        assertAttribute(
            "com.luckyframework.httpclient.proxy.spel.SpELImport", "var",
            SpelValueKind.KEY_VALUE, SpelLifecycle.METHOD
        )
    }

    fun testNonSpelAttributesAreMarkedNone() {
        assertAttribute(
            "com.luckyframework.httpclient.proxy.spel.SpELImport", "separator",
            SpelValueKind.NONE, SpelLifecycle.METHOD
        )
    }

    private fun assertAttribute(
        annotation: String,
        attribute: String,
        kind: SpelValueKind,
        lifecycle: SpelLifecycle
    ) {
        val metadata = table.attribute(annotation, attribute)
        assertNotNull("缺少 $annotation#$attribute", metadata)
        assertEquals("$annotation#$attribute kind", kind, metadata!!.kind)
        assertEquals("$annotation#$attribute lifecycle", lifecycle, metadata.lifecycle)
    }
}
