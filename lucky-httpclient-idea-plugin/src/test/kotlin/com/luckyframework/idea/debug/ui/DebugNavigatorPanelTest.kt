package com.luckyframework.idea.debug.ui

import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBSplitter
import com.luckyframework.idea.debug.invoke.DebugCallOutcome
import com.luckyframework.idea.debug.invoke.DebugJsonDocumentationTargetProvider
import com.luckyframework.idea.debug.invoke.DebugParameter
import com.luckyframework.idea.debug.invoke.DebugParameterKind
import com.luckyframework.idea.debug.invoke.DebugTarget
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.awt.BorderLayout
import java.io.File
import java.util.Base64

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

    fun `test two argument rows fill the arguments area`() {
        val panel = panel(
            parameter("origin", "java.lang.String", DebugParameterKind.STRING),
            parameter("destination", "java.lang.String", DebugParameterKind.STRING)
        )
        try {
            layoutPanel(panel)
            val contentHeight = panel.parameterContentHeightForTest()
            val chrome = panel.argumentsChromeHeightForTest()
            val argumentsHeight = panel.argumentsHeightForTest()
            // 两行参数时参数区高度贴合内容，不按比例预留空白
            assertTrue(
                "arguments area should hug two rows: $argumentsHeight, content $contentHeight, chrome $chrome",
                argumentsHeight >= contentHeight + chrome - 4 && argumentsHeight <= contentHeight + chrome + 6
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test more than three argument rows reveal only three rows`() {
        val panel = panel(
            parameter("a", "java.lang.String", DebugParameterKind.STRING),
            parameter("b", "java.lang.String", DebugParameterKind.STRING),
            parameter("c", "java.lang.String", DebugParameterKind.STRING),
            parameter("d", "java.lang.String", DebugParameterKind.STRING)
        )
        try {
            val splitter = layoutPanel(panel)
            val contentHeight = panel.parameterContentHeightForTest()
            val chrome = panel.argumentsChromeHeightForTest()
            val argumentsHeight = panel.argumentsHeightForTest()
            val state = "rows=${panel.parameterRowCountForTest()}, splitter=${splitter.height}, " +
                "proportion=${splitter.proportion}, content=$contentHeight, chrome=$chrome, " +
                "arguments=$argumentsHeight, heights=${panel.parameterRowHeightsForTest()}"
            // 四行参数时只露出三行，第四行隐藏到滚动区
            assertTrue(
                "arguments area should hide the fourth row ($state)",
                argumentsHeight < contentHeight + chrome - 8
            )
            assertTrue(
                "arguments area should keep about three rows ($state)",
                argumentsHeight > (contentHeight + chrome) / 2
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test arguments use the editor font and show an Args tab`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            val scheme = EditorColorsManager.getInstance().globalScheme
            // 参数名、输入控件与 Args 标题都跟随 Result/Logs 的编辑器字体
            assertEquals(scheme.editorFontName, panel.parameterFontFamilyForTest())
            assertEquals(scheme.editorFontName, panel.argsTabFontFamilyForTest())
            assertTrue("Args tab should sit above the argument cards", panel.argsTabAboveArgumentsForTest())
            assertEquals(listOf("token"), panel.groupLabelsForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test object parameter flattens into property paths`() {
        val panel = panel(requestParameter())
        try {
            // 参数标题只显示参数名，字段缩进且不带参数前缀
            assertEquals(listOf("request"), panel.groupLabelsForTest())
            assertEquals(2, panel.inputRowCountForTest())
            assertEquals(
                listOf("city", "location.lat"),
                panel.inputPathsForTest()
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test multiple parameters keep the parameter prefix`() {
        val panel = panel(
            requestParameter(),
            parameter("token", "java.lang.String", DebugParameterKind.STRING)
        )
        try {
            assertEquals(
                listOf("request", "token"),
                panel.groupLabelsForTest()
            )
            // 多参数的内部路径保留前缀，避免同名字段互相覆盖；字段行仍只显示相对名
            assertEquals(
                listOf("request.city", "request.location.lat", "token"),
                panel.inputPathsForTest()
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test object group can collapse and expand field rows`() {
        val panel = panel(requestParameter())
        try {
            assertFalse(panel.groupCollapsedForTest(0))
            assertTrue(panel.groupFieldRowsVisibleForTest(0))

            panel.setGroupCollapsedForTest(0, true)
            assertTrue(panel.groupCollapsedForTest(0))
            assertFalse(panel.groupFieldRowsVisibleForTest(0))

            panel.setGroupCollapsedForTest(0, false)
            assertFalse(panel.groupCollapsedForTest(0))
            assertTrue(panel.groupFieldRowsVisibleForTest(0))
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test form values assemble nested object json`() {
        val panel = panel(requestParameter())
        try {
            panel.setInputValueForTest("city", "Shanghai")
            panel.setInputValueForTest("location.lat", "31.2")

            assertEquals(
                """{"city":"Shanghai","location":{"lat":31.2}}""",
                panel.formValuesForTest()["request"]
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test empty object fields are omitted from form json`() {
        val panel = panel(requestParameter())
        try {
            panel.setInputValueForTest("city", "Shanghai")

            assertEquals("""{"city":"Shanghai"}""", panel.formValuesForTest()["request"])
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test field descriptions are shown in the form`() {
        val panel = panel(requestParameter())
        try {
            assertEquals(mapOf("city" to "城市名"), panel.inputDescriptionsForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test file and bytes parameters offer local file pickers`() {
        val panel = panel(
            parameter("path", "java.nio.file.Path", DebugParameterKind.FILE),
            parameter("content", "byte[]", DebugParameterKind.BYTES),
            parameter("token", "java.lang.String", DebugParameterKind.STRING)
        )
        try {
            assertEquals(listOf("path", "content"), panel.filePickerPathsForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test applying local file fills path or base64`() {
        val file = File.createTempFile("lucky-debug", ".bin")
        try {
            file.writeText("lucky")
            val panel = panel(
                parameter("path", "java.nio.file.Path", DebugParameterKind.FILE),
                parameter("content", "byte[]", DebugParameterKind.BYTES)
            )
            try {
                panel.applyFileForTest("path", file, base64 = false)
                panel.applyFileForTest("content", file, base64 = true)

                val values = panel.formValuesForTest()
                assertEquals(file.absolutePath, values["path"])
                assertEquals(
                    Base64.getEncoder().encodeToString("lucky".toByteArray()),
                    values["content"]
                )
            } finally {
                Disposer.dispose(panel)
            }
        } finally {
            file.delete()
        }
    }

    fun `test json mode template carries argument structure`() {
        val panel = panel(requestParameter())
        try {
            // 模板保持纯 JSON：字段说明改由悬停文档展示，不再写注释
            val template = panel.jsonTemplateForTest()
            assertFalse(template.contains("//"))

            val root = JsonParser.parseString(template).asJsonObject
            assertTrue(root["request"].asJsonObject["location"].isJsonObject)
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json key hover provides field documentation`() {
        val panel = panel(requestParameter())
        try {
            val file = panel.jsonPsiFileForTest()
            val template = panel.jsonTemplateForTest()
            val offset = template.indexOf("\"city\"") + 2
            var targets: List<DocumentationTarget> = emptyList()
            ApplicationManager.getApplication().runReadAction(
                Computable {
                    targets = DebugJsonDocumentationTargetProvider().documentationTargets(file, offset)
                }
            )
            assertEquals(1, targets.size)
            val hint = targets[0].computeDocumentationHint()
            assertNotNull(hint)
            assertTrue(hint!!, hint.contains("String"))
            assertTrue(hint, hint.contains("城市名"))
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json mode inserts selected file path or content`() {
        val file = File.createTempFile("lucky-debug", ".json")
        try {
            file.writeText("lucky")
            val base64 = Base64.getEncoder().encodeToString("lucky".toByteArray())
            val panel = panel(parameter("content", "byte[]", DebugParameterKind.BYTES))
            try {
                panel.insertJsonFileForTest(file, base64 = false)
                assertEquals(JsonPrimitive(file.absolutePath).toString(), panel.jsonTemplateForTest())

                panel.insertJsonFileForTest(file, base64 = true)
                assertEquals(JsonPrimitive(base64).toString(), panel.jsonTemplateForTest())
            } finally {
                Disposer.dispose(panel)
            }
        } finally {
            file.delete()
        }
    }

    fun `test json mode formats compact text and keeps comments`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            panel.setJsonTextForTest("""{"a":1,"tags":[1,2]} // 行尾注释""")
            panel.formatJsonForTest()

            assertEquals(
                """
                {
                  "a": 1,
                  "tags": [
                    1,
                    2
                  ]
                }
                // 行尾注释
                """.trimIndent(),
                panel.jsonTemplateForTest()
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test execute button spins while busy`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertFalse(panel.executeSpinningForTest())

            panel.setBusyForTest(true)
            assertTrue(panel.executeSpinningForTest())

            panel.setBusyForTest(false)
            assertFalse(panel.executeSpinningForTest())
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

    fun `test simple parameters default to the form`() {
        val panel = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            parameter("count", "int", DebugParameterKind.INT)
        )
        try {
            assertFalse("simple parameters should open in form mode", panel.isJsonModeForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test object parameter defaults to json mode`() {
        val panel = panel(requestParameter())
        try {
            assertTrue("object parameter should open in JSON mode", panel.isJsonModeForTest())
            // JSON 模板仍按参数骨架生成，首次打开就能直接填值
            assertTrue(panel.jsonTemplateForTest().contains("\"city\""))
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test collection parameter defaults to json mode`() {
        val panel = panel(parameter("query", "java.util.Map", DebugParameterKind.MAP))
        try {
            assertTrue("map parameter should open in JSON mode", panel.isJsonModeForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json arguments use the editor font and single line by default`() {
        val panel = panel(requestParameter())
        try {
            // 手动释放句柄让 getEditor(true) 真正创建编辑器
            panel.jsonEditorForTest().setDisposedWith(panel)
            var font: String? = null
            var wrap = true
            ApplicationManager.getApplication().invokeAndWait {
                font = panel.jsonEditorFontFamilyForTest()
                wrap = panel.jsonSoftWrapEnabledForTest()
            }
            // 与 Logs/Result 同一编辑器字体；默认单行，长行横向滚动
            assertEquals(EditorColorsManager.getInstance().globalScheme.editorFontName, font)
            assertFalse("JSON arguments should default to single line", wrap)
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json toolbar uses icons like the result view`() {
        val panel = panel(requestParameter())
        try {
            val actions = panel.jsonToolbarActionsForTest()
            assertEquals(
                listOf(
                    "Format JSON",
                    "Show Field Comments",
                    "Collapse All",
                    "Expand All",
                    "Wrap Lines",
                    "Copy JSON",
                    "Insert File Path",
                    "Insert File as Base64"
                ),
                actions.map { it.templatePresentation.text }
            )
            assertTrue(
                "JSON toolbar actions should render as icons",
                actions.all { it.templatePresentation.icon != null }
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json comments toggle shows field documents`() {
        val panel = panel(requestParameter())
        try {
            val field = panel.jsonEditorForTest()
            field.setDisposedWith(panel)
            ApplicationManager.getApplication().invokeAndWait {
                // 默认关闭：模板保持纯 JSON，不显示任何字段注释
                assertFalse(panel.jsonCommentsEnabledForTest())
                assertEquals(0, panel.jsonCommentInlayCountForTest())

                panel.setJsonCommentsForTest(true)
                assertTrue(panel.jsonCommentsEnabledForTest())
                // request 与 request.city 有文档；location 及其 lat 没有文档
                assertEquals(2, panel.jsonCommentInlayCountForTest())

                panel.setJsonCommentsForTest(false)
                assertEquals(0, panel.jsonCommentInlayCountForTest())
            }
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test result comments toggle shows return type documents`() {
        val panel = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            returnType = parameter(
                "result",
                "demo.PageResult",
                DebugParameterKind.OBJECT,
                fields = listOf(
                    parameter("total", "long", DebugParameterKind.LONG, description = "总数"),
                    parameter(
                        "items",
                        "java.util.List",
                        DebugParameterKind.LIST,
                        description = "条目列表",
                        element = parameter(
                            "element",
                            "demo.Item",
                            DebugParameterKind.OBJECT,
                            fields = listOf(
                                parameter(
                                    "name",
                                    "java.lang.String",
                                    DebugParameterKind.STRING,
                                    description = "名称"
                                )
                            )
                        )
                    )
                )
            )
        )
        try {
            ApplicationManager.getApplication().invokeAndWait {
                panel.renderOutcomeForTest(
                    DebugCallOutcome.Success(
                        returnType = "demo.PageResult",
                        kind = "json",
                        body = """
                            {
                              "total": 1,
                              "items": [
                                {
                                  "name": "a"
                                }
                              ]
                            }
                        """.trimIndent(),
                        elapsedMs = 3
                    )
                )
            }
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            ApplicationManager.getApplication().invokeAndWait {
                val view = panel.resultViewForTest()
                view.ensureEditorForTest()
                assertFalse("comments should default to off", view.commentsEnabledForTest())
                assertEquals(0, view.commentInlayCountForTest())

                view.setCommentsForTest(true)
                assertTrue(view.commentsEnabledForTest())
                // total / items / items.name 都有文档
                assertEquals(3, view.commentInlayCountForTest())

                view.setCommentsForTest(false)
                assertEquals(0, view.commentInlayCountForTest())
            }
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json toolbar sits vertically on the right of the json editor`() {
        val panel = panel(requestParameter())
        try {
            assertTrue("JSON toolbar should sit on the editor's right edge", panel.jsonToolbarOnRightForTest())
            assertTrue("JSON mode should show the toolbar", panel.jsonToolbarVisibleForTest())

            panel.switchJsonModeForTest(false)
            assertFalse("form mode should hide the JSON toolbar", panel.jsonToolbarVisibleForTest())

            panel.switchJsonModeForTest(true)
            assertTrue("switching back to JSON should show the toolbar", panel.jsonToolbarVisibleForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test result toolbar sits vertically on the right of the body`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertTrue(
                "result toolbar should sit on the editor's right edge",
                panel.resultViewForTest().toolbarOnRightForTest()
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json wrap toggle switches soft wraps`() {
        val panel = panel(requestParameter())
        try {
            panel.jsonEditorForTest().setDisposedWith(panel)
            ApplicationManager.getApplication().invokeAndWait {
                panel.setJsonSoftWrapForTest(true)
            }
            var wrap = false
            ApplicationManager.getApplication().invokeAndWait {
                wrap = panel.jsonSoftWrapEnabledForTest()
            }
            assertTrue(wrap)
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test result view defaults to single line`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertFalse("result view should default to single line", panel.resultViewForTest().wrapLinesEnabled)
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json mode builds folding regions from psi`() {
        val panel = panel(
            parameter(
                "request",
                "demo.WeatherRequest",
                DebugParameterKind.OBJECT,
                fields = listOf(
                    parameter("city", "java.lang.String", DebugParameterKind.STRING),
                    parameter("lat", "double", DebugParameterKind.DOUBLE)
                )
            )
        )
        try {
            // 纯文本 document 没有 PsiFile，JSON folding builder 不会产生任何区域
            assertTrue("JSON 编辑器应基于 PSI 文件", panel.jsonFoldRegionCountForTest() > 0)
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json editor installs fold regions`() {
        val panel = panel(requestParameter())
        try {
            val field = panel.jsonEditorForTest()
            field.setDisposedWith(panel)
            var regions = 0
            var outline = false
            var lineNumbers = false
            ApplicationManager.getApplication().invokeAndWait {
                // 编辑器创建时折叠区域就装进折叠模型；折叠栏可见才会画箭头
                val editor = field.getEditor(true)
                regions = panel.jsonEditorFoldRegionCountForTest()
                outline = editor?.settings?.isFoldingOutlineShown ?: false
                lineNumbers = editor?.settings?.isLineNumbersShown ?: false
            }
            assertTrue("folding outline should be visible", outline)
            assertTrue("line numbers should be visible", lineNumbers)
            assertTrue("fold regions=$regions", regions > 0)

            // 文本变化后重建：多行嵌套对象应重新得到折叠区域
            ApplicationManager.getApplication().invokeAndWait {
                panel.setJsonTextForTest("{\n  \"a\": {\n    \"b\": 1\n  }\n}")
            }
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            var rebuilt = 0
            ApplicationManager.getApplication().invokeAndWait {
                rebuilt = panel.jsonEditorFoldRegionCountForTest()
            }
            assertTrue("rebuilt fold regions=$rebuilt", rebuilt > 0)
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test json syntax errors are highlighted`() {
        val panel = panel(requestParameter())
        try {
            val field = panel.jsonEditorForTest()
            field.setDisposedWith(panel)
            var valid = -1
            ApplicationManager.getApplication().invokeAndWait {
                valid = panel.jsonSyntaxErrorCountForTest()
            }
            assertEquals("valid template should have no syntax errors", 0, valid)

            // 缺值：平台解析器产出 PsiErrorElement
            assertJsonErrorHighlighted(panel, "{\n  \"city\": \n}", "missing value should be highlighted")

            // 尾逗号：平台解析器不报错，严格 JSON 校验兜底
            assertJsonErrorHighlighted(panel, "{\n  \"city\": \"ok\",\n}", "trailing comma should be highlighted")

            // 修好之后错误高亮随重建移除
            ApplicationManager.getApplication().invokeAndWait {
                panel.setJsonTextForTest("{\n  \"city\": \"ok\"\n}")
            }
            PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
            var fixed = -1
            ApplicationManager.getApplication().invokeAndWait {
                fixed = panel.jsonSyntaxErrorCountForTest()
            }
            assertEquals(0, fixed)
        } finally {
            Disposer.dispose(panel)
        }
    }

    /** 替换 JSON 文本后断言出现语法错误高亮。 */
    private fun assertJsonErrorHighlighted(panel: DebugNavigatorPanel, text: String, message: String) {
        ApplicationManager.getApplication().invokeAndWait {
            panel.setJsonTextForTest(text)
        }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        var broken = 0
        ApplicationManager.getApplication().invokeAndWait {
            broken = panel.jsonSyntaxErrorCountForTest()
        }
        assertTrue("$message, got $broken", broken > 0)
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

    fun `test log view uses the editor scheme like the run console`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            val scheme = EditorColorsManager.getInstance().globalScheme
            assertEquals(scheme.defaultBackground, panel.logBackgroundForTest())
            assertEquals(scheme.editorFontName, panel.logFontFamilyForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test result view uses the editor font and shows line numbers`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            ApplicationManager.getApplication().invokeAndWait {
                panel.renderOutcomeForTest(
                    DebugCallOutcome.Success(
                        returnType = "demo.Result",
                        kind = "json",
                        body = """
                            {
                              "a": {
                                "b": 1
                              }
                            }
                        """.trimIndent(),
                        elapsedMs = 12
                    )
                )
            }
            val scheme = EditorColorsManager.getInstance().globalScheme
            ApplicationManager.getApplication().invokeAndWait {
                // 结果正文与 Logs 用同一套编辑器字体
                assertEquals(scheme.editorFontName, panel.resultFontFamilyForTest())
                assertTrue("line numbers should be visible", panel.resultLineNumbersShownForTest())
                assertTrue("json body should use the json editor", panel.resultJsonModeForTest())
            }
            ApplicationManager.getApplication().invokeAndWait {
                assertTrue("json result should install fold regions", panel.resultFoldRegionCountForTest() > 0)
            }
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test text result stays on the plain editor`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            ApplicationManager.getApplication().invokeAndWait {
                panel.renderOutcomeForTest(
                    DebugCallOutcome.Success(
                        returnType = "java.lang.String",
                        kind = "text",
                        body = "hello",
                        elapsedMs = 5
                    )
                )
            }
            ApplicationManager.getApplication().invokeAndWait {
                assertFalse("text body should not use the json editor", panel.resultJsonModeForTest())
                assertEquals("hello", panel.resultTextForTest())
            }
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test log scrollbar follows console and actions sit on the right`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            // 平台控制台自带右侧滚动条，日志区不再做左右镜像
            assertTrue(panel.logScrollBarRightSideForTest())
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
            assertEquals(
                listOf("Show method path", "JSON arguments", "Web server"),
                panel.moreOptionsForTest()
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test header switches between method and url dimensions`() {
        val panel = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            httpMethod = "GET",
            baseUrl = "http://localhost:8080",
            requestPath = "/users"
        )
        try {
            // 默认方法维度：方法图标 + 完整方法路径（类名.方法名(简单参数类型)）
            assertTrue(panel.headerShowsMethodPathForTest())
            assertFalse(panel.headerUsesHttpMethodIconForTest())
            assertEquals("demo.Api.get(String)", panel.headerTextForTest())
            assertEquals("GET", panel.requestMethodForTest())

            // URL 维度：HTTP 方法徽标 + 完整地址
            panel.switchHeaderMethodPathForTest(false)
            assertFalse(panel.headerShowsMethodPathForTest())
            assertTrue(panel.headerUsesHttpMethodIconForTest())
            assertEquals("http://localhost:8080/users", panel.headerTextForTest())

            panel.switchHeaderMethodPathForTest(true)
            assertTrue(panel.headerShowsMethodPathForTest())
            assertFalse(panel.headerUsesHttpMethodIconForTest())
            assertEquals("demo.Api.get(String)", panel.headerTextForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test header method dimension falls back to method path without request path`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertTrue(panel.headerShowsMethodPathForTest())
            assertEquals("demo.Api.get(String)", panel.headerTextForTest())
            assertFalse(panel.headerUsesHttpMethodIconForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test title row shows class name and hosts the close button`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            assertEquals("Api", panel.headerClassNameForTest())
            assertTrue(panel.closeButtonInTitleRowForTest())
            assertFalse(panel.addressBarHasCloseButtonForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test header dimension choice is persisted across panels`() {
        val first = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            baseUrl = "http://localhost:8080",
            requestPath = "/users"
        )
        first.switchHeaderMethodPathForTest(false)
        Disposer.dispose(first)

        val second = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            baseUrl = "http://localhost:8080",
            requestPath = "/users"
        )
        try {
            assertFalse(second.headerShowsMethodPathForTest())
            assertEquals("http://localhost:8080/users", second.headerTextForTest())
        } finally {
            Disposer.dispose(second)
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

    fun `test target menu stays at the left edge without sibling methods`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            // 单方法组件也保留三角入口，且固定贴在输入框最左端
            assertTrue(panel.targetMenuVisibleForTest())
            assertTrue(panel.targetMenuAtLeftEdgeForTest())
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
            assertTrue(panel.targetMenuAtLeftEdgeForTest())
            assertEquals(
                listOf("Api#get(String)", "Api#post(Body)"),
                panel.targetLabelsForTest()
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test logs are scoped to the panel's own execution windows`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            // 没有执行过：即使容器里有日志也不展示，避免多个标签页共用同一份日志
            assertTrue(panel.visibleLogsForTest(listOf("a", "b", "c", "d"), 0, 4).isEmpty())

            // 第一次执行窗口 [1,3)：只拼接这一段的日志
            panel.addLogWindowForTest(1, 3)
            assertEquals(
                listOf("b", "c"),
                panel.visibleLogsForTest(listOf("a", "b", "c", "d"), 0, 4)
            )

            // 第二次执行仍在进行中（end 为负）：展示到当前末尾
            panel.addLogWindowForTest(2, -1)
            assertEquals(
                listOf("b", "c", "c", "d"),
                panel.visibleLogsForTest(listOf("a", "b", "c", "d"), 0, 4)
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test new execution drops previous log windows`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            panel.addLogWindowForTest(1, 3)
            panel.addLogWindowForTest(2, -1)
            assertEquals(2, panel.logWindowCountForTest())

            // 点击执行：上一次请求的日志窗口不再保留，只展示本次请求
            panel.startLogWindowForTest()
            assertEquals(1, panel.logWindowCountForTest())
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test log window skips lines scrolled out of the ring buffer`() {
        val panel = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        try {
            // 窗口起点早于当前缓冲：已被环形缓冲挤掉的旧行直接跳过
            panel.addLogWindowForTest(0, 10)
            assertEquals(
                listOf("c", "d"),
                panel.visibleLogsForTest(listOf("c", "d"), 2, 4)
            )
        } finally {
            Disposer.dispose(panel)
        }
    }

    fun `test embedded panel hides its own title row and exposes tab title`() {
        val standalone = panel(parameter("token", "java.lang.String", DebugParameterKind.STRING))
        val embedded = panel(
            parameter("token", "java.lang.String", DebugParameterKind.STRING),
            showTitleRow = false
        )
        try {
            // 标签页模式下标题与关闭由标签头承担，面板内部不再重复一行；标签标题只保留方法名
            assertTrue(standalone.titleRowVisibleForTest())
            assertFalse(embedded.titleRowVisibleForTest())
            assertEquals("get", embedded.tabTitleForTest())
        } finally {
            Disposer.dispose(standalone)
            Disposer.dispose(embedded)
        }
    }

    /**
     * 模拟首次显示：给根面板尺寸后手动布局。
     *
     * 不创建真实窗口，直接调用 doLayout，避免测试依赖窗口系统；
     * 分割条的默认高度校准就发生在它的 doLayout 里。
     */
    private fun layoutPanel(panel: DebugNavigatorPanel): JBSplitter {
        val root = panel.component
        root.setSize(800, 900)
        root.doLayout()
        val splitter = (root.layout as BorderLayout)
            .getLayoutComponent(BorderLayout.CENTER) as JBSplitter
        splitter.doLayout()
        return splitter
    }

    private fun panel(
        vararg parameters: DebugParameter,
        returnType: DebugParameter? = null,
        siblings: List<DebugTarget> = emptyList(),
        httpMethod: String? = null,
        baseUrl: String? = null,
        requestPath: String? = null,
        showTitleRow: Boolean = true
    ): DebugNavigatorPanel {
        val module = ModuleManager.getInstance(project).modules.first()
        val target = DebugTarget(
            moduleName = module.name,
            className = "demo.Api",
            methodName = "get",
            parameters = parameters.mapIndexed { index, parameter -> parameter.copy(index = index) },
            signature = "get(${parameters.joinToString { it.typeName }})",
            httpMethod = httpMethod,
            baseUrl = baseUrl,
            requestPath = requestPath,
            returnType = returnType
        )
        return DebugNavigatorPanel(project, target, siblings = listOf(target) + siblings, showTitleRow = showTitleRow)
    }

    private fun requestParameter(): DebugParameter = parameter(
        "request",
        "demo.WeatherRequest",
        DebugParameterKind.OBJECT,
        description = "天气查询请求",
        fields = listOf(
            parameter("city", "java.lang.String", DebugParameterKind.STRING, description = "城市名"),
            parameter(
                "location",
                "demo.Location",
                DebugParameterKind.OBJECT,
                fields = listOf(parameter("lat", "double", DebugParameterKind.DOUBLE))
            )
        )
    )

    private fun parameter(
        name: String,
        typeName: String,
        kind: DebugParameterKind,
        unsupportedReason: String? = null,
        description: String? = null,
        fields: List<DebugParameter> = emptyList(),
        element: DebugParameter? = null
    ): DebugParameter = DebugParameter(
        index = 0,
        name = name,
        typeName = typeName,
        kind = kind,
        unsupportedReason = unsupportedReason,
        description = description,
        fields = fields,
        element = element
    )

    /**
     * Windows 的 JTextPane 会把插入的 `\n` 规范成 `\r\n`；
     * 日志断言只比较逻辑行，避免平台差异导致误报。
     */
    private fun String.normalizedNewlines(): String = replace("\r\n", "\n")
}
