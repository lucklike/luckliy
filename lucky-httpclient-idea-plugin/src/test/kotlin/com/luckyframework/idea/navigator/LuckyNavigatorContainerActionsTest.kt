package com.luckyframework.idea.navigator

import com.luckyframework.idea.debug.container.DebugContainerService.State
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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
    fun `power button toggles container by state`() {
        assertEquals(ContainerAction.START, containerPowerAction(null))
        assertEquals(ContainerAction.START, containerPowerAction(State.STOPPED))
        assertEquals(ContainerAction.STOP, containerPowerAction(State.STARTING))
        assertEquals(ContainerAction.STOP, containerPowerAction(State.RESTARTING))
        assertEquals(ContainerAction.STOP, containerPowerAction(State.READY))
        assertEquals(ContainerAction.STOP, containerPowerAction(State.FAILED))
        assertEquals(ContainerAction.STOP, containerPowerAction(State.SUSPENDED))
        assertNull(containerPowerAction(State.STOPPING))
    }

    @Test
    fun `power context menu follows container state`() {
        assertEquals(listOf(ContainerAction.START), containerContextMenuActions(null))
        assertEquals(listOf(ContainerAction.START), containerContextMenuActions(State.STOPPED))
        assertEquals(
            listOf(ContainerAction.LOG, ContainerAction.START),
            containerContextMenuActions(State.FAILED)
        )
        assertEquals(
            listOf(ContainerAction.LOG, ContainerAction.START),
            containerContextMenuActions(State.SUSPENDED)
        )
        assertEquals(
            listOf(ContainerAction.STOP, ContainerAction.LOG),
            containerContextMenuActions(State.STARTING)
        )
        assertEquals(
            listOf(ContainerAction.STOP, ContainerAction.LOG),
            containerContextMenuActions(State.RESTARTING)
        )
        assertEquals(
            listOf(ContainerAction.STOP, ContainerAction.LOG, ContainerAction.RESTART),
            containerContextMenuActions(State.READY)
        )
        assertEquals(listOf(ContainerAction.LOG), containerContextMenuActions(State.STOPPING))
    }

    @Test
    fun `power color reflects container state`() {
        assertEquals(containerStateColor(State.READY), containerPowerColor(State.READY))
        assertEquals(containerStateColor(State.FAILED), containerPowerColor(State.FAILED))
        assertEquals(containerStateColor(State.STARTING), containerPowerColor(State.STARTING))
        assertNotEquals(containerPowerColor(State.READY), containerPowerColor(null))
        assertEquals(containerPowerColor(null), containerPowerColor(State.STOPPED))
    }
}
