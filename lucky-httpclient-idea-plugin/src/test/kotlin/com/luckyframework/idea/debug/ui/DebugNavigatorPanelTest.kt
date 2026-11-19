package com.luckyframework.idea.debug.ui

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.luckyframework.idea.debug.container.DebugContainerService
import com.luckyframework.idea.debug.invoke.DebugParameter
import com.luckyframework.idea.debug.invoke.DebugParameterKind
import com.luckyframework.idea.debug.invoke.DebugTarget
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings

class DebugNavigatorPanelTest : BasePlatformTestCase() {

    private var originalSettings: LuckyHttpClientSettings.State? = null

    override fun setUp() {
        super.setUp()
        val settings = LuckyHttpClientSettings.getInstance()
        originalSettings = settings.getState().copy()
        settings.loadState(LuckyHttpClientSettings.State())
    }

    override fun tearDown() {
        try {
            originalSettings?.let { LuckyHttpClientSettings.getInstance().loadState(it) }
        } finally {
            super.tearDown()
        }
    }

    fun `test builds one input row per parameter`() {
        val panel = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            parameter("count", "int", DebugParameterKind.INT)
        )
        try {
            assertEquals(2, panel.inputRowCountForTest())
            assertTrue(panel.executeEnabledForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test unsupported parameter disables execute`() {
        val panel = panel(
            parameter(
                "stream",
                "java.io.InputStream",
                DebugParameterKind.UNSUPPORTED,
                unsupportedReason = "P1 不支持流类型参数"
            )
        )
        try {
            assertEquals(1, panel.inputRowCountForTest())
            assertFalse(panel.executeEnabledForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test web server is disabled by default`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertFalse(panel.isWebServerEnabledForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test web server checkbox reflects persisted setting`() {
        LuckyHttpClientSettings.getInstance().isDebugWebServerEnabled = true
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertTrue(panel.isWebServerEnabledForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json mode toggles without losing parameter rows`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            panel.switchJsonModeForTest(true)
            assertTrue(panel.isJsonModeForTest())
            assertEquals(1, panel.inputRowCountForTest())

            panel.switchJsonModeForTest(false)
            assertFalse(panel.isJsonModeForTest())
            assertEquals(1, panel.inputRowCountForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test log area appends new lines incrementally`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            panel.renderLogsForTest(listOf("a", "b"))
            assertEquals("a\nb", panel.logTextForTest())

            panel.renderLogsForTest(listOf("a", "b", "c"))
            assertEquals("a\nb\nc", panel.logTextForTest())

            // 容器清空日志或环形缓冲滚动后重新回到全量渲染
            panel.renderLogsForTest(emptyList())
            assertEquals("", panel.logTextForTest())
            panel.renderLogsForTest(listOf("x"))
            assertEquals("x", panel.logTextForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test auto scroll is enabled by default and can be toggled`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertTrue(panel.autoScrollEnabledForTest())

            panel.toggleAutoScrollForTest()
            assertFalse(panel.autoScrollEnabledForTest())

            panel.toggleAutoScrollForTest()
            assertTrue(panel.autoScrollEnabledForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test copy text strips ansi escapes`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            panel.renderLogsForTest(listOf("\u001B[1;31mred\u001B[0m", "plain"))

            assertEquals("red\nplain", panel.copyableLogsForTest())
            assertTrue(panel.copyLogsEnabledForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test clear logs only clears the view`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            panel.renderLogsForTest(listOf("a", "b"))
            assertEquals("a\nb", panel.logTextForTest())
            assertTrue(panel.copyLogsEnabledForTest())

            panel.clearLogsForTest()
            assertEquals("", panel.logTextForTest())
            assertFalse(panel.copyLogsEnabledForTest())

            // 清空后新的日志仍能继续显示
            panel.renderLogsForTest(listOf("c"))
            assertEquals("c", panel.logTextForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test log view defaults to single line and supports wrap toggle`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertFalse(panel.logWrapEnabledForTest())
            assertFalse(panel.logWrapLinesForTest())
            assertFalse(panel.logTracksViewportWidthForTest())

            panel.toggleLogWrapForTest()
            assertTrue(panel.logWrapEnabledForTest())
            assertTrue(panel.logWrapLinesForTest())
            assertTrue(panel.logTracksViewportWidthForTest())

            panel.toggleLogWrapForTest()
            assertFalse(panel.logWrapLinesForTest())
            assertFalse(panel.logTracksViewportWidthForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test start button label follows container state`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertEquals("Start Container", panel.startButtonTextForTest())
            assertTrue(panel.startButtonEnabledForTest())
            assertFalse(panel.stopButtonEnabledForTest())

            panel.applyStateForTest(DebugContainerService.State.STARTING)
            assertEquals("Starting…", panel.startButtonTextForTest())
            assertFalse(panel.startButtonEnabledForTest())
            assertFalse(panel.stopButtonEnabledForTest())

            panel.applyStateForTest(DebugContainerService.State.READY)
            assertEquals("Restart Container", panel.startButtonTextForTest())
            assertTrue(panel.startButtonEnabledForTest())
            assertTrue(panel.stopButtonEnabledForTest())

            panel.applyStateForTest(DebugContainerService.State.FAILED)
            assertEquals("Start Container", panel.startButtonTextForTest())
            assertTrue(panel.startButtonEnabledForTest())
            assertFalse(panel.stopButtonEnabledForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    private fun panel(vararg parameters: DebugParameter): DebugNavigatorPanel {
        val module = ModuleManager.getInstance(project).modules.first()
        val target = DebugTarget(
            moduleName = module.name,
            className = "demo.Api",
            methodName = "get",
            parameters = parameters.toList(),
            signature = "get(${parameters.joinToString { it.typeName }})"
        )
        return DebugNavigatorPanel(project, target)
    }

    private fun parameter(
        name: String,
        typeName: String,
        kind: DebugParameterKind,
        unsupportedReason: String? = null
    ): DebugParameter = DebugParameter(
        index = 0,
        name = name,
        typeName = typeName,
        kind = kind,
        unsupportedReason = unsupportedReason
    )
}
