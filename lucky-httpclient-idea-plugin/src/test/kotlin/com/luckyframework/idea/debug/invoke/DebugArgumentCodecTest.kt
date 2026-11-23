package com.luckyframework.idea.debug.invoke

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        val root = JsonParser.parseString(DebugArgumentCodec.stripComments(text)).asJsonObject
        assertEquals(setOf("name", "ids", "query", "status"), root.keySet())
        assertTrue(root["ids"].isJsonArray)
        assertTrue(root["query"].isJsonObject)
        assertEquals("OPEN", root["status"].asString)
    }

    @Test
    fun `sample json expands nested fields`() {
        val text = DebugArgumentCodec.sampleJson(
            listOf(
                parameter(
                    "request",
                    "com.example.WeatherRequest",
                    DebugParameterKind.OBJECT,
                    description = "天气查询请求",
                    fields = listOf(
                        parameter("city", "java.lang.String", DebugParameterKind.STRING, description = "城市名"),
                        parameter(
                            "location",
                            "com.example.Location",
                            DebugParameterKind.OBJECT,
                            fields = listOf(parameter("lat", "double", DebugParameterKind.DOUBLE))
                        ),
                        parameter(
                            "days",
                            "java.util.List",
                            DebugParameterKind.LIST,
                            element = parameter(
                                "element",
                                "com.example.Day",
                                DebugParameterKind.OBJECT,
                                fields = listOf(
                                    parameter("date", "java.lang.String", DebugParameterKind.STRING)
                                )
                            )
                        )
                    )
                )
            )
        )

        // 模板保持纯 JSON：字段类型与说明由悬停文档提供，不再写入注释
        assertFalse(text.contains("//"))
        assertTrue(text.contains("\"city\": \"\""))

        // 骨架本身就是可执行 JSON
        val request = JsonParser.parseString(text)
            .asJsonObject["request"].asJsonObject
        assertEquals("", request["city"].asString)
        assertEquals(0.0, request["location"].asJsonObject["lat"].asDouble, 0.0)
        assertEquals(1, request["days"].asJsonArray.size())
        assertEquals("", request["days"].asJsonArray[0].asJsonObject["date"].asString)
    }

    @Test
    fun `json mode accepts the commented template`() {
        val parameters = listOf(
            parameter("name", "java.lang.String", DebugParameterKind.STRING),
            parameter("count", "int", DebugParameterKind.INT)
        )

        val encoded = DebugArgumentCodec.encodeJson(DebugArgumentCodec.sampleJson(parameters), parameters)

        assertEquals("", encoded[0].asJsonObject["value"].asString)
        assertEquals(0L, encoded[1].asJsonObject["value"].asLong)
    }

    @Test
    fun `strip comments keeps slashes inside strings`() {
        val text = """
            {
              // 行注释
              "url": "http://example.com/a",
              "path": "c:/temp/*keep*/" /* 块注释 */
            }
        """.trimIndent()

        val stripped = DebugArgumentCodec.stripComments(text)
        assertFalse(stripped.contains("行注释"))
        assertFalse(stripped.contains("块注释"))

        val root = JsonParser.parseString(stripped).asJsonObject
        assertEquals("http://example.com/a", root["url"].asString)
        assertEquals("c:/temp/*keep*/", root["path"].asString)
    }

    @Test
    fun `form template fills object list skeleton`() {
        val days = parameter(
            "days",
            "java.util.List",
            DebugParameterKind.LIST,
            element = parameter(
                "element",
                "com.example.Day",
                DebugParameterKind.OBJECT,
                fields = listOf(
                    parameter("date", "java.lang.String", DebugParameterKind.STRING, description = "日期"),
                    parameter("count", "int", DebugParameterKind.INT)
                )
            )
        )

        val text = DebugArgumentCodec.formTemplateOf(days)
        assertTrue(text.contains("// String · 日期"))

        val array = JsonParser.parseString(DebugArgumentCodec.stripComments(text)).asJsonArray
        assertEquals(1, array.size())
        assertEquals("", array[0].asJsonObject["date"].asString)
        assertEquals(0, array[0].asJsonObject["count"].asInt)
    }

    @Test
    fun `form template keeps containers empty without element structure`() {
        assertEquals(
            "[]",
            DebugArgumentCodec.formTemplateOf(parameter("ids", "java.util.List", DebugParameterKind.LIST))
        )
        assertEquals(
            "{}",
            DebugArgumentCodec.formTemplateOf(parameter("query", "com.example.Query", DebugParameterKind.OBJECT))
        )
    }

    @Test
    fun `field element converts form text for object assembly`() {
        assertEquals(
            "Shanghai",
            DebugArgumentCodec.fieldElement(
                parameter("city", "java.lang.String", DebugParameterKind.STRING),
                "Shanghai"
            ).asString
        )
        assertEquals(
            42,
            DebugArgumentCodec.fieldElement(parameter("count", "int", DebugParameterKind.INT), "42").asInt
        )
        assertEquals(
            31.2,
            DebugArgumentCodec.fieldElement(parameter("lat", "double", DebugParameterKind.DOUBLE), "31.2").asDouble,
            0.0
        )
        assertTrue(
            DebugArgumentCodec.fieldElement(
                parameter("enabled", "boolean", DebugParameterKind.BOOLEAN),
                "true"
            ).asBoolean
        )
        // long 在协议里是字符串，组装 JSON 时必须还原成数字
        val total = DebugArgumentCodec.fieldElement(
            parameter("total", "long", DebugParameterKind.LONG),
            "9007199254740993"
        )
        assertTrue(total.asJsonPrimitive.isNumber)
        assertEquals(9007199254740993L, total.asLong)
        assertEquals(
            2,
            DebugArgumentCodec.fieldElement(
                parameter("ids", "java.util.List", DebugParameterKind.LIST),
                "[1,2]"
            ).asJsonArray.size()
        )
    }

    private fun parameter(
        name: String,
        typeName: String,
        kind: DebugParameterKind,
        enumConstants: List<String> = emptyList(),
        unsupportedReason: String? = null,
        description: String? = null,
        fields: List<DebugParameter> = emptyList(),
        element: DebugParameter? = null
    ) = DebugParameter(
        index = 0,
        name = name,
        typeName = typeName,
        kind = kind,
        enumConstants = enumConstants,
        unsupportedReason = unsupportedReason,
        description = description,
        fields = fields,
        element = element
    )
}
