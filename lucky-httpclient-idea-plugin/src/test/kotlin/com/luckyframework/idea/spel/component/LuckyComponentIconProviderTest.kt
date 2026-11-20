package com.luckyframework.idea.spel.component

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import com.luckyframework.idea.spel.symbol.SpelRuntimeSymbols

/**
 * PSI 级图标（项目树、结构视图、查找类）。
 *
 * 图标解析可能发生在没有读锁的绘制路径上，实现只能用组件清单里的简单名
 * 匹配，不能调用 `PsiClass.getQualifiedName()`（会沿 stub 链解析父级）。
 */
class LuckyComponentIconProviderTest : LuckyFixtureTestCase() {

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

    fun testComponentClassUsesBuiltinIcon() {
        val component = componentClass("TreeApi")
        SpelRuntimeSymbols.components(project)
        assertSame(LuckyIcons.Component, iconOf(component))
    }

    fun testPlainClassKeepsPlatformIcon() {
        val plain = plainClass("TreePlainApi")
        SpelRuntimeSymbols.components(project)
        assertNull(iconOf(plain))
    }

    fun testColdSnapshotKeepsPlatformIcon() {
        // 不预热组件清单：图标解析保持平台默认
        assertNull(iconOf(componentClass("TreeColdApi")))
    }

    fun testDisabledSettingReturnsNull() {
        settings.isComponentIconEnabled = false
        val component = componentClass("TreeDisabledApi")
        SpelRuntimeSymbols.components(project)
        assertNull(iconOf(component))
    }

    private fun iconOf(psiClass: PsiClass) = LuckyComponentIconProvider().getIcon(psiClass, 0)

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
