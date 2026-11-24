package com.luckyframework.idea.spel.component

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import java.lang.reflect.Modifier
import javax.swing.Icon

/**
 * 标签页图标刷新依赖平台实现上的 `refreshIcons()` /
 * `updateFileIconImmediately$intellij_platform_ide_impl` 入口（都不在公开接口上），
 * 这里用最小替身固定反射约定，平台升级改名时能立即暴露。
 */
class EditorTabIconRefresherTest : BasePlatformTestCase() {

    fun `test refresh invokes platform entry`() {
        val manager = IconRefreshTestManager(restored = true)

        assertTrue(invokeRefreshIcons(manager))
        assertTrue(manager.refreshed)
    }

    fun `test refresh reports missing entry`() {
        assertFalse(invokeRefreshIcons(Any()))
    }

    fun `test write icons through platform entry`() {
        val file = javaFile("DemoApi")

        val splitters = FakeSplitters()
        assertEquals(1, applyToSplitters(listOf(splitters), listOf(file), LuckyIcons.Component))
        assertEquals(listOf(file), splitters.files)
        assertSame(LuckyIcons.Component, splitters.icons.single())
    }

    fun `test write icons into every splitter`() {
        val file = javaFile("SplitApi")
        val first = FakeSplitters()
        val second = FakeSplitters()

        // 同一文件写入多个分栏只计一次
        assertEquals(1, applyToSplitters(listOf(first, second), listOf(file), LuckyIcons.Component))
        assertEquals(listOf(file), first.files)
        assertEquals(listOf(file), second.files)
    }

    fun `test write reports unsupported platform entry`() {
        assertEquals(-1, applyToSplitters(listOf(Any()), listOf(javaFile("PlainApi")), LuckyIcons.Component))
    }

    fun `test write skips empty input`() {
        assertEquals(0, applyToSplitters(emptyList(), listOf(javaFile("NoSplitterApi")), LuckyIcons.Component))
        assertEquals(0, applyToSplitters(listOf(FakeSplitters()), emptyList(), LuckyIcons.Component))
        assertEquals(0, applyToSplitters(listOf(FakeSplitters()), listOf(javaFile("NoIconApi")), null))
    }

    fun `test keep ticking until tabs are restored`() {
        assertTrue(shouldContinueTabTicks(tick = 20, lastTabCount = 0, openTabCount = 0, minTicks = 6, maxTicks = 60))
        assertTrue(shouldContinueTabTicks(tick = 20, lastTabCount = 3, openTabCount = 5, minTicks = 6, maxTicks = 60))
    }

    fun `test keep ticking through the minimum window`() {
        assertTrue(shouldContinueTabTicks(tick = 1, lastTabCount = 3, openTabCount = 3, minTicks = 6, maxTicks = 60))
    }

    fun `test stop ticking once tabs are stable`() {
        assertFalse(shouldContinueTabTicks(tick = 8, lastTabCount = 3, openTabCount = 3, minTicks = 6, maxTicks = 60))
    }

    fun `test stop ticking at the upper bound`() {
        assertFalse(shouldContinueTabTicks(tick = 60, lastTabCount = 0, openTabCount = 0, minTicks = 6, maxTicks = 60))
    }

    fun `test prefer all splitters entry`() {
        val splitter = FakeSplitters()

        assertEquals(listOf(splitter), splittersOf(SplittersManager(setOf(splitter))))
    }

    fun `test fall back to editor window owner`() {
        val owner = FakeSplitters()

        assertEquals(listOf(owner), splittersOf(WindowedManager(arrayOf(WindowOwner(owner)))))
    }

    fun `test no splitters entry reports empty`() {
        assertTrue(splittersOf(Any()).isEmpty())
    }

    /** 平台入口必须在编译基线上保持 public 且签名不变，反射直写才成立。 */
    fun `test platform write entry stays public`() {
        val type = Class.forName("com.intellij.openapi.fileEditor.impl.EditorsSplitters")
        val method = type.getMethod(APPLY_ICON_METHOD, VirtualFile::class.java, Icon::class.java)

        assertTrue("platform entry must stay public", Modifier.isPublic(method.modifiers))
    }

    /**
     * 真实 `FileEditorManager` 验证：headless 测试替身 `TestEditorManagerImpl`
     * 不提供分栏与刷新入口，刷新只能安全地空转，不能抛异常。
     */
    fun `test refresh tolerates manager without splitters`() {
        val settings = LuckyHttpClientSettings.getInstance()
        val originalEnabled = settings.isComponentIconEnabled
        val originalPath = settings.componentIconPath
        settings.isComponentIconEnabled = true
        settings.componentIconPath = ""
        ComponentIconResolver.invalidate()
        try {
            val file = myFixture.addFileToProject(
                "com/example/TabbedApi.java",
                """
                package com.example;

                import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

                @LuckyComponent
                public interface TabbedApi {
                }
                """.trimIndent()
            ).virtualFile
            // 文件图标与标签页刷新共用预热后的组件清单
            SpelRuntimeSymbols.components(project)
            myFixture.openFileInEditor(file)

            val manager = FileEditorManager.getInstance(project)
            assertTrue("headless manager has no splitters", splittersOf(manager).isEmpty())
            assertEquals(0, EditorTabIconRefresher.refresh(project, manager))
        } finally {
            settings.isComponentIconEnabled = originalEnabled
            settings.componentIconPath = originalPath
            ComponentIconResolver.invalidate()
        }
    }

    private fun javaFile(name: String) = myFixture.addFileToProject(
        "com/example/$name.java",
        """
        package com.example;

        public interface $name {
        }
        """.trimIndent()
    ).virtualFile
}

private class IconRefreshTestManager(restored: Boolean) {
    private val initJob = IconRefreshTestJob(restored)
    var refreshed = false
        private set

    fun refreshIcons() {
        refreshed = true
    }
}

private class IconRefreshTestJob(private val completed: Boolean) {
    fun isCompleted(): Boolean = completed
}

/** 替身：平台 `getAllSplitters()` 入口。 */
private class SplittersManager(private val splitters: Set<Any>) {
    fun getAllSplitters(): Set<Any> = splitters
}

/** 替身：`getWindows()` 回退路径。 */
private class WindowedManager(private val windows: Array<Any>) {
    fun getWindows(): Array<Any> = windows
}

/** 替身：`EditorWindow.getOwner()` 返回分栏容器。 */
private class WindowOwner(private val owner: Any) {
    fun getOwner(): Any = owner
}
