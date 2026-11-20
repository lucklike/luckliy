package com.luckyframework.idea.navigator

import com.luckyframework.idea.debug.container.DebugContainerService.State
import org.junit.Assert.assertEquals
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
}
