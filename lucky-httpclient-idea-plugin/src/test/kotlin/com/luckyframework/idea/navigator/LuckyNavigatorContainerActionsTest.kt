package com.luckyframework.idea.navigator

import com.luckyframework.idea.debug.container.DebugContainerService.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LuckyNavigatorContainerActionsTest {

    @Test
    fun `stopped project only offers start`() {
        assertEquals(listOf(ContainerAction.START), containerActionsFor(null))
        assertEquals(listOf(ContainerAction.START), containerActionsFor(State.STOPPED))
    }

    @Test
    fun `failed project offers start and logs`() {
        assertEquals(
            listOf(ContainerAction.START, ContainerAction.LOG),
            containerActionsFor(State.FAILED)
        )
        assertEquals(
            listOf(ContainerAction.START, ContainerAction.LOG),
            containerActionsFor(State.SUSPENDED)
        )
    }

    @Test
    fun `ready project offers restart and stop`() {
        assertEquals(
            listOf(ContainerAction.RESTART, ContainerAction.STOP),
            containerActionsFor(State.READY)
        )
    }

    @Test
    fun `transitioning project hides container actions`() {
        assertTrue(containerActionsFor(State.STARTING).isEmpty())
        assertTrue(containerActionsFor(State.RESTARTING).isEmpty())
        assertTrue(containerActionsFor(State.STOPPING).isEmpty())
    }

    @Test
    fun `container bar only enables start when container is not running`() {
        assertTrue(containerStartEnabled(null))
        assertTrue(containerStartEnabled(State.STOPPED))
        assertTrue(containerStartEnabled(State.FAILED))
        assertTrue(containerStartEnabled(State.SUSPENDED))
        assertFalse(containerStartEnabled(State.STARTING))
        assertFalse(containerStartEnabled(State.READY))
        assertFalse(containerStartEnabled(State.STOPPING))
    }

    @Test
    fun `container bar enables restart for running and failed containers`() {
        assertTrue(containerRestartEnabled(State.READY))
        assertTrue(containerRestartEnabled(State.FAILED))
        assertTrue(containerRestartEnabled(State.SUSPENDED))
        assertFalse(containerRestartEnabled(null))
        assertFalse(containerRestartEnabled(State.STOPPED))
        assertFalse(containerRestartEnabled(State.STARTING))
    }

    @Test
    fun `container bar only enables stop while running`() {
        assertTrue(containerStopEnabled(State.READY))
        assertFalse(containerStopEnabled(null))
        assertFalse(containerStopEnabled(State.STOPPED))
        assertFalse(containerStopEnabled(State.FAILED))
        assertFalse(containerStopEnabled(State.STOPPING))
    }

    @Test
    fun `container state labels are human readable`() {
        assertEquals("Stopped", containerStateLabel(null))
        assertEquals("Stopped", containerStateLabel(State.STOPPED))
        assertEquals("Starting...", containerStateLabel(State.STARTING))
        assertEquals("Running", containerStateLabel(State.READY))
        assertEquals("Failed", containerStateLabel(State.FAILED))
        assertEquals("Stopping...", containerStateLabel(State.STOPPING))
        assertEquals("Restarting...", containerStateLabel(State.RESTARTING))
        assertEquals("Suspended", containerStateLabel(State.SUSPENDED))
    }
}
