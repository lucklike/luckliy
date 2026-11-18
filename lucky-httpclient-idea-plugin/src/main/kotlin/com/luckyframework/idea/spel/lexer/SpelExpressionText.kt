package com.luckyframework.idea.spel.lexer

/**
 * SpEL 表达式的文本边界工具：从 `.` 向左回溯接收者表达式的起点。
 *
 * 补全、引用收集与调用检查共用同一套规则，避免多处实现漂移：
 * - 括号/下标/花括号成对闭合后回到整个调用表达式（`fn(1).` → `fn(1)`）；
 * - 未闭合的括号视为当前参数（`add(request.` → `request`）；
 * - 字符串、逗号与运算符是顶层边界；文本运算符（`and`/`or`/`eq`/`not` 等）
 *   与字符运算符地位相同，`?.`、`::` 与选择/投影标记
 *   （`?[`、`![`、`^[`、`$[`）不作为边界。
 */
internal object SpelExpressionText {

    /**
     * 返回接收者表达式在 [text] 内的起点。
     *
     * [text] 必须从表达式起点开始，到 `.` 之前结束。
     */
    fun receiverStart(text: String): Int {
        var start = 0
        var quote: Char? = null
        val groups = ArrayList<Pair<Char, Int>>()
        var index = 0
        while (index < text.length) {
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
                // 文本运算符与字符运算符一样切分接收者，否则
                // `page > 0 and {10, 20, 50}.contains(size)` 的接收者会算成
                // `0 and {10, 20, 50}`，成员解析全部失败
                current.isLetter() && (index == 0 || !isIdentifierPart(text[index - 1])) -> {
                    val operatorEnd = textOperatorEnd(text, index)
                    if (operatorEnd > index) {
                        start = operatorEnd
                        index = operatorEnd
                        continue
                    }
                }
                // `::` 是参数类型后缀（`$0::type`），不是三目运算符的分隔符
                current == ':' && text.getOrNull(index + 1) == ':' -> {
                    index += 2
                    continue
                }

                // `?.` 安全导航属于接收者表达式的一部分
                current == '?' && text.getOrNull(index + 1) == '.' -> Unit
                // `?[`/`![`/`^[` 是选择/投影标记，`[` 本身不是独立的下标：
                // `members.?[x]` 的接收者应回到 `members`
                current in SELECTION_MARKERS && text.getOrNull(index + 1) == '[' -> {
                    groups.add('[' to start)
                    start = index + 2
                    index += 2
                    continue
                }
                current in OPEN_BRACKETS -> {
                    groups.add(current to start)
                    start = index + 1
                }

                current in CLOSE_BRACKETS -> {
                    val open = groups.removeLastOrNull()
                    if (open != null && CLOSE_BRACKETS[current] == open.first) {
                        start = open.second
                    }
                }

                current in MEMBER_BOUNDARIES -> start = index + 1
            }
            index++
        }
        return start
    }

    private val OPEN_BRACKETS = setOf('(', '[', '{')
    private val CLOSE_BRACKETS = mapOf(')' to '(', ']' to '[', '}' to '{')
    private val MEMBER_BOUNDARIES = setOf(
        ',', '?', ':', '+', '-', '*', '/', '%', '!', '=', '<', '>', '&', '|', '^', '~'
    )

    /**
     * SpEL 文本运算符。
     *
     * 与 [MEMBER_BOUNDARIES] 的字符运算符作用相同：位于运算符右侧的
     * 子表达式整体才是接收者。与 Spring 解析器一致，文本运算符大小写不敏感；
     * `not` 是一元前缀运算符，只在其前面是运算符/括号/逗号/表达式开头时切分。
     */
    private val TEXT_OPERATORS = setOf(
        "and", "or", "not", "eq", "ne", "lt", "le", "gt", "ge",
        "div", "mod", "matches", "between", "instanceof", "as"
    )

    /**
     * [index] 处文本运算符的结束位置；不是运算符时返回 -1。
     *
     * `obj.and`、`#and` 的单词前紧邻 `.` 或 `#`，按成员/变量名处理，
     * 不参与切分；`xand` 这类连续标识符内部同样不切分。
     */
    private fun textOperatorEnd(text: String, index: Int): Int {
        var end = index + 1
        while (end < text.length && isIdentifierPart(text[end])) {
            end++
        }
        val word = text.substring(index, end).lowercase()
        if (word !in TEXT_OPERATORS) {
            return -1
        }
        val before = previousSignificant(text, index)
        return when {
            word == "not" -> if (isOperatorPrefix(before)) end else -1
            isValueEnd(before) -> end
            else -> -1
        }
    }

    /** 单词前最后一个非空白字符；到达表达式开头时返回 null。 */
    private fun previousSignificant(text: String, index: Int): Char? {
        var cursor = index - 1
        while (cursor >= 0 && text[cursor].isWhitespace()) {
            cursor--
        }
        return text.getOrNull(cursor)
    }

    private fun isIdentifierPart(char: Char): Boolean = char.isLetterOrDigit() || char == '_'

    /** 中缀运算符左侧的值结束字符：标识符字符、`)`、`]`、`}`、字符串右引号。 */
    private fun isValueEnd(char: Char?): Boolean =
        char != null && (isIdentifierPart(char) || char in ")]}\"'")

    /** `not` 只做前缀运算符：前面必须是运算符、括号、逗号、冒号或表达式开头。 */
    private fun isOperatorPrefix(char: Char?): Boolean =
        char == null || char.isWhitespace() || char in "([{,;:=<>!&|+-*/%^~?\"'"

    /** 选择/投影标记；`$[` 的 `$` 本就不是边界，无需在此处理。 */
    private val SELECTION_MARKERS = setOf('?', '!', '^')

    /**
     * 按顶层逗号切分实参文本；括号、方括号、花括号与字符串内部的逗号不切分。
     *
     * [generics] 为 true 时尖括号也参与嵌套深度计算，用于切分函数签名中的泛型参数。
     */
    fun splitArguments(text: String, generics: Boolean = false): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }
        val result = ArrayList<String>()
        var depth = 0
        var quote: Char? = null
        var start = 0
        var index = 0
        while (index < text.length) {
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
            when (current) {
                '\'', '"' -> quote = current
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                '<', '>' -> if (generics) depth += if (current == '<') 1 else -1
                ',' -> if (depth == 0) {
                    result += text.substring(start, index)
                    start = index + 1
                }
            }
            index++
        }
        result += text.substring(start)
        return result.map { it.trim() }.filter { it.isNotEmpty() }
    }
}
