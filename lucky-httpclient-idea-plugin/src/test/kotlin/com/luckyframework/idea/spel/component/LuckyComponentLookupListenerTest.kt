package com.luckyframework.idea.spel.component

import com.intellij.codeInsight.completion.JavaPsiClassReferenceElement
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.DefaultLookupItemRenderer
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupItem
import com.intellij.codeInsight.lookup.LookupListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.util.ui.UIUtil
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols
import java.awt.Rectangle

/**
 * 补全列表中的组件图标。
 *
 * 平台渲染器从 `PsiElement.getIcon()` 取图标，不经过 `IconProvider`；老平台
 * （没有渲染钩子）上监听器通过 `LookupItem.ICON_ATTR` 覆盖组件候选的图标。
 * 真实补全上的渲染钩子安装见 [LuckyComponentRealCompletionTest]。
 */
class LuckyComponentLookupListenerTest : LuckyFixtureTestCase() {

    private lateinit var settings: LuckyHttpClientSettings
    private var originalIconEnabled = true
    private var originalPath = ""

    override fun setUp() {
        super.setUp()
        settings = LuckyHttpClientSettings.getInstance()
        originalIconEnabled = settings.isComponentIconEnabled
        originalPath = settings.componentIconPath
        settings.isComponentIconEnabled = true
        settings.componentIconPath = ""
        ComponentIconResolver.invalidate()
    }

    override fun tearDown() {
        try {
            settings.isComponentIconEnabled = originalIconEnabled
            settings.componentIconPath = originalPath
            ComponentIconResolver.invalidate()
        } finally {
            super.tearDown()
        }
    }

    fun testComponentLookupItemGetsBrandIcon() {
        val component = componentClass("StdApi")
        val plain = plainClass("PlainApi")
        // 模拟项目启动后的图标预热，让补全链路能读到组件快照
        SpelRuntimeSymbols.components(project)

        val componentItem = JavaPsiClassReferenceElement(component)
        val plainItem = JavaPsiClassReferenceElement(plain)
        val lookup = FakeLookup(project, listOf(componentItem, plainItem))

        LuckyComponentLookupListener(project).activeLookupChanged(null, lookup)

        assertSame(LuckyIcons.Component, componentItem.getAttribute(LookupItem.ICON_ATTR))
        assertNull(plainItem.getAttribute(LookupItem.ICON_ATTR))
        // 平台渲染器确实会优先使用 ICON_ATTR
        assertSame(LuckyIcons.Component, DefaultLookupItemRenderer.getRawIcon(componentItem))
        assertTrue(lookup.refreshed)
    }

    fun testColdSnapshotKeepsPlatformIcon() {
        // 不预热组件快照：补全在 EDT 上执行，监听器不能主动触发索引扫描
        val component = componentClass("ColdApi")
        val item = JavaPsiClassReferenceElement(component)
        val lookup = FakeLookup(project, listOf(item))

        LuckyComponentLookupListener(project).activeLookupChanged(null, lookup)

        assertNull(item.getAttribute(LookupItem.ICON_ATTR))
        assertFalse(lookup.refreshed)
    }

    fun testStaleComponentSnapshotStillGetsBrandIcon() {
        val component = componentClass("StaleApi")
        // 模拟项目启动后的图标预热
        SpelRuntimeSymbols.components(project)
        // 模拟后续编辑：触发补全的按键本身就会改变 PSI 修改计数，
        // 组件清单不能因此失效
        myFixture.addFileToProject(
            "com/example/Unrelated.java",
            """
            package com.example;

            public class Unrelated {
            }
            """.trimIndent()
        )
        val item = JavaPsiClassReferenceElement(component)
        val lookup = FakeLookup(project, listOf(item))

        LuckyComponentLookupListener(project).activeLookupChanged(null, lookup)

        assertSame(LuckyIcons.Component, item.getAttribute(LookupItem.ICON_ATTR))
        assertTrue(lookup.refreshed)
    }

