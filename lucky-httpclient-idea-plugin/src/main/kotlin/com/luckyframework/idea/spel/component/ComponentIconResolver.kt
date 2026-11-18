package com.luckyframework.idea.spel.component

import com.intellij.ui.scale.JBUIScale
import com.intellij.util.SVGLoader
import com.luckyframework.idea.spel.LuckyIcons
import com.luckyframework.idea.spel.settings.LuckyHttpClientSettings
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.Icon
import javax.swing.ImageIcon
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * `@LuckyComponent` 组件类图标解析。
 *
 * 解析顺序：
 * 1. 设置中关闭「替换图标」→ 返回 null，沿用平台默认的类图标；
 * 2. 配置了可读取的本地图标文件 → 使用该文件（SVG/PNG/JPG/GIF）；
 * 3. 其余情况 → 使用插件内置图标。
 *
 * 用户提供的图标尺寸与宽高比各不相同，这里统一渲染到标准正方形画布
 * （保持宽高比居中），避免行内图标忽大忽小。
 *
 * `IconProvider` 会在项目树、导航栏等位置随重绘高频调用，这里按
 * 「路径 + 文件修改时间 + 当前 DPI」缓存，图标文件或显示缩放变化后自动重新加载。
 */
object ComponentIconResolver {

    private var cacheKey: String? = null
    private var cacheIcon: Icon? = null

    /** 当前生效的组件类图标；关闭图标替换时返回 null。 */
    @Synchronized
    fun icon(): Icon? {
        val settings = LuckyHttpClientSettings.getInstance()
        if (!settings.isComponentIconEnabled) {
            return null
        }
        val path = settings.componentIconPath.trim()
        if (path.isEmpty()) {
            return LuckyIcons.Component
        }
        val file = File(path)
        if (!file.isFile) {
            return LuckyIcons.Component
        }
        val key = "$path@${file.lastModified()}@${JBUIScale.scale(ICON_SIZE)}"
        if (key == cacheKey) {
            return cacheIcon
        }
        val icon = load(file, ICON_SIZE) ?: LuckyIcons.Component
        cacheKey = key
        cacheIcon = icon
        return icon
    }

    /** 设置变更后丢弃缓存，使新图标立即生效。 */
    @Synchronized
    fun invalidate() {
        cacheKey = null
        cacheIcon = null
    }

    /** 设置页预览：按路径读取图标并渲染到较大画布，读取失败返回 null。 */
    fun preview(path: String): Icon? {
        val file = File(path)
        return if (file.isFile) load(file, PREVIEW_SIZE) else null
    }

    private fun load(file: File, logicalSize: Int): Icon? = try {
        val image = if (file.extension.equals("svg", ignoreCase = true)) {
            SVGLoader.load(file.toURI().toURL(), 1.0f)
        } else {
            ImageIO.read(file)
        }
        if (image == null || image.getWidth(null) <= 0 || image.getHeight(null) <= 0) {
            null
        } else {
            toCanvasIcon(image, logicalSize)
        }
    } catch (e: Exception) {
        null
    }

    /**
     * 渲染到标准正方形透明画布。
     *
     * 把任意尺寸、任意宽高比的图标等比缩放后居中绘制到
     * `logicalSize` 逻辑尺寸的画布上（物理像素按当前 DPI 换算），
     * 使不同来源的图标在项目树行内保持一致的视觉大小。
     */
    private fun toCanvasIcon(image: Image, logicalSize: Int): Icon {
        val target = max(logicalSize, JBUIScale.scale(logicalSize))
        val sourceWidth = image.getWidth(null)
        val sourceHeight = image.getHeight(null)
        val scale = min(target.toDouble() / sourceWidth, target.toDouble() / sourceHeight)
        val width = max(1, (sourceWidth * scale).roundToInt())
        val height = max(1, (sourceHeight * scale).roundToInt())
        val canvas = BufferedImage(target, target, BufferedImage.TYPE_INT_ARGB)
        val graphics = canvas.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            val scaled = image.getScaledInstance(width, height, Image.SCALE_SMOOTH)
            graphics.drawImage(scaled, (target - width) / 2, (target - height) / 2, null)
        } finally {
            graphics.dispose()
        }
        return ImageIcon(canvas)
    }

    private const val ICON_SIZE = 16
    private const val PREVIEW_SIZE = 32
}
