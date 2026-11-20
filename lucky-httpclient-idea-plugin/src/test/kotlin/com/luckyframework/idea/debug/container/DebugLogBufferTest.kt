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
    fun `keeps raw lines without adding a timestamp`() {
        val buffer = DebugLogBuffer()
        val line = "2026-11-20 11:14:29.224  INFO 19356 --- [debug-container] app started"

        buffer.append(line)

        assertEquals(line, buffer.snapshot().single())
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
    fun `append notifies listener for real time ui`() {
        var notifications = 0
        val buffer = DebugLogBuffer(capacity = 2, onAppend = { notifications++ })

        buffer.append("a")
        buffer.append("b", stderr = true)

        assertEquals(2, notifications)
        assertEquals(2, buffer.snapshot().size)
    }

    @Test
    fun `tail with sequence keeps increasing after clear`() {
        val buffer = DebugLogBuffer(capacity = 2)
        buffer.append("a")
        buffer.append("b")
        buffer.append("c")

        val tail = buffer.tailWithSequence(2)
        assertEquals(2, tail.lines.size)
        assertEquals(1L, tail.startSequence)
        assertEquals(3L, tail.nextSequence)

        buffer.clear()
        buffer.append("d")

        val afterClear = buffer.tailWithSequence(1)
        assertEquals(3L, afterClear.startSequence)
        assertEquals(4L, afterClear.nextSequence)
    }

    @Test
    fun `clear removes all lines`() {
        val buffer = DebugLogBuffer()
        buffer.append("line")

        buffer.clear()

        assertTrue(buffer.snapshot().isEmpty())
    }
}
