package com.luckyframework.idea.spel.lexer

/**
 * 一个未闭合的选择/投影方括号作用域：`container.?[ ... ]`、`container.![ ... ]`。
 *
 * [base] 是标记前的接收者表达式（已去掉 `.`/`?.`），起始形式（`?[ ... ]`）
 * 为空串；[markerOffset] 是标记字符（`?`/`!`/`^`/`$`）的偏移；
 * [innerStart] 是 `[` 之后的偏移；[innerEnd] 是扫描终点（不含）。
 */
data class SpelElementScope(
    val base: String,
    val markerOffset: Int,
    val innerStart: Int,
    val innerEnd: Int
)

/**
 * 按纯文本扫描光标所在位置的选择/投影作用域。
 *
 * 补全、成员检查与引用解析共用同一套规则，保证三处对
 * `.?[...]`/`.![...]` 元素上下文的判断一致：
 * - `?[`/`![`/`^[`/`$[` 与 `[` 必须紧邻，`$name$` 响应变量整体跳过
 *   （`$body$[0]` 是下标访问，不是 `$[` 选择表达式）；
 * - 字符串、`(`/`{`/`[` 配对与普通下标不影响外层作用域；
 * - 只返回扫描到 [to] 时仍未闭合的作用域，按由外到内排列。
 */
object SpelElementScopes {

    /** 单字符元素标记；`?` 另有 `?.`/`?:`/三目的用途，仅紧邻 `[` 时成立。 */
    private val MARKERS = setOf('?', '!', '^', '$')

    /**
     * [from] 到 [to] 之间仍未闭合的选择/投影作用域，由外到内。
     *
     * [from] 是片段表达式起点，[to] 是查询位置（token 起点、光标等）。
     */
    fun enclosing(text: String, from: Int, to: Int): List<SpelElementScope> {
        val start = from.coerceIn(0, text.length)
        val limit = to.coerceIn(start, text.length)
        val frames = ArrayList<Frame>()
        var quote: Char? = null
        var index = start
        while (index < limit) {
            val current = text[index]
            if (quote != null) {
                when {
                    current == '\\' -> {
                        index += 2
                        continue
                    }

                    current == quote -> quote = null
                }
                index++
                continue
            }
            when {
                current == '\'' || current == '"' -> quote = current
                // `$name$` 响应变量整体跳过，避免 `$body$[0]` 被当成 `$[` 选择表达式
                current == '$' && text.getOrNull(index + 1) != '[' -> {
                    val end = readRootVariable(text, index + 1, limit)
                    index = if (end > index + 1) end else index + 1
                    continue
                }
                current in MARKERS && text.getOrNull(index + 1) == '[' -> {
                    frames += elementFrame(text, start, index)
                    index += 2
                    continue
                }
                current == '(' || current == '{' || current == '[' -> frames += Frame.Group
                current == ')' || current == '}' -> popGroup(frames)
                current == ']' -> {
                    // 元素作用域以 `]` 闭合；普通下标只弹出普通括号
                    if (frames.lastOrNull() is Frame.Element) {
                        frames.removeAt(frames.lastIndex)
                    } else {
                        popGroup(frames)
                    }
                }
            }
            index++
        }
        return frames.filterIsInstance<Frame.Element>().map {
            SpelElementScope(it.base, it.markerOffset, it.innerStart, limit)
        }
    }

    /** 标记前的接收者：去掉尾部空白与 `.`/`?.` 后按 [SpelExpressionText.receiverStart] 回溯。 */
    private fun elementFrame(text: String, from: Int, markerOffset: Int): Frame.Element {
        // 允许 `. ?[`/`?. ![` 这类标记前夹杂空白的写法
        var baseEnd = markerOffset
        while (baseEnd > from && text[baseEnd - 1].isWhitespace()) {
            baseEnd--
        }
        if (baseEnd > from && text[baseEnd - 1] == '.') {
            baseEnd--
            if (baseEnd > from && text[baseEnd - 1] == '?') {
                baseEnd--
            }
        }
        val receiverText = text.substring(from, baseEnd)
        val receiverStart = from + SpelExpressionText.receiverStart(receiverText)
        val base = text.substring(receiverStart, baseEnd).trim()
        return Frame.Element(base, markerOffset, markerOffset + 2)
    }

    /** `$` 之后的响应变量：`$body$`、`$0`、`$n::type` 等整体跳过。 */
    private fun readRootVariable(text: String, start: Int, limit: Int): Int {
        var cursor = start
        while (cursor < limit && text[cursor].isJavaIdentifierPart() && text[cursor] != '$') {
            cursor++
        }
        if (cursor == start) {
            return start
        }
        return if (cursor < limit && text[cursor] == '$') cursor + 1 else cursor
    }

    private fun popGroup(frames: MutableList<Frame>) {
        if (frames.lastOrNull() is Frame.Group) {
            frames.removeAt(frames.lastIndex)
        }
    }

    private sealed interface Frame {
        /** 普通括号/下标，只用于配对，不产生元素上下文。 */
        object Group : Frame

        /** 选择/投影方括号。 */
        data class Element(
            val base: String,
            val markerOffset: Int,
            val innerStart: Int
        ) : Frame
    }
}
