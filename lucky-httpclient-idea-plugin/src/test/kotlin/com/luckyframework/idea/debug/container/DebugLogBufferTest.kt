package com.luckyframework.idea.debug.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugLogBufferTest {

    @Test
    fun `keeps only latest lines up to capacity`() {
        val buffer = DebugLogBuffer(capacity = 3)

        (1..5).forEach { buffer.append("line$it") }

        val lines = buffer.snapshot()
        assertEquals(3, lines.size)
        assertTrue(lines.first().endsWith("line3"))
        assertTrue(lines.last().endsWith("line5"))
    }

    @Test
    fun `stderr lines are marked`() {
        val buffer = DebugLogBuffer()

        buffer.append("boom", stderr = true)

        assertTrue(buffer.snapshot().single().contains("[err] boom"))
    }

    @Test
    fun `tail returns last lines only`() {
        val buffer = DebugLogBuffer()
        (1..10).forEach { buffer.append("line$it") }

        val tail = buffer.tail(3)

        assertEquals(3, tail.size)
        assertTrue(tail.first().endsWith("line8"))
        assertTrue(tail.last().endsWith("line10"))
    }

    @Test
    fun `tail with zero returns empty`() {
        val buffer = DebugLogBuffer()
        buffer.append("line")

        assertTrue(buffer.tail(0).isEmpty())
    }

    @Test
    fun `clear removes all lines`() {
        val buffer = DebugLogBuffer()
        buffer.append("line")

        buffer.clear()

        assertTrue(buffer.snapshot().isEmpty())
    }
}
