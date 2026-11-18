package com.luckyframework.idea.spel.component

import com.luckyframework.idea.spel.LuckyFixtureTestCase
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings

/**
 * 编辑器标签页等文件级位置的组件类图标。
 */
class LuckyComponentFileIconProviderTest : LuckyFixtureTestCase() {

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

    fun testComponentFileUsesBuiltinIcon() {
        val file = componentFile("StdApi")
        assertSame(LuckyIcons.Component, iconOf(file.virtualFile))
    }

    fun testPlainJavaFileKeepsPlatformIcon() {
        val file = myFixture.addFileToProject(
            "com/example/PlainApi.java",
            """
            package com.example;

            public class PlainApi {
            }
            """.trimIndent()
        )
        assertNull(iconOf(file.virtualFile))
    }

    fun testNonJavaFileIsIgnored() {
        val file = myFixture.addFileToProject("readme.txt", "plain text")
        assertNull(iconOf(file.virtualFile))
    }

    fun testDisabledReplacementReturnsNull() {
        settings.isComponentIconEnabled = false
        val file = componentFile("DisabledApi")
        assertNull(iconOf(file.virtualFile))
    }

    private fun componentFile(name: String) = myFixture.addFileToProject(
        "com/example/$name.java",
        """
        package com.example;

        import com.luckyframework.httpclient.proxy.spel.meta.LuckyComponent;

        @LuckyComponent
        public interface $name {
        }
        """.trimIndent()
    )

    private fun iconOf(file: com.intellij.openapi.vfs.VirtualFile?) =
        LuckyComponentFileIconProvider().getIcon(file!!, 0, project)
}
