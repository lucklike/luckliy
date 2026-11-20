package com.luckyframework.idea.debug.ui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
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
            assertEquals("a\nb", panel.logTextForTest().normalizedNewlines())

            panel.renderLogsForTest(listOf("a", "b", "c"))
            assertEquals("a\nb\nc", panel.logTextForTest().normalizedNewlines())

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
            assertEquals("a\nb", panel.logTextForTest().normalizedNewlines())
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

    fun `test log scrollbar is on the left and actions on the right`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertTrue(panel.logScrollBarLeftSideForTest())
            assertTrue(panel.logActionsOnRightForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test log actions update on the EDT`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            // 平台 2026.x 拒绝默认的 OLD_EDT，工具栏动作必须显式选择更新线程
            assertEquals(
                List(4) { ActionUpdateThread.EDT },
                panel.logActionUpdateThreadsForTest()
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test more menu hosts argument switches`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertEquals(listOf("JSON arguments", "Web server"), panel.moreOptionsForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test header falls back to fully qualified target without request path`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertEquals("demo.Api#get", panel.requestUrlForTest())
            assertNull(panel.requestMethodForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test header shows method badge and joined request path`() {
        val panel = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            httpMethod = "GET",
            baseUrl = "http://localhost:8080",
            requestPath = "/users"
        )
        try {
            // 调试窗口头部与导航栏一致：方法徽标 + baseUrl/接口 path 拼接后的完整路径
            assertEquals("GET", panel.requestMethodForTest())
            assertEquals("http://localhost:8080/users", panel.requestUrlForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test header keeps a single slash between base url and path`() {
        val panel = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            baseUrl = "http://localhost:8080/",
            requestPath = "/users"
        )
        try {
            assertEquals("http://localhost:8080/users", panel.requestUrlForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test header shows full url without base url`() {
        val panel = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            baseUrl = "",
            requestPath = "https://api.example.com/v1/users"
        )
        try {
            assertEquals("https://api.example.com/v1/users", panel.requestUrlForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test target menu is hidden without sibling methods`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertFalse(panel.targetMenuVisibleForTest())
            assertEquals(listOf("Api#get(String)"), panel.targetLabelsForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test target menu lists sibling methods with parameter types`() {
        val module = ModuleManager.getInstance(project).modules.first()
        val sibling = DebugTarget(
            moduleName = module.name,
            className = "demo.Api",
            methodName = "post",
            parameters = listOf(parameter("body", "demo.Body", DebugParameterKind.OBJECT)),
            signature = "post(demo.Body)"
        )
        val panel = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            siblings = listOf(sibling)
        )
        try {
            assertTrue(panel.targetMenuVisibleForTest())
            assertEquals(
                listOf("Api#get(String)", "Api#post(Body)"),
                panel.targetLabelsForTest()
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    private fun panel(
        vararg parameters: DebugParameter,
        siblings: List<DebugTarget> = emptyList(),
        httpMethod: String? = null,
        baseUrl: String? = null,
        requestPath: String? = null
    ): DebugNavigatorPanel {
        val module = ModuleManager.getInstance(project).modules.first()
        val target = DebugTarget(
            moduleName = module.name,
            className = "demo.Api",
            methodName = "get",
            parameters = parameters.toList(),
            signature = "get(${parameters.joinToString { it.typeName }})",
            httpMethod = httpMethod,
            baseUrl = baseUrl,
            requestPath = requestPath
        )
        return DebugNavigatorPanel(project, target, siblings = listOf(target) + siblings)
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

    /**
     * Windows 的 JTextPane 会把插入的 `\n` 规范成 `\r\n`；
     * 日志断言只比较逻辑行，避免平台差异导致误报。
     */
    private fun String.normalizedNewlines(): String = replace("\r\n", "\n")
}
