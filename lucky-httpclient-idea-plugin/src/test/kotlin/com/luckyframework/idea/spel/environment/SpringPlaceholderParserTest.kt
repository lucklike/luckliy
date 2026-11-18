package com.luckyframework.idea.spel.environment

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * `${key:default}` 纯文本解析：嵌套、默认值与未闭合场景。
 */
class SpringPlaceholderParserTest : BasePlatformTestCase() {

    fun testParsesKeysAndDefaultValues() {
        val placeholders = SpringPlaceholderParser.parse(
            "http://\${api.host}:\${api.port:8080}/x"
        )

        assertEquals(2, placeholders.size)
        val host = placeholders[0]
        assertEquals("api.host", host.key)
        assertEquals("\${api.host}", "http://\${api.host}:\${api.port:8080}/x".substring(host.startOffset, host.endOffset))
        assertTrue(host.closed)
        assertNull(host.defaultText)

        val port = placeholders[1]
        assertEquals("api.port", port.key)
        assertEquals("8080", port.defaultText)
        assertTrue(port.closed)
        assertEquals(
            "8080",
            "http://\${api.host}:\${api.port:8080}/x".substring(port.defaultStartOffset!!, port.defaultEndOffset!!)
        )
    }

    /** `${a:${b}}` 只产生外层占位符，默认值内部保留嵌套文本。 */
    fun testNestedPlaceholderBelongsToDefaultText() {
        val placeholder = SpringPlaceholderParser.parse("\${a:\${b}}").single()

        assertEquals("a", placeholder.key)
        assertEquals("\${b}", placeholder.defaultText)
        assertTrue(placeholder.closed)
        assertEquals(9, placeholder.endOffset)
    }

    fun testNestedDefaultWithOwnDefault() {
        val placeholder = SpringPlaceholderParser.parse("\${a:\${b:default}}").single()

        assertEquals("a", placeholder.key)
        assertEquals("\${b:default}", placeholder.defaultText)
        assertEquals(17, placeholder.endOffset)
    }

    fun testUnclosedPlaceholderKeepsRemainingText() {
        val placeholder = SpringPlaceholderParser.parse("http://\${api.host").single()

        assertEquals("api.host", placeholder.key)
        assertFalse(placeholder.closed)
        assertEquals("http://\${api.host".length, placeholder.endOffset)
    }

    fun testFindEndSkipsNestedBraces() {
        assertEquals(9, SpringPlaceholderParser.findEnd("\${a:\${b}}", 2))
        assertEquals(-1, SpringPlaceholderParser.findEnd("\${a:\${b}", 2))
    }

    fun testPlaceholderAtKeyOnly() {
        val text = "\${api.host:localhost}"

        assertEquals("api.host", SpringPlaceholderParser.placeholderAt(text, 5)?.key)
        assertNull(SpringPlaceholderParser.placeholderAt(text, text.indexOf(":localhost") + 3))
        assertNull(SpringPlaceholderParser.placeholderAt(text, text.length))
    }

    fun testPlaceholderContainingIncludesDefault() {
        val text = "\${api.host:localhost}"
        val offset = text.indexOf("localhost") + 2

        assertEquals("api.host", SpringPlaceholderParser.placeholderContaining(text, offset)?.key)
        assertNull(SpringPlaceholderParser.placeholderContaining("plain text", 2))
    }
}
