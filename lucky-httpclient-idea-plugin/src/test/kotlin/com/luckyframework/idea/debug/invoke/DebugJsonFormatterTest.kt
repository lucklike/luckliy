package com.luckyframework.idea.debug.invoke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugJsonFormatterTest {

    @Test
    fun `formats compact objects and arrays`() {
        assertEquals(
            """
            {
              "a": 1,
              "b": [
                1,
                {
                  "c": 2
                }
              ],
              "d": {}
            }
            """.trimIndent(),
            DebugJsonFormatter.format("""{"a":1,"b":[1,{"c":2}],"d":{}}""")
        )
    }

    @Test
    fun `keeps line comments on their own line`() {
        assertEquals(
            """
            {
              // 城市
              "city": "",
              "tags": [
                1,
                2
              ]
            }
            """.trimIndent(),
            DebugJsonFormatter.format(
                """
                {
                // 城市
                "city":"","tags":[1,2]
                }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `keeps block comments next to the value`() {
        assertEquals(
            """
            {
              "a": 1 /* x */,
              "b": 2
            }
            """.trimIndent(),
            DebugJsonFormatter.format("""{"a":1 /* x */,"b":2}""")
        )
    }

    @Test
    fun `does not split delimiters inside strings`() {
        val compact = """{"url":"http://a/b,c{d}","quote":"say \"hi\""}"""
        assertEquals(
            """
            {
              "url": "http://a/b,c{d}",
              "quote": "say \"hi\""
            }
            """.trimIndent(),
            DebugJsonFormatter.format(compact)
        )
    }

    @Test
    fun `generated template is already formatted`() {
        val template = DebugArgumentCodec.sampleJson(
            listOf(
                DebugParameter(
                    index = 0,
                    name = "request",
                    typeName = "demo.WeatherRequest",
                    kind = DebugParameterKind.OBJECT,
                    description = "天气查询请求",
                    fields = listOf(
                        DebugParameter(
                            index = 0,
                            name = "city",
                            typeName = "java.lang.String",
                            kind = DebugParameterKind.STRING,
                            description = "城市名"
                        ),
                        DebugParameter(
                            index = 1,
                            name = "days",
                            typeName = "java.util.List",
                            kind = DebugParameterKind.LIST,
                            element = DebugParameter(
                                index = 0,
                                name = "element",
                                typeName = "demo.WeatherDay",
                                kind = DebugParameterKind.OBJECT,
                                fields = listOf(
                                    DebugParameter(
                                        index = 0,
                                        name = "date",
                                        typeName = "java.lang.String",
                                        kind = DebugParameterKind.STRING
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        assertEquals(template, DebugJsonFormatter.format(template))
    }

    @Test
    fun `incomplete input is formatted without throwing`() {
        val formatted = DebugJsonFormatter.format("""{"a":"未闭合""")
        assertTrue(formatted.contains("\"a\""))
        assertTrue(formatted.contains("未闭合"))
    }

    @Test
    fun `blank input stays blank`() {
        assertEquals("", DebugJsonFormatter.format(""))
        assertEquals("", DebugJsonFormatter.format("   \n\t"))
    }
}
