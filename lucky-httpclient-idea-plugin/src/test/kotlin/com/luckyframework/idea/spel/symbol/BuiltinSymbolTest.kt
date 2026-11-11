package com.luckyframework.idea.spel.symbol

import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 7.2 内置符号表接入符号解析。
 */
class BuiltinSymbolTest : SymbolFixtureTestCase() {

    fun testTableMeetsMinimumCounts() {
        val table = BuiltinSymbolsLoader.load()

        assertTrue(table.tableVersion.isNotBlank())
        assertTrue(table.frameworkVersion.isNotBlank())
        assertTrue("Root 变量不足: ${table.rootVariables.size}", table.rootVariables.size >= 40)
        assertTrue("普通变量不足: ${table.variables.size}", table.variables.size >= 17)
        assertTrue("函数不足: ${table.functions.size}", table.functions.size >= 148)
    }

    fun testBuiltinVariablesResolve() {
        val symbols = BuiltinSymbolsLoader.load().symbols()

        val status = symbols.first { it.name == "\$status\$" }
        assertEquals(SpelSymbolKind.ROOT_VARIABLE, status.kind)
        assertEquals("Integer", status.type)
        assertEquals(SpelLifecycle.RESPONSE_INIT, status.lifecycle)
        assertEquals(SpelSymbolOrigin.BUILTIN, status.origin)
        assertEquals("响应状态码", status.description)

        val body = symbols.first { it.name == "\$body\$" }
        assertEquals(SpelSymbolKind.ROOT_VARIABLE, body.kind)
        assertEquals(SpelLifecycle.RESPONSE_INIT, body.lifecycle)
    }

    fun testBuiltinFunctionsResolve() {
        val symbols = BuiltinSymbolsLoader.load().symbols()

        val time = symbols.first { it.name == "time" }
        assertEquals(SpelSymbolKind.FUNCTION, time.kind)
        assertEquals("time()", time.signature)
        assertEquals("long", time.type)
        assertEquals(SpelSymbolOrigin.BUILTIN, time.origin)
        assertTrue(time.description!!.isNotBlank())

        val md5 = symbols.first { it.name == "md5" }
        assertEquals(SpelSymbolKind.FUNCTION, md5.kind)
        assertEquals("md5(Object, String...)", md5.signature)
        assertEquals("byte[]", md5.type)
        assertEquals("使用MD5算法进行消息摘要", md5.description)
    }

    fun testBuiltinSymbolsArePartOfContext() {
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
        val context = SpelContextBuilder().build(literalOf("#{x}"))

        assertTrue(context.known)
        assertEquals(1, context.find("time", SpelSymbolKind.FUNCTION).size)
        assertEquals(1, context.find("\$status\$", SpelSymbolKind.ROOT_VARIABLE).size)
        // URL 属性属于请求阶段，响应变量在上下文里可见但阶段不可用
        assertEquals(
            SpelVisibility.STAGE_UNAVAILABLE,
            context.find("\$status\$").single().visibility
        )
    }

    fun testVersionStatusAndMessage() {
        val table = BuiltinSymbolsLoader.load()

        assertEquals(BuiltinVersionStatus.VERIFIED, table.versionStatus("3.0.3"))
        assertNull(table.versionMessage("3.0.3"))

        assertEquals(BuiltinVersionStatus.UNVERIFIED, table.versionStatus("4.1.0"))
        assertTrue(table.versionMessage("4.1.0")!!.contains("未验证版本"))

        assertEquals(BuiltinVersionStatus.UNKNOWN, table.versionStatus(null))
        assertTrue(table.versionMessage(null)!!.contains("未验证版本"))
        // 版本未知不阻断解析
        assertFalse(table.symbols().isEmpty())
    }

    fun testVersionDetectionFromJarName() {
        assertEquals("3.0.3", FrameworkVersionDetector.parseVersion("lucky-httpclient-3.0.3.jar"))
        assertEquals("3.0.3", FrameworkVersionDetector.parseVersion("LUCKY-HTTPCLIENT-3.0.3.JAR"))
        assertNull(FrameworkVersionDetector.parseVersion("lucky-httpclient-3.0.3-sources.jar"))
        assertNull(FrameworkVersionDetector.parseVersion("lucky-httpclient.jar"))
    }
}
