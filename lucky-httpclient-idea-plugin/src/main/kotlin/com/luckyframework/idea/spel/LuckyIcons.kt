package com.luckyframework.idea.spel

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.util.IconUtil
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon

/**
 * 插件品牌图标。
 *
 * SVG 资源位于 `src/main/resources/icons`，平台按主题自动选择同目录下的
 * `*_dark.svg` 变体。
 */
object LuckyIcons {

    /** 插件 Logo（设置页、品牌展示）。 */
    @JvmField
    val Logo: Icon = loadIcon("/icons/luckySpel.svg")

    /** `@LuckyComponent` 组件类的内置图标。 */
    @JvmField
    val Component: Icon = loadIcon("/icons/lucky-component-3.svg")

    /** `@StdHttpClient` 接口/方法已关联配置文件的图标。 */
    @JvmField
    val Config: Icon = loadIcon("/icons/lucky-config.svg")

    /** `@StdHttpClient` 接口未找到配置文件的提示图标。 */
    @JvmField
    val ConfigMissing: Icon = loadIcon("/icons/lucky-config-missing.svg")

    /** Lucky 侧边导航栏的 Tool Window 图标。 */
    @JvmField
    val ToolWindow: Icon = loadIcon("/icons/lucky-r3.svg")

    /**
     * HTTP 请求方法徽标（GET/POST 等）。
     *
     * 未识别的方法返回 null，由调用方回退平台方法图标。
     */
    fun httpMethod(name: String?): Icon? {
        val method = name?.trim()?.uppercase().orEmpty()
        if (method.isEmpty()) {
            return null
        }
        return HTTP_METHOD_ICONS.getOrPut(method) {
            HttpMethodIcon(method, METHOD_COLORS[method] ?: DEFAULT_METHOD_COLOR)
        }
    }

    /**
     * 归一化为平台标准图标尺寸。
     *
     * 图标库导出的 SVG 常声明 `width="200"` 一类的画布尺寸，而 `IconProvider`
     * 返回的图标会按原尺寸画在项目树、导航栏的行内，不缩放就会溢出相邻行
     * 并被裁切。
     */
    internal fun normalize(icon: Icon): Icon {
        val width = icon.iconWidth
        return if (width <= 0 || width == ICON_SIZE) {
            icon
        } else {
            IconUtil.scale(icon, null, ICON_SIZE.toFloat() / width)
        }
    }

    /** 加载插件内置图标并归一化尺寸。 */
    private fun loadIcon(path: String): Icon =
        normalize(IconLoader.getIcon(path, LuckyIcons::class.java))

    private const val ICON_SIZE = 16
}

/** 徽标色：浅色主题用深色块，深色主题用亮色块，保证白字可读。 */
private val METHOD_COLORS: Map<String, JBColor> = mapOf(
    "GET" to JBColor(Color(0x2C8A46), Color(0x3FA85B)),
    "POST" to JBColor(Color(0x2F6FBE), Color(0x4385D6)),
    "PUT" to JBColor(Color(0xB97A0C), Color(0xD39422)),
    "DELETE" to JBColor(Color(0xB33B33), Color(0xCC4C43)),
    "PATCH" to JBColor(Color(0x0E7F7F), Color(0x229999)),
    "HEAD" to JBColor(Color(0x7150A8), Color(0x8A69C4)),
    "OPTIONS" to JBColor(Color(0x5B6B7C), Color(0x75879A)),
    "TRACE" to JBColor(Color(0x806044), Color(0x9A785A)),
    "CONNECT" to JBColor(Color(0x45549B), Color(0x5D6DB8))
)

private val DEFAULT_METHOD_COLOR = JBColor(Color(0x6E7681), Color(0x8B949E))

private val HTTP_METHOD_ICONS = ConcurrentHashMap<String, Icon>()

/**
 * HTTP 方法徽标：圆角色块 + 白色方法名。
 *
 * 平台没有公开的请求方法图标，用代码绘制可以避免新增 SVG 资源，
 * 并根据方法名长度自动调整宽度与字号（DELETE/OPTIONS 等长名不被裁切）。
 */
private class HttpMethodIcon(
    private val text: String,
    private val background: Color
) : Icon {

    private val width: Int = (text.length * 5 + 6).coerceIn(MIN_WIDTH, MAX_WIDTH)

    override fun getIconWidth(): Int = width

    override fun getIconHeight(): Int = HEIGHT

    override fun paintIcon(component: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g2.color = background
            g2.fillRoundRect(x, y, width, HEIGHT, ARC, ARC)
            g2.color = Color.WHITE
            g2.font = if (text.length >= 6) SMALL_FONT else FONT
            val metrics = g2.fontMetrics
            g2.drawString(
                text,
                x + (width - metrics.stringWidth(text)) / 2,
                y + (HEIGHT - metrics.height) / 2 + metrics.ascent
            )
        } finally {
            g2.dispose()
        }
    }

    private companion object {
        private const val HEIGHT = 12
        private const val ARC = 6
        private const val MIN_WIDTH = 20
        private const val MAX_WIDTH = 32
        private val FONT = Font("Dialog", Font.BOLD, 8)
        private val SMALL_FONT = Font("Dialog", Font.BOLD, 7)
    }
}
