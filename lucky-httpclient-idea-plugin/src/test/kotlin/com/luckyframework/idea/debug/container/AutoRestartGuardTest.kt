package com.luckyframework.idea.debug.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoRestartGuardTest {

    @Test
    fun `first failure keeps auto restart enabled`() {
        val guard = AutoRestartGuard()

        assertTrue(guard.record(false))
        assertEquals(1, guard.consecutiveFailures)
        assertFalse(guard.suspended)
        assertTrue(guard.canAutoRestart())
    }

    @Test
    fun `second consecutive failure suspends auto restart`() {
        val guard = AutoRestartGuard()
        guard.record(false)

        assertFalse(guard.record(false))
        assertEquals(2, guard.consecutiveFailures)
        assertTrue(guard.suspended)
        assertFalse(guard.canAutoRestart())
    }

    @Test
    fun `success resets failure counter`() {
        val guard = AutoRestartGuard()
        guard.record(false)

        assertTrue(guard.record(true))
        assertEquals(0, guard.consecutiveFailures)
        assertFalse(guard.suspended)
    }

    @Test
    fun `failures count again from zero after success`() {
        val guard = AutoRestartGuard()
        guard.record(false)
        guard.record(true)

        assertTrue(guard.record(false))
        assertFalse(guard.suspended)
        assertEquals(1, guard.consecutiveFailures)
    }

    @Test
    fun `reset clears suspension and counter`() {
        val guard = AutoRestartGuard()
        guard.record(false)
        guard.record(false)
        assertTrue(guard.suspended)

        guard.reset()

        assertFalse(guard.suspended)
        assertEquals(0, guard.consecutiveFailures)
        assertTrue(guard.canAutoRestart())
    }

    @Test
    fun `custom threshold is respected`() {
        val guard = AutoRestartGuard(maxConsecutiveFailures = 3)

        assertTrue(guard.record(false))
        assertTrue(guard.record(false))
        assertFalse(guard.record(false))
        assertTrue(guard.suspended)
    }
}
