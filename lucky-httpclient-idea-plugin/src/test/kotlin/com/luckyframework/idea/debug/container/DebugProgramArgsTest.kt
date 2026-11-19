package com.luckyframework.idea.debug.container

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 调试容器启动参数：默认不启动 Web 容器，开关打开后使用随机端口。 */
class DebugProgramArgsTest {

    @Test
    fun `default arguments disable the web application type`() {
        assertEquals(
            listOf("--spring.main.web-application-type=none"),
            DebugContainerService.DEFAULT_PROGRAM_ARGS
        )
        assertFalse(DebugContainerService.DEFAULT_PROGRAM_ARGS.any { it.startsWith("--server.port") })
    }

    @Test
    fun `web server arguments use a random port`() {
        val args = DebugContainerService.programArgsFor(webServer = true)
        assertEquals(listOf("--server.port=0"), args)
        assertFalse(args.any { it.startsWith("--spring.main.web-application-type") })
    }

    @Test
    fun `non-web arguments do not start a server`() {
        val args = DebugContainerService.programArgsFor(webServer = false)
        assertTrue(args.contains("--spring.main.web-application-type=none"))
        assertFalse(args.any { it.startsWith("--server.port") })
    }
}
