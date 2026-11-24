package com.luckyframework.idea.spel

import com.intellij.openapi.util.ScalableIcon
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.SVGLoader
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import javax.swing.Icon
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 按屏幕物理像素直接栅格化图标。
 *
 * 平台 `CachedImageIcon` 在 IDE 自管理 HiDPI（125%/150%/200%）下可能只按
 * 逻辑尺寸栅格化 SVG，绘制时再放大，图标因此发虚。这里在绘制阶段读取
 * 组件 Graphics 的实际设备缩放，把 SVG 渲染到对应物理像素的画布，保证
 * 屏幕像素与画布 1:1。
 *
 * JRE HiDPI 下 `JBUIScale.sysScale()` 恒等于逻辑尺寸的 1 倍，系统缩放只
 * 体现在 Graphics 的变换上，因此不能只在加载阶段按逻辑尺寸栅格化。
 */
internal object IconRasterizer {

    /** 从本地文件读取图标（SVG/PNG/JPG/GIF）。 */
    fun load(file: File, logicalSize: Int): Icon? =
        load(file.toURI().toURL(), logicalSize)

    /** 从资源 URL 读取图标（SVG/PNG/JPG/GIF）。 */
    fun load(url: URL, logicalSize: Int): Icon? = try {
        val size = max(logicalSize, 1)
        val canvasSize = initialCanvasSize(size)
        val image = rasterize(url, canvasSize)
        if (image == null) null else CanvasIcon(url, size, mapOf(canvasSize to image))
    } catch (e: Exception) {
        null
    }

    /**
     * 加载阶段的画布尺寸（物理像素）。
     *
     * `JBUIScale.scale` 只反映 IDE 自管理缩放，JRE HiDPI 下它等于逻辑尺寸；
     * 此时真实设备缩放要从 AWT 默认变换读取，避免首帧先按逻辑尺寸渲染、
     * 绘制时再被放大。
     */
    private fun initialCanvasSize(logicalSize: Int): Int {
        val device = (logicalSize * awtScale()).roundToInt()
        return max(1, max(JBUIScale.scale(logicalSize), device))
    }

    private fun awtScale(): Double = try {
        val transform = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice
            .defaultConfiguration
            .defaultTransform
        max(abs(transform.scaleX), abs(transform.scaleY))
    } catch (e: Exception) {
        1.0
    }

    /**
     * 绘制时的设备缩放。
     *
     * JRE HiDPI 下组件 Graphics 已带系统缩放变换，直接使用它的比例；
     * IDE 自管理缩放下变换为 1，再回退到 [JBUIScale.sysScale]。
     */
    private fun deviceScale(g: Graphics?): Double {
        val transform = (g as? Graphics2D)?.transform
        val graphicsScale = transform?.let { max(abs(it.scaleX), abs(it.scaleY)) } ?: 1.0
        return max(graphicsScale, JBUIScale.sysScale().toDouble()).coerceIn(1.0, MAX_DEVICE_SCALE)
    }

    private fun readImage(url: URL, canvasSize: Int): Image? =
        if (url.path.endsWith(".svg", ignoreCase = true)) {
            loadSvg(url, canvasSize)
        } else {
            ImageIO.read(url)
        }

    /**
     * 按目标物理尺寸重新栅格化 SVG。
     *
     * 不能先用 1.0 比例加载成原生画布（图标库常见 200/1024）再缩小：
     * 那样会丢掉小尺寸需要的边缘细节。原生渲染已经不小于目标尺寸时
     * 直接使用，下采样比先缩后放更锐利。
     */
    private fun loadSvg(url: URL, canvasSize: Int): Image? {
        val base = SVGLoader.load(url, 1.0f) ?: return null
        val source = max(base.getWidth(null), base.getHeight(null))
        if (source <= 0) {
            return base
        }
        val scale = canvasSize.toFloat() / source
        return if (scale <= 1.01f) base else SVGLoader.load(url, scale)
    }

    /**
     * 渲染到标准正方形透明画布。
     *
     * 把任意尺寸、任意宽高比的图标等比缩放后居中绘制到 [canvasSize]
     * 物理像素的画布上，使不同来源的图标在行内保持一致的视觉大小。
     */
    private fun rasterize(url: URL, canvasSize: Int): Image? {
        val image = readImage(url, canvasSize) ?: return null
        val sourceWidth = image.getWidth(null)
        val sourceHeight = image.getHeight(null)
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return null
        }
        if (sourceWidth == canvasSize && sourceHeight == canvasSize) {
            return image
        }
        val scale = min(canvasSize.toDouble() / sourceWidth, canvasSize.toDouble() / sourceHeight)
        val width = max(1, (sourceWidth * scale).roundToInt())
        val height = max(1, (sourceHeight * scale).roundToInt())
        val canvas = BufferedImage(canvasSize, canvasSize, BufferedImage.TYPE_INT_ARGB)
        val graphics = canvas.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            val scaled = if (width == sourceWidth && height == sourceHeight) {
                image
            } else {
                image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
            }
            graphics.drawImage(scaled, (canvasSize - width) / 2, (canvasSize - height) / 2, null)
        } finally {
            graphics.dispose()
        }
        return canvas
    }

    /**
     * 物理像素画布图标：逻辑尺寸固定为 [logicalSize]，绘制时按 Graphics 的
     * 实际设备缩放选用对应分辨率的画布，保证物理像素与屏幕像素 1:1。
     *
     * 直接返回 `ImageIcon` 会把物理像素当成逻辑尺寸，平台在 HiDPI 下再放大
     * 一次；只按逻辑尺寸栅格化则在 JRE HiDPI 下被 Graphics 放大，两种情况
     * 都会发虚。
     *
     * 必须实现 [ScalableIcon]：新 UI 的 stripe 按钮会把工具窗口图标强转成
     * `ScalableIcon` 再按按钮尺寸缩放，纯 [Icon] 实现会抛 ClassCastException，
     * 导致工具窗口初始化失败。
     */
    private class CanvasIcon(
        private val url: URL,
        private val logicalSize: Int,
        seed: Map<Int, Image>
    ) : ScalableIcon {

        private val canvases = ConcurrentHashMap<Int, Image>(seed)

        override fun getIconWidth(): Int = logicalSize

        override fun getIconHeight(): Int = logicalSize

        /** 画布按系统 DPI 栅格化，逻辑尺寸固定，基准缩放即系统缩放。 */
        override fun getScale(): Float = JBUIScale.sysScale()

        override fun scale(scale: Float): Icon {
            val scaled = (logicalSize * scale).roundToInt().coerceAtLeast(1)
            return if (scaled == logicalSize) this else CanvasIcon(url, scaled, canvases)
        }

        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val target = max(1, (logicalSize * IconRasterizer.deviceScale(g)).roundToInt())
            val image = imageFor(target) ?: return
            g.drawImage(image, x, y, logicalSize, logicalSize, null)
        }

        private fun imageFor(target: Int): Image? {
            canvases[target]?.let { return it }
            val rendered = try {
                IconRasterizer.rasterize(url, target)
            } catch (e: Exception) {
                null
            } ?: return canvases.values.firstOrNull()
            return canvases.putIfAbsent(target, rendered) ?: rendered
        }
    }

    private const val MAX_DEVICE_SCALE = 4.0
}
