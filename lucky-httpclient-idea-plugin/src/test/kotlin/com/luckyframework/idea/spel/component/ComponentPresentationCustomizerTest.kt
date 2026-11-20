package com.luckyframework.idea.spel.component

import com.intellij.codeInsight.completion.JavaPsiClassReferenceElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.icons.AllIcons
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols

/**
 * 渲染期图标定制。
 *
 * 2026.3 的补全渲染器缓存 presentation 前会调用 `freeze()`，冻结后的对象
 * 拒绝写入；定制器必须返回复制出来的新对象。这里的用例覆盖两种情况：
 * 普通 presentation 与已冻结 presentation。
 */
class ComponentPresentationCustomizerTest : LuckyFixtureTestCase() {

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

    fun testComponentPresentationGetsBrandIcon() {
        val component = componentClass("PresentApi")
        SpelRuntimeSymbols.components(project)
        val item = JavaPsiClassReferenceElement(component)
        val source = platformPresentation(item)

        val customized = ComponentPresentationCustomizer(project)
            .customizePresentation(item, source)

        assertSame(LuckyIcons.Component, customized.icon)
        assertNotSame("渲染器缓存的对象不能被替换", source, customized)
        assertSame("原始展示数据保持平台图标", AllIcons.Nodes.Class, source.icon)
    }

    fun testFrozenPresentationStillGetsBrandIcon() {
        val component = componentClass("FrozenApi")
        SpelRuntimeSymbols.components(project)
        val item = JavaPsiClassReferenceElement(component)
        val source = platformPresentation(item)
        // 2026.3 的 AsyncRendering 在缓存前冻结展示数据
        source.freeze()

        val customized = ComponentPresentationCustomizer(project)
            .customizePresentation(item, source)

        assertSame(LuckyIcons.Component, customized.icon)
        assertSame(AllIcons.Nodes.Class, source.icon)
    }

    fun testPlainClassKeepsPlatformIcon() {
        val plain = plainClass("PlainPresentApi")
        SpelRuntimeSymbols.components(project)
        val item = JavaPsiClassReferenceElement(plain)
        val source = platformPresentation(item)

        val customized = ComponentPresentationCustomizer(project)
            .customizePresentation(item, source)

        assertSame("非组件应原样返回，避免每格重建展示数据", source, customized)
        assertSame(AllIcons.Nodes.Class, customized.icon)
    }

    fun testDecoratedComponentStillGetsBrandIcon() {
        val component = componentClass("DecoratedApi")
        SpelRuntimeSymbols.components(project)
        val item = JavaPsiClassReferenceElement(component)
        val wrapped = object : LookupElementDecorator<LookupElement>(item) {}
        val source = platformPresentation(wrapped)

        val customized = ComponentPresentationCustomizer(project)
            .customizePresentation(wrapped, source)

        assertSame(LuckyIcons.Component, customized.icon)
    }

    fun testColdSnapshotKeepsPlatformIcon() {
        val component = componentClass("ColdPresentApi")
        // 不预热组件清单：渲染时保持平台默认图标
        val item = JavaPsiClassReferenceElement(component)
        val source = platformPresentation(item)

        val customized = ComponentPresentationCustomizer(project)
            .customizePresentation(item, source)

        assertSame(source, customized)
        assertSame(AllIcons.Nodes.Class, customized.icon)
    }

    fun testDisabledSettingKeepsPlatformIcon() {
        settings.isComponentIconEnabled = false
        ComponentIconResolver.invalidate()
        val component = componentClass("DisabledPresentApi")
        SpelRuntimeSymbols.components(project)
        val item = JavaPsiClassReferenceElement(component)
        val source = platformPresentation(item)

        val customized = ComponentPresentationCustomizer(project)
            .customizePresentation(item, source)

        assertSame(source, customized)
        assertSame(AllIcons.Nodes.Class, customized.icon)
    }

    /**
     * 渲染期只能使用补全元素构造时缓存的名称。
     *
     * 2026.3 的绘制路径没有读锁，访问 `PsiClass.getQualifiedName()` 会抛
     * `Read access is allowed from inside read-action only`。删除文件后 PSI
     * 失效，任何 PSI 读取都会抛异常；这里仍能命中组件，说明判定链路
     * 不依赖 PSI（名称来自 `JavaPsiClassReferenceElement.myQualifiedName`）。
     */
    fun testCachedNameStillMatchesAfterPsiDisposed() {
        val component = componentClass("DisposedApi")
        SpelRuntimeSymbols.components(project)
        val item = JavaPsiClassReferenceElement(component)
        val source = platformPresentation(item)
        val file = component.containingFile
        WriteCommandAction.runWriteCommandAction(project) { file.delete() }

        val customized = ComponentPresentationCustomizer(project)
            .customizePresentation(item, source)

        assertSame(LuckyIcons.Component, customized.icon)
    }

    /** 模拟渲染器的输入：平台默认图标（需要冻结的用例自行调用 `freeze()`）。 */
    private fun platformPresentation(item: LookupElement): LookupElementPresentation =
        LookupElementPresentation().apply {
            itemText = item.lookupString
            icon = AllIcons.Nodes.Class
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
}
