package com.luckyframework.idea.debug.invoke

import com.google.gson.JsonObject
import com.luckyframework.idea.debug.protocol.DebugErrorCode
import com.luckyframework.idea.debug.protocol.DebugRequest
import com.luckyframework.idea.debug.protocol.DebugResponse
import com.luckyframework.idea.debug.rpc.DebugTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugCallExecutorTest {

    @Test
    fun `form values become an invoke request with parameter types`() {
        lateinit var captured: DebugRequest
        val executor = DebugCallExecutor { request, _ ->
            captured = request
            okResponse(jsonPayload())
        }

        val outcome = executor.invoke(
            target(parameter("token", "java.lang.String", DebugParameterKind.STRING)),
            mapOf("token" to "u1")
        )

        assertTrue(outcome is DebugCallOutcome.Success)
        assertEquals("demo.Api", captured.payload.get("className").asString)
        assertEquals("get", captured.payload.get("methodName").asString)
        assertEquals(
            listOf("java.lang.String"),
            captured.payload.getAsJsonArray("parameterTypes").map { it.asString }
        )
        val argument = captured.payload.getAsJsonArray("arguments")[0].asJsonObject
        assertEquals("string", argument.get("type").asString)
        assertEquals("u1", argument.get("value").asString)
    }

    @Test
    fun `json arguments are encoded by name`() {
        lateinit var captured: DebugRequest
        val executor = DebugCallExecutor { request, _ ->
            captured = request
            okResponse(jsonPayload())
        }

        val outcome = executor.invoke(
            target(
                parameter("token", "java.lang.String", DebugParameterKind.STRING),
                parameter("count", "int", DebugParameterKind.INT)
            ),
            jsonArguments = """{"token":"u1","count":3}"""
        )

        assertTrue(outcome is DebugCallOutcome.Success)
        val arguments = captured.payload.getAsJsonArray("arguments")
        assertEquals("u1", arguments[0].asJsonObject.get("value").asString)
        assertEquals(3L, arguments[1].asJsonObject.get("value").asLong)
    }

    @Test
    fun `codec failure is reported as argument error`() {
        val executor = DebugCallExecutor { _, _ -> error("must not be called") }

        val outcome = executor.invoke(
            target(parameter("count", "int", DebugParameterKind.INT)),
            mapOf("count" to "abc")
        )

        assertTrue(outcome is DebugCallOutcome.Failure)
        outcome as DebugCallOutcome.Failure
        assertEquals(DebugErrorCode.UNKNOWN, outcome.code)
        assertTrue(outcome.message.contains("参数格式错误"))
        assertTrue(outcome.detail.orEmpty().contains("不是合法整数"))
    }

    @Test
    fun `error response keeps code message and stack`() {
        val executor = DebugCallExecutor { _, _ ->
            errorResponse(
                DebugErrorCode.INVOCATION_TARGET.code,
                "目标方法执行失败",
                "demo.Api.get(Api.java:10)"
            )
        }

        val outcome = executor.invoke(
            target(parameter("token", "java.lang.String", DebugParameterKind.STRING)),
            mapOf("token" to "u1")
        )

        assertTrue(outcome is DebugCallOutcome.Failure)
        outcome as DebugCallOutcome.Failure
        assertEquals(DebugErrorCode.INVOCATION_TARGET, outcome.code)
        assertEquals("目标方法执行失败", outcome.message)
        assertTrue(outcome.detail.orEmpty().contains("Api.java:10"))
    }

    @Test
    fun `timeout is reported with the wait window`() {
        val executor = DebugCallExecutor { _, timeout ->
            throw DebugTimeoutException("req-1", timeout)
        }

        val outcome = executor.invoke(
            target(parameter("token", "java.lang.String", DebugParameterKind.STRING)),
            mapOf("token" to "u1"),
            timeoutMillis = 1_500L
        )

        assertTrue(outcome is DebugCallOutcome.Failure)
        outcome as DebugCallOutcome.Failure
        assertEquals(DebugErrorCode.TIMEOUT, outcome.code)
        assertTrue(outcome.message.contains("1500ms"))
    }

    @Test
    fun `json result is pretty printed`() {
        val executor = DebugCallExecutor { _, _ ->
            okResponse(
                JsonObject().apply {
                    addProperty("returnType", "java.util.Map")
                    addProperty("resultKind", "json")
                    addProperty("result", """{"a":1,"b":{"c":2}}""")
                }
            )
        }

        val outcome = executor.invoke(target()) as DebugCallOutcome.Success

        assertEquals("java.util.Map", outcome.returnType)
        assertEquals("json", outcome.kind)
        assertTrue(outcome.body.contains("\n"))
        assertTrue(outcome.body.contains("\"a\": 1"))
    }

    @Test
    fun `void and null results have explicit labels`() {
        val void = executorWith(JsonObject().apply {
            addProperty("returnType", "void")
            addProperty("resultKind", "void")
        }).invoke(target()) as DebugCallOutcome.Success
        assertEquals("void（方法没有返回值）", void.body)

        val nullResult = executorWith(JsonObject().apply {
            addProperty("returnType", "java.lang.String")
            addProperty("resultKind", "null")
        }).invoke(target()) as DebugCallOutcome.Success
        assertEquals("null", nullResult.body)
    }

    @Test
    fun `bytes result reports base64 length`() {
        val executor = executorWith(JsonObject().apply {
            addProperty("returnType", "byte[]")
            addProperty("resultKind", "bytes")
            addProperty("base64", "AQID")
        })

        val outcome = executor.invoke(target()) as DebugCallOutcome.Success

        assertTrue(outcome.body.contains("Base64 长度：4"))
        assertTrue(outcome.body.contains("AQID"))
    }

    @Test
    fun `response result is expanded as json`() {
        val response = JsonObject().apply {
            addProperty("status", 200)
            addProperty("contentType", "application/json")
        }
        val executor = executorWith(JsonObject().apply {
            addProperty("returnType", "com.luckyframework.httpclient.core.meta.Response")
            addProperty("resultKind", "response")
            add("response", response)
        })

        val outcome = executor.invoke(target()) as DebugCallOutcome.Success

        assertEquals("response", outcome.kind)
        assertTrue(outcome.body.contains("\"status\": 200"))
    }

    private fun executorWith(payload: JsonObject): DebugCallExecutor =
        DebugCallExecutor { _, _ -> okResponse(payload) }

    private fun okResponse(payload: JsonObject, elapsedMs: Long = 12L): DebugResponse {
        val root = JsonObject().apply {
            addProperty("id", "req-1")
            addProperty("op", "invoke")
            addProperty("protocolVersion", 1)
            addProperty("ok", true)
            add("payload", payload)
            addProperty("elapsedMs", elapsedMs)
        }
        return DebugResponse.parse(root.toString())
    }

    private fun errorResponse(code: String, message: String, stack: String?): DebugResponse {
        val root = JsonObject().apply {
            addProperty("id", "req-1")
            addProperty("op", "invoke")
            addProperty("protocolVersion", 1)
            addProperty("ok", false)
            addProperty("errorCode", code)
            addProperty("errorMessage", message)
            if (stack != null) {
                addProperty("stackTrace", stack)
            }
            add("payload", JsonObject())
            addProperty("elapsedMs", 5L)
        }
        return DebugResponse.parse(root.toString())
    }

    private fun jsonPayload() = JsonObject().apply {
        addProperty("returnType", "java.lang.String")
        addProperty("resultKind", "text")
        addProperty("result", "ok")
    }

    private fun target(vararg parameters: DebugParameter): DebugTarget = DebugTarget(
        moduleName = "demo",
        className = "demo.Api",
        methodName = "get",
        parameters = parameters.toList(),
        signature = "get(${parameters.joinToString { it.typeName }})"
    )

    private fun parameter(
        name: String,
        typeName: String,
        kind: DebugParameterKind
    ): DebugParameter = DebugParameter(
        index = 0,
        name = name,
        typeName = typeName,
        kind = kind
    )
}
