package com.luckyframework.idea.spel

import com.intellij.openapi.util.ScalableIcon
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.image.BufferedImage

/**
 * 栅格化图标必须实现 [ScalableIcon]：新 UI 的 stripe 按钮会把工具窗口图标
 * 强转成 `ScalableIcon` 再按按钮尺寸缩放，纯 Icon 会让工具窗口初始化失败。
 */
class IconRasterizerTest : BasePlatformTestCase() {

    fun `test rasterized icon is scalable`() {
        val url = javaClass.getResource("/icons/lucky-component-1.svg")
        assertNotNull(url)

        val icon = IconRasterizer.load(url!!, 16)
        assertNotNull(icon)
        assertTrue(icon is ScalableIcon)

        val scalable = icon as ScalableIcon
        assertEquals(16, scalable.iconWidth)
        assertEquals(16, scalable.iconHeight)
        assertSame(scalable, scalable.scale(1f))
        assertEquals(24, scalable.scale(1.5f).iconWidth)
    }

    /**
     * JRE HiDPI 下逻辑尺寸不变，缩放只体现在 Graphics 的变换上，
     * 绘制时必须改选按设备缩放栅格化的画布，否则 16 像素位图会被放大而发虚。
     */
    fun `test paint selects canvas by device scale`() {
        val url = javaClass.getResource("/icons/lucky-config.svg")
        assertNotNull(url)

        val icon = IconRasterizer.load(url!!, 16)
        assertNotNull(icon)
        val canvas = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        val graphics = canvas.createGraphics()
        try {
            graphics.scale(2.0, 2.0)
            icon!!.paintIcon(null, graphics, 0, 0)
        } finally {
            graphics.dispose()
        }

        val field = icon!!.javaClass.getDeclaredField("canvases")
        field.isAccessible = true
        val rendered = (field.get(icon) as Map<*, *>)[32] as? BufferedImage
        assertNotNull(rendered)
        assertEquals(32, rendered!!.width)
        assertEquals(32, rendered.height)
    }

    /** 所有走 [LuckyIcons] 内置栅格化的图标都必须具备同一 HiDPI 能力。 */
    fun `test builtin rasterized icons select device scale canvas`() {
        val icons = mapOf(
            "Component" to LuckyIcons.Component,
            "Config" to LuckyIcons.Config,
            "ConfigMissing" to LuckyIcons.ConfigMissing,
            "ToolWindow" to LuckyIcons.ToolWindow
        )
        for ((name, icon) in icons) {
            val canvas = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
            val graphics = canvas.createGraphics()
            try {
                graphics.scale(2.0, 2.0)
                icon.paintIcon(null, graphics, 0, 0)
            } finally {
                graphics.dispose()
            }

            val field = icon.javaClass.getDeclaredField("canvases")
            field.isAccessible = true
            val render = (field.get(icon) as Map<*, *>)[32] as? BufferedImage
            assertNotNull("$name should render a 32px canvas", render)
            assertEquals("$name canvas width", 32, render!!.width)
        }
    }
}
