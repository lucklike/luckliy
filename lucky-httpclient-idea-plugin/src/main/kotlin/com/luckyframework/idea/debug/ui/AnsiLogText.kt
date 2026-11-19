package com.luckyframework.idea.debug.ui

import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

/**
 * 终端日志的 ANSI SGR 解析。
 *
 * 子进程日志带有 `ESC[...m` 颜色/样式控制序列，直接显示会变成一串
 * `[1;35m` 之类的文本。这里把每行拆成带样式的片段后写入
 * [StyledDocument]，只处理 SGR（颜色与字体样式），光标移动等
 * 其它 CSI 序列直接丢弃。
 */
object AnsiLogText {

    private const val ESC = '\u001B'

    /** 一段文本及其 ANSI 样式；颜色为 null 表示使用组件默认前景/背景。 */
    data class Style(
        val foreground: Color? = null,
        val background: Color? = null,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val underline: Boolean = false
    )

    data class Segment(val text: String, val style: Style)

    /** 把一行含 ANSI 转义的文本拆成带样式的片段。 */
    fun parse(line: String): List<Segment> {
        val segments = ArrayList<Segment>()
        var current = Style()
        val text = StringBuilder()

        fun flush() {
            if (text.isNotEmpty()) {
                segments.add(Segment(text.toString(), current))
                text.setLength(0)
            }
        }

        var index = 0
        while (index < line.length) {
            val ch = line[index]
            if (ch == ESC && index + 1 < line.length && line[index + 1] == '[') {
                val end = csiEnd(line, index + 2)
                if (end < 0) {
                    // 未闭合的转义序列按普通文本保留
                    text.append(ch)
                    index++
                    continue
                }
                flush()
                if (line[end] == 'm') {
                    current = applySgr(line.substring(index + 2, end), current)
                }
                index = end + 1
            } else {
                text.append(ch)
                index++
            }
        }
        flush()
        return segments
    }

    /** 去掉 ANSI 控制序列后的可见文本，用于宽度测量等场景。 */
    fun plainText(line: String): String = parse(line).joinToString("") { it.text }

    /** 把一行日志按 ANSI 样式写入文档；[fontFamily] 用于保持日志区的等宽字体。 */
    fun insert(document: StyledDocument, line: String, fontFamily: String? = null) {
        for (segment in parse(line)) {
            if (segment.text.isEmpty()) {
                continue
            }
            val attributes = attributes(segment.style)
            if (fontFamily != null) {
                StyleConstants.setFontFamily(attributes, fontFamily)
            }
            document.insertString(document.length, segment.text, attributes)
        }
    }

    fun attributes(style: Style): SimpleAttributeSet {
        val attributes = SimpleAttributeSet()
        style.foreground?.let { StyleConstants.setForeground(attributes, it) }
        style.background?.let { StyleConstants.setBackground(attributes, it) }
        if (style.bold) {
            StyleConstants.setBold(attributes, true)
        }
        if (style.italic) {
            StyleConstants.setItalic(attributes, true)
        }
        if (style.underline) {
            StyleConstants.setUnderline(attributes, true)
        }
        return attributes
    }

    /** CSI 参数串的终止字节（0x40..0x7E），找不到说明序列未闭合。 */
    private fun csiEnd(line: String, start: Int): Int {
        var index = start
        while (index < line.length) {
            if (line[index] in '@'..'~') {
                return index
            }
            index++
        }
        return -1
    }

