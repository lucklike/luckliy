package com.luckyframework.idea.debug.invoke

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 严格 JSON 校验：平台解析器放过的宽松写法要在这里报出来，
 * 且错误偏移要落在编辑器文本的出错字符上。
 */
class DebugJsonSyntaxTest {

    @Test
    fun `valid json with comments passes`() {
        assertNull(DebugJsonSyntax.strictIssue("{\n  // 说明\n  \"a\": 1\n}"))
        assertNull(DebugJsonSyntax.strictIssue("[]"))
        assertNull(DebugJsonSyntax.strictIssue("123"))
    }

    @Test
    fun `trailing comma is reported at the closing brace`() {
        val text = "{\n  \"city\": \"ok\",\n}"
        val issue = DebugJsonSyntax.strictIssue(text)
        assertNotNull(issue)
        // 错误指向第 3 行的 }
        assertEquals(text.length - 1, issue!!.offset)
        assertTrue(issue.message, issue.message.isNotBlank())
    }

    @Test
    fun `unterminated string is reported`() {
        assertNotNull(DebugJsonSyntax.strictIssue("{\n  \"city\": \"ceewv\n}"))
    }

    @Test
    fun `unquoted key stays accepted like the submit parser`() {
        // 提交参数用的宽松解析接受未加引号的键，编辑器不能报它未知的错误
        assertNull(DebugJsonSyntax.strictIssue("{\n  city: 1\n}"))
    }

    @Test
    fun `trailing content after the value is reported`() {
        val issue = DebugJsonSyntax.strictIssue("{\"a\": 1} extra")
        assertNotNull(issue)
        assertTrue(issue!!.offset <= "{\"a\": 1} extra".length)
    }
}