    fun testItemsFilledAfterLookupEventStillGetsBrandIcon() {
        val component = componentClass("LateApi")
        SpelRuntimeSymbols.components(project)
        val item = JavaPsiClassReferenceElement(component)
        val lookup = FakeLookup(project, emptyList())

        // 模拟 LookupManagerListener 早于候选填充触发：此时列表为空
        LuckyComponentLookupListener(project).activeLookupChanged(null, lookup)
        assertNull(item.getAttribute(LookupItem.ICON_ATTR))

        lookup.elements.add(item)
        UIUtil.dispatchAllInvocationEvents()

        assertSame(LuckyIcons.Component, item.getAttribute(LookupItem.ICON_ATTR))
        assertTrue(lookup.refreshed)
    }

    fun testRepeatedInjectionDoesNotRefreshAgain() {
        val component = componentClass("RepeatApi")
        SpelRuntimeSymbols.components(project)
        val item = JavaPsiClassReferenceElement(component)
        val lookup = FakeLookup(project, listOf(item))
        val listener = LuckyComponentLookupListener(project)

        listener.activeLookupChanged(null, lookup)
        assertTrue(lookup.refreshed)

        // 继续输入会重复触发注入：已设置过图标的候选不应再次重建展示数据
        lookup.refreshed = false
        listener.activeLookupChanged(null, lookup)
        assertFalse(lookup.refreshed)
    }

    fun testDisabledReplacementKeepsPlatformIcon() {
        val component = componentClass("DisabledApi")
        SpelRuntimeSymbols.components(project)
        settings.isComponentIconEnabled = false
        val item = JavaPsiClassReferenceElement(component)
        val lookup = FakeLookup(project, listOf(item))

        LuckyComponentLookupListener(project).activeLookupChanged(null, lookup)

        assertNull(item.getAttribute(LookupItem.ICON_ATTR))
        assertFalse(lookup.refreshed)
    }

    private fun componentClass(name: String): PsiClass = (myFixture.addFileToProject(
        "com/example/$name.java",
        """
        package com.example;

        import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

        @LuckyComponent
        public interface $name {
        }
        """.trimIndent()
    ) as PsiJavaFile).classes.single()

    private fun plainClass(name: String): PsiClass = (myFixture.addFileToProject(
        "com/example/$name.java",
        """
        package com.example;

        public class $name {
        }
        """.trimIndent()
    ) as PsiJavaFile).classes.single()

    private class FakeLookup(
        private val project: Project,
        elements: List<LookupElement>
    ) : Lookup {

        /** 可变，用于模拟候选晚于 lookup 事件填充。 */
        val elements: MutableList<LookupElement> = elements.toMutableList()

        var refreshed = false

        @Suppress("unused")
        fun refreshUi(mayCheckReused: Boolean, onExplicitAction: Boolean) {
            refreshed = true
        }

        override fun getLookupStart(): Int = 0

        override fun getCurrentItem(): LookupElement? = null

        override fun addLookupListener(listener: LookupListener) {}

        override fun removeLookupListener(listener: LookupListener) {}

        override fun getBounds(): Rectangle = Rectangle()

        override fun getCurrentItemBounds(): Rectangle = Rectangle()

        override fun isPositionedAboveCaret(): Boolean = false

        override fun getPsiElement(): PsiElement? = null

        override fun getEditor(): Editor = throw UnsupportedOperationException()

        override fun getTopLevelEditor(): Editor = throw UnsupportedOperationException()

        override fun getProject(): Project = project

        override fun getPsiFile(): PsiFile? = null

        override fun isCompletion(): Boolean = true

        override fun getItems(): List<LookupElement> = elements

        override fun isFocused(): Boolean = false

        override fun itemPattern(item: LookupElement): String = ""

        override fun itemMatcher(item: LookupElement): PrefixMatcher = PrefixMatcher.ALWAYS_TRUE

        override fun isSelectionTouched(): Boolean = false

        override fun getAdvertisements(): List<String> = emptyList()
    }
}