    private fun applySgr(parameters: String, style: Style): Style {
        val params = if (parameters.isEmpty()) {
            listOf(0)
        } else {
            parameters.split(';').map { it.toIntOrNull() ?: 0 }
        }
        var result = style
        var index = 0
        while (index < params.size) {
            when (val code = params[index]) {
                0 -> result = Style()
                1 -> result = result.copy(bold = true)
                3 -> result = result.copy(italic = true)
                4 -> result = result.copy(underline = true)
                22 -> result = result.copy(bold = false)
                23 -> result = result.copy(italic = false)
                24 -> result = result.copy(underline = false)
                in 30..37 -> result = result.copy(foreground = palette(code - 30))
                39 -> result = result.copy(foreground = null)
                in 40..47 -> result = result.copy(background = palette(code - 40))
                49 -> result = result.copy(background = null)
                in 90..97 -> result = result.copy(foreground = palette(code - 90 + 8))
                in 100..107 -> result = result.copy(background = palette(code - 100 + 8))
                38 -> {
                    val (color, consumed) = extendedColor(params, index)
                    result = result.copy(foreground = color ?: result.foreground)
                    index += consumed - 1
                }
                48 -> {
                    val (color, consumed) = extendedColor(params, index)
                    result = result.copy(background = color ?: result.background)
                    index += consumed - 1
                }
                else -> Unit
            }
            index++
        }
        return result
    }

    /** `38/48;5;n`、`38/48;2;r;g;b` 扩展颜色，返回颜色与消耗的参数个数。 */
    private fun extendedColor(params: List<Int>, start: Int): Pair<Color?, Int> {
        if (start + 1 >= params.size) {
            return null to 1
        }
        return when (params[start + 1]) {
            5 -> {
                if (start + 2 >= params.size) {
                    null to 2
                } else {
                    color256(params[start + 2]) to 3
                }
            }

            2 -> {
                if (start + 4 >= params.size) {
                    null to 2
                } else {
                    val red = params[start + 2].coerceIn(0, 255)
                    val green = params[start + 3].coerceIn(0, 255)
                    val blue = params[start + 4].coerceIn(0, 255)
                    Color(red, green, blue) to 5
                }
            }

            else -> null to 2
        }
    }

    /** xterm 256 色：0-15 基础色，16-231 色立方，232-255 灰阶。 */
    private fun color256(index: Int): Color {
        val value = index.coerceIn(0, 255)
        if (value < 16) {
            return PALETTE[value]
        }
        if (value < 232) {
            val offset = value - 16
            return Color(
                cube(offset / 36),
                cube((offset % 36) / 6),
                cube(offset % 6)
            )
        }
        val gray = 8 + (value - 232) * 10
        return Color(gray, gray, gray)
    }

    private fun cube(value: Int): Int = if (value == 0) 0 else 55 + value * 40

    private fun palette(index: Int): Color = PALETTE[index.coerceIn(0, PALETTE.size - 1)]

    /**
     * 终端 16 色调色板。
     *
     * 同一索引在浅色/深色主题下取不同亮度，保证两套主题下都能看清。
     */
    private val PALETTE: Array<Color> = arrayOf(
        JBColor(Color(0x000000), Color(0x777777)), // black
        JBColor(Color(0xCD3131), Color(0xCD3131)), // red
        JBColor(Color(0x008000), Color(0x23D18B)), // green
        JBColor(Color(0x949800), Color(0xE5E510)), // yellow
        JBColor(Color(0x0451A5), Color(0x3B8EEA)), // blue
        JBColor(Color(0xBC05BC), Color(0xD670D6)), // magenta
        JBColor(Color(0x0598BC), Color(0x29B8DB)), // cyan
        JBColor(Color(0x555555), Color(0xE5E5E5)), // white
        JBColor(Color(0x666666), Color(0x999999)), // bright black
        JBColor(Color(0xCD3131), Color(0xF14C4C)), // bright red
        JBColor(Color(0x14CE14), Color(0x23D18B)), // bright green
        JBColor(Color(0xB5BA00), Color(0xF5F543)), // bright yellow
        JBColor(Color(0x316BCD), Color(0x3B8EEA)), // bright blue
        JBColor(Color(0xBC05BC), Color(0xD670D6)), // bright magenta
        JBColor(Color(0x0598BC), Color(0x29B8DB)), // bright cyan
        JBColor(Color(0xA5A5A5), Color(0xE5E5E5)) // bright white
    )
}
