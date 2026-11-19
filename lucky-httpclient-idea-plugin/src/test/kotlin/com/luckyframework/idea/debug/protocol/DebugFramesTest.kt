package com.luckyframework.idea.debug.protocol

import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugFramesTest {

    @Test
    fun `request flattens payload and carries protocol version`() {
        val payload = JsonObject().apply {
            addProperty("className", "com.example.UserApi")
            addProperty("methodName", "getUser")
        }
        val json = DebugRequest("req-1", DebugProtocol.OP_INVOKE, payload).toJson()

        assertEquals("req-1", json["id"].asString)
        assertEquals(DebugProtocol.OP_INVOKE, json["op"].asString)
        assertEquals(DebugProtocol.VERSION, json["protocolVersion"].asInt)
        assertEquals("com.example.UserApi", json["className"].asString)
        assertEquals("getUser", json["methodName"].asString)
    }

    @Test
    fun `response parses success frame`() {
        val line = """
            {"id":"r1","op":"invoke","ok":true,"elapsedMs":12,
             "payload":{"resultKind":"text","result":"ok"}}
        """.trimIndent()

        val response = DebugResponse.parse(line)
        assertTrue(response.ok)
        assertNull(response.errorCode)
        assertEquals(12L, response.elapsedMs)
        assertEquals("text", response.payloadString("resultKind"))
        assertEquals("ok", response.payloadString("result"))
    }

    @Test
    fun `response parses error frame and maps error code`() {
        val line = """
            {"id":"r2","op":"invoke","ok":false,"errorCode":"BEAN_MISSING",
             "errorMessage":"容器中不存在工厂 Bean","elapsedMs":3,"payload":{}}
        """.trimIndent()

        val response = DebugResponse.parse(line)
        assertFalse(response.ok)
        assertEquals(DebugErrorCode.BEAN_MISSING, response.error)
        assertEquals("容器中不存在工厂 Bean", response.errorMessage)
    }

    @Test
    fun `unknown error code falls back to UNKNOWN`() {
        val response = DebugResponse.parse(
            """{"id":"r3","op":"invoke","ok":false,"errorCode":"NEW_CODE_FROM_FUTURE","payload":{}}"""
        )
        assertEquals(DebugErrorCode.UNKNOWN, response.error)
        assertNull(response.errorMessage)
        assertEquals(0L, response.elapsedMs)
    }

    @Test
    fun `module constants stay aligned with launcher`() {
        // 这些值被 launcher 的 DebugProtocol 逐字复制，改动必须两端同步
        assertEquals(1, DebugProtocol.VERSION)
        assertEquals("LUCKY_DEBUG_PORT=", DebugProtocol.PORT_PREFIX)
        assertEquals("__luckyHttpClientProxyFactory__", DebugProtocol.FACTORY_BEAN_NAME)
        assertEquals("hello", DebugProtocol.OP_HELLO)
        assertEquals("READY", DebugProtocol.STATE_READY)
        assertEquals("BEAN_MISSING", DebugErrorCode.BEAN_MISSING.code)
    }
}
