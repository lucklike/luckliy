package com.luckyframework.idea.debug.invoke

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugArgumentCodecTest {

    @Test
    fun `form encodes primitive literals`() {
        val parameters = listOf(
            parameter("name", "java.lang.String", DebugParameterKind.STRING),
            parameter("enabled", "boolean", DebugParameterKind.BOOLEAN),
            parameter("count", "int", DebugParameterKind.INT),
            parameter("total", "long", DebugParameterKind.LONG),
            parameter("ratio", "double", DebugParameterKind.DOUBLE),
            parameter("initial", "char", DebugParameterKind.CHAR),
            parameter("amount", "java.math.BigDecimal", DebugParameterKind.DECIMAL),
            parameter("huge", "java.math.BigInteger", DebugParameterKind.BIGINT)
        )

        val encoded = DebugArgumentCodec.encodeForm(
            mapOf(
                "name" to "lucky",
                "enabled" to "true",
                "count" to "42",
                "total" to "9007199254740993",
                "ratio" to "1.5",
                "initial" to "L",
                "amount" to "12.345",
                "huge" to "12345678901234567890"
            ),
            parameters
        )

        assertEquals("string", encoded[0].asJsonObject["type"].asString)
        assertEquals("lucky", encoded[0].asJsonObject["value"].asString)
        assertEquals("boolean", encoded[1].asJsonObject["type"].asString)
        assertTrue(encoded[1].asJsonObject["value"].asBoolean)
        assertEquals("int", encoded[2].asJsonObject["type"].asString)
        assertEquals(42L, encoded[2].asJsonObject["value"].asLong)
        assertEquals("long", encoded[3].asJsonObject["type"].asString)
        // long 以字符串传输，避免 JSON 数字精度丢失
        assertEquals("9007199254740993", encoded[3].asJsonObject["value"].asString)
        assertEquals("double", encoded[4].asJsonObject["type"].asString)
        assertEquals("char", encoded[5].asJsonObject["type"].asString)
        assertEquals("decimal", encoded[6].asJsonObject["type"].asString)
        assertEquals("bigint", encoded[7].asJsonObject["type"].asString)
    }

    @Test
    fun `form keeps empty string for string parameter`() {
        val encoded = DebugArgumentCodec.encodeForm(
            mapOf("name" to ""),
            listOf(parameter("name", "java.lang.String", DebugParameterKind.STRING))
        )
        assertEquals("string", encoded[0].asJsonObject["type"].asString)
        assertEquals("", encoded[0].asJsonObject["value"].asString)
    }

    @Test(expected = DebugArgumentCodec.CodecException::class)
    fun `form rejects empty required primitive`() {
        DebugArgumentCodec.encodeForm(
            mapOf("count" to ""),
            listOf(parameter("count", "int", DebugParameterKind.INT))
        )
    }

    @Test(expected = DebugArgumentCodec.CodecException::class)
    fun `form rejects invalid boolean`() {
        DebugArgumentCodec.encodeForm(
            mapOf("enabled" to "yes"),
            listOf(parameter("enabled", "boolean", DebugParameterKind.BOOLEAN))
        )
    }

    @Test(expected = DebugArgumentCodec.CodecException::class)
    fun `form rejects invalid base64`() {
        DebugArgumentCodec.encodeForm(
            mapOf("body" to "###"),
            listOf(parameter("body", "byte[]", DebugParameterKind.BYTES))
        )
    }

    @Test(expected = DebugArgumentCodec.CodecException::class)
    fun `form rejects unknown enum constant`() {
        DebugArgumentCodec.encodeForm(
            mapOf("status" to "CLOSED"),
            listOf(
                parameter(
                    "status",
                    "com.example.Status",
                    DebugParameterKind.ENUM,
                    enumConstants = listOf("OPEN", "CLOSED_BY_USER")
                )
            )
        )
    }

    @Test
    fun `form encodes list and object as raw json`() {
        val encoded = DebugArgumentCodec.encodeForm(
            mapOf(
                "ids" to "[1,2,3]",
                "query" to """{"page":1}"""
            ),
            listOf(
                parameter("ids", "java.util.List", DebugParameterKind.LIST),
                parameter("query", "com.example.Query", DebugParameterKind.OBJECT)
            )
        )
        assertEquals("list", encoded[0].asJsonObject["type"].asString)
        assertEquals(3, encoded[0].asJsonObject["value"].asJsonArray.size())
        assertEquals("object", encoded[1].asJsonObject["type"].asString)
        assertEquals("""{"page":1}""", encoded[1].asJsonObject["json"].asString)
    }

    @Test
    fun `json object encodes by parameter name`() {
        val encoded = DebugArgumentCodec.encodeJson(
            """{"name":"lucky","count":3}""",
            listOf(
                parameter("name", "java.lang.String", DebugParameterKind.STRING),
                parameter("count", "int", DebugParameterKind.INT)
            )
        )
        assertEquals("string", encoded[0].asJsonObject["type"].asString)
        assertEquals("lucky", encoded[0].asJsonObject["value"].asString)
        assertEquals("int", encoded[1].asJsonObject["type"].asString)
        assertEquals(3L, encoded[1].asJsonObject["value"].asLong)
    }

    @Test
    fun `json array encodes by position`() {
        val encoded = DebugArgumentCodec.encodeJson(
            """["lucky",7]""",
            listOf(
                parameter("name", "java.lang.String", DebugParameterKind.STRING),
                parameter("count", "int", DebugParameterKind.INT)
            )
        )
        assertEquals("lucky", encoded[0].asJsonObject["value"].asString)
        assertEquals(7L, encoded[1].asJsonObject["value"].asLong)
    }

    @Test(expected = DebugArgumentCodec.CodecException::class)
    fun `json array rejects size mismatch`() {
        DebugArgumentCodec.encodeJson(
            """["lucky"]""",
            listOf(
                parameter("name", "java.lang.String", DebugParameterKind.STRING),
                parameter("count", "int", DebugParameterKind.INT)
            )
        )
    }

    @Test(expected = DebugArgumentCodec.CodecException::class)
    fun `json object rejects missing required primitive`() {
        DebugArgumentCodec.encodeJson(
            """{"name":"lucky"}""",
            listOf(
                parameter("name", "java.lang.String", DebugParameterKind.STRING),
                parameter("count", "int", DebugParameterKind.INT)
            )
        )
    }

    @Test(expected = DebugArgumentCodec.CodecException::class)
    fun `unsupported parameter is rejected`() {
        DebugArgumentCodec.encodeForm(
            mapOf("stream" to "x"),
            listOf(
                parameter(
                    "stream",
                    "java.io.InputStream",
                    DebugParameterKind.UNSUPPORTED,
                    unsupportedReason = "P1 不支持流类型参数"
                )
            )
        )
    }

    @Test
    fun `example hint follows parameter kind`() {
        assertEquals(
            "text",
            DebugArgumentCodec.exampleOf(parameter("name", "java.lang.String", DebugParameterKind.STRING))
        )
        assertEquals(
            "true / false",
            DebugArgumentCodec.exampleOf(parameter("enabled", "boolean", DebugParameterKind.BOOLEAN))
        )
        assertEquals(
            "OPEN",
            DebugArgumentCodec.exampleOf(
                parameter(
                    "status",
                    "com.example.Status",
                    DebugParameterKind.ENUM,
                    enumConstants = listOf("OPEN", "CLOSED")
                )
            )
        )
        assertEquals(
            "{\"key\": \"value\"}",
            DebugArgumentCodec.exampleOf(parameter("query", "java.util.Map", DebugParameterKind.MAP))
        )
        assertEquals(
            "P1 不支持流类型参数",
            DebugArgumentCodec.exampleOf(
                parameter(
                    "stream",
                    "java.io.InputStream",
                    DebugParameterKind.UNSUPPORTED,
                    unsupportedReason = "P1 不支持流类型参数"
                )
            )
        )
    }

    @Test
    fun `sample json mirrors parameter names`() {
        val text = DebugArgumentCodec.sampleJson(
            listOf(
                parameter("name", "java.lang.String", DebugParameterKind.STRING),
                parameter("ids", "java.util.List", DebugParameterKind.LIST),
                parameter("query", "com.example.Query", DebugParameterKind.OBJECT),
                parameter(
                    "status",
                    "com.example.Status",
                    DebugParameterKind.ENUM,
                    enumConstants = listOf("OPEN", "CLOSED")
                )
            )
        )

        val root = JsonParser.parseString(text).asJsonObject
        assertEquals(setOf("name", "ids", "query", "status"), root.keySet())
        assertTrue(root["ids"].isJsonArray)
        assertTrue(root["query"].isJsonObject)
        assertEquals("OPEN", root["status"].asString)
    }

    private fun parameter(
        name: String,
        typeName: String,
        kind: DebugParameterKind,
        enumConstants: List<String> = emptyList(),
        unsupportedReason: String? = null
    ) = DebugParameter(
        index = 0,
        name = name,
        typeName = typeName,
        kind = kind,
        enumConstants = enumConstants,
        unsupportedReason = unsupportedReason
    )
}
