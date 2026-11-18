package com.luckyframework.idea.spel.environment

import com.intellij.openapi.util.TextRange

/**
 * 一个 Spring 环境变量占位符 `${key:default}`。
 *
 * 范围相对承载它的字符串字面量/标量源码文本：
 * - [startOffset] 指向 `$`，[endOffset] 指向结尾 `}` 之后；未闭合时等于文本长度；
 * - [keyStartOffset]/[keyEndOffset] 只覆盖键名，不含 `${`、`}` 与默认值；
 * - [defaultStartOffset]/[defaultEndOffset] 覆盖 `:` 之后的默认值。
 */
data class SpringPlaceholder(
    val startOffset: Int,
    val endOffset: Int,
    val keyStartOffset: Int,
    val keyEndOffset: Int,
    val key: String,
    val defaultStartOffset: Int? = null,
    val defaultEndOffset: Int? = null,
    val defaultText: String? = null,
    val closed: Boolean = true
) {

    val range: TextRange get() = TextRange(startOffset, endOffset)

    val keyRange: TextRange get() = TextRange(keyStartOffset, keyEndOffset)

    val defaultRange: TextRange?
        get() = if (defaultStartOffset == null || defaultEndOffset == null) {
            null
        } else {
            TextRange(defaultStartOffset, defaultEndOffset)
        }
}

/**
 * `${key:default}` 的纯文本解析。
 *
 * 与 Spring `PropertyPlaceholderHelper` 一致：
 * - 支持嵌套占位符，`${a:${b}}` 的结尾是外层 `}`；
 * - 键名与默认值由第一层 `:` 分隔，嵌套占位符里的 `:` 不参与分隔；
 * - 未闭合的 `${` 不会被丢弃，[SpringPlaceholder.closed] 为 false，
 *   由调用方决定显示原文本还是报语法错误。
 *
 * 解析器不依赖 PSI，供词法分析、高亮、补全、折叠与引用共同使用。
 */
object SpringPlaceholderParser {

    fun parse(text: String, from: Int = 0, to: Int = text.length): List<SpringPlaceholder> {
        val placeholders = ArrayList<SpringPlaceholder>()
        var index = from.coerceIn(0, text.length)
        val limit = to.coerceIn(index, text.length)
        while (index < limit - 1) {
            if (text[index] != '$' || text[index + 1] != '{') {
                index++
                continue
            }
            placeholders += parseAt(text, index, limit)
            val parsed = placeholders.last()
            index = if (parsed.closed) parsed.endOffset else limit
        }
        return placeholders
    }

    /**
     * 光标处正在书写的键名占位符。
     *
     * 光标位于键名内（含末尾）时返回占位符；位于默认值或 `}` 之后返回 null，
     * 避免键名补全出现在默认值位置。
     */
    fun placeholderAt(text: String, offset: Int): SpringPlaceholder? =
        parse(text).firstOrNull { placeholder ->
            offset >= placeholder.keyStartOffset && offset <= placeholder.keyEndOffset
        }

    /** 光标处于占位符任意位置（含默认值）时返回它，用于让 SpEL 补全让位。 */
    fun placeholderContaining(text: String, offset: Int): SpringPlaceholder? =
        parse(text).firstOrNull { placeholder ->
            offset >= placeholder.startOffset && offset <= placeholder.endOffset
        }

    /**
     * 从 `${` 之后的 [contentStart] 找到配对的 `}`，返回其后的偏移。
     * 找不到时返回 -1。
     */
    fun findEnd(text: String, contentStart: Int, limit: Int = text.length): Int {
        var depth = 0
        var index = contentStart.coerceIn(0, text.length)
        val end = limit.coerceIn(index, text.length)
        while (index < end) {
            if (index + 1 < end && text[index] == '$' && text[index + 1] == '{') {
                depth++
                index += 2
                continue
            }
            if (text[index] == '}') {
                if (depth == 0) {
                    return index + 1
                }
                depth--
            }
            index++
        }
        return -1
    }

    private fun parseAt(text: String, start: Int, limit: Int): SpringPlaceholder {
        val keyStart = start + 2
        val end = findEnd(text, keyStart, limit)
        val closed = end >= 0
        val contentEnd = if (closed) end - 1 else limit
        val separator = separatorIndex(text, keyStart, contentEnd)
        val keyEnd = separator ?: contentEnd
        val key = text.substring(keyStart, keyEnd)
        val defaultStart = separator?.let { it + 1 }
        val defaultText = defaultStart?.let { text.substring(it, contentEnd) }
        return SpringPlaceholder(
            startOffset = start,
            endOffset = if (closed) end else limit,
            keyStartOffset = keyStart,
            keyEndOffset = keyEnd,
            key = key,
            defaultStartOffset = defaultStart,
            defaultEndOffset = defaultStart?.let { contentEnd },
            defaultText = defaultText,
            closed = closed
        )
    }

    /** 第一层 `:` 的位置；嵌套 `${...}` 内的冒号跳过。 */
    private fun separatorIndex(text: String, from: Int, to: Int): Int? {
        var index = from
        while (index < to) {
            if (index + 1 < to && text[index] == '$' && text[index + 1] == '{') {
                val nestedEnd = findEnd(text, index + 2, to)
                index = if (nestedEnd < 0) to else nestedEnd
                continue
            }
            if (text[index] == ':') {
                return index
            }
            index++
        }
        return null
    }
}
