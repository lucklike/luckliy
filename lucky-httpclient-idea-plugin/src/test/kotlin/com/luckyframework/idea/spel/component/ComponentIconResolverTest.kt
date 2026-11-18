package com.luckyframework.idea.spel.component

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.swing.Icon

/**
 * 组件类图标解析：开关、自定义文件与内置图标回退。
 */
class ComponentIconResolverTest : BasePlatformTestCase() {

    private lateinit var settings: LuckyHttpClientSettings
    private var originalEnabled = true
    private var originalIconEnabled = true
    private var originalPath = ""

    override fun setUp() {
        super.setUp()
        settings = LuckyHttpClientSettings.getInstance()
        originalEnabled = settings.isEnabled
        originalIconEnabled = settings.isComponentIconEnabled
        originalPath = settings.componentIconPath
        settings.isComponentIconEnabled = true
        settings.componentIconPath = ""
        ComponentIconResolver.invalidate()
    }

    override fun tearDown() {
        try {
            settings.isEnabled = originalEnabled
            settings.isComponentIconEnabled = originalIconEnabled
            settings.componentIconPath = originalPath
            ComponentIconResolver.invalidate()
        } finally {
            super.tearDown()
        }
    }

    fun testDisabledUsesPlatformDefaultIcon() {
        settings.isComponentIconEnabled = false
        assertNull(ComponentIconResolver.icon())
    }

    fun testEmptyPathUsesBuiltinIcon() {
        assertSame(LuckyIcons.Component, ComponentIconResolver.icon())
    }

    fun testMissingFileFallsBackToBuiltinIcon() {
        settings.componentIconPath = newTempFile("missing.svg").absolutePath
        assertSame(LuckyIcons.Component, ComponentIconResolver.icon())
    }

    fun testCustomPngIsNormalizedTo16() {
        val file = newTempFile("custom.png")
        ImageIO.write(BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png", file)
        settings.componentIconPath = file.absolutePath

        val icon = ComponentIconResolver.icon()
        assertNotNull(icon)
        assertNotSame(LuckyIcons.Component, icon)
        assertFitsRowIcon(icon!!)
    }

    fun testNonSquarePngUsesSquareCanvas() {
        val file = newTempFile("wide.png")
        ImageIO.write(BufferedImage(64, 16, BufferedImage.TYPE_INT_ARGB), "png", file)
        settings.componentIconPath = file.absolutePath

        val icon = ComponentIconResolver.icon()
        assertNotNull(icon)
        assertFitsRowIcon(icon!!)
    }

    fun testSmallPngIsScaledToRowSize() {
        val file = newTempFile("small.png")
        ImageIO.write(BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), "png", file)
        settings.componentIconPath = file.absolutePath

        val icon = ComponentIconResolver.icon()
        assertNotNull(icon)
        assertFitsRowIcon(icon!!)
    }

    fun testCustomSvgIsNormalizedTo16() {
        val file = newTempFile("custom.svg")
        file.writeText(
            "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"200\" height=\"200\" viewBox=\"0 0 200 200\">" +
                "<rect width=\"200\" height=\"200\" fill=\"#02BE4D\"/></svg>"
        )
        settings.componentIconPath = file.absolutePath

        val icon = ComponentIconResolver.icon()
        assertNotNull(icon)
        assertNotSame(LuckyIcons.Component, icon)
        assertFitsRowIcon(icon!!)
    }

    fun testBundledIconFileIsLoadable() {
        val file = File("src/main/resources/icons/lucky-component-4.svg")
        assertTrue("测试需在插件模块根目录运行", file.isFile)
        settings.componentIconPath = file.absolutePath

        val icon = ComponentIconResolver.icon()
        assertNotNull(icon)
        assertNotSame("自定义 SVG 加载失败，回退到了内置图标", LuckyIcons.Component, icon)
        assertFitsRowIcon(icon!!)
    }

    /** 图标必须统一渲染为正方形画布，且能放进行内高度（宽容 2x DPI）。 */
    private fun assertFitsRowIcon(icon: Icon) {
        assertEquals("图标画布应为正方形", icon.iconWidth, icon.iconHeight)
        assertTrue("图标宽度 ${icon.iconWidth} 未归一化", icon.iconWidth in 16..32)
    }

    /** 独立于测试夹具的临时文件，避免污染项目目录。 */
    private fun newTempFile(name: String): File {
        val dir = Files.createTempDirectory("lucky-component-icon").toFile()
        dir.deleteOnExit()
        return File(dir, name).also { it.deleteOnExit() }
    }
}
