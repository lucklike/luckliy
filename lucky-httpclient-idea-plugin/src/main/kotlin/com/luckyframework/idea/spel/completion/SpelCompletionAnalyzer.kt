package com.luckyframework.idea.spel.completion

/**
 * 光标所在位置的补全类别。
 */
enum class SpelCompletionMode {
    /** `#` 之后的变量、函数与命名空间 */
    VARIABLE,

    /** 表达式起始位置或运算符之后的 Root 变量 */
    ROOT,

    /** `.` 之后的成员 */
    MEMBER,

    /** `T(` 之后的类型 */
    TYPE
}

/**
 * 一个补全位置。
 *
 * [prefixOffset] 是候选替换范围的起点（`#` 之后时包含 `#`，并跳过中间的空白）；
 * [wordOffset] 是当前输入单词的起点，`$` 简写候选需要保留前面的 `#`。
 * [qualifier] 是成员补全时 `.` 左侧的接收者表达式。
 */
data class SpelCompletionSite(
    val mode: SpelCompletionMode,
    val word: String,
    val wordOffset: Int,
    val prefixOffset: Int,
    val qualifier: String? = null
)

/**
 * 判断光标是否位于一个未闭合的 `#{...}` 片段内，并区分补全类别。
 *
 * 只处理字面量内部文本，不依赖 PSI；字符串字面量内部不补全。
 */
class SpelCompletionAnalyzer {

    fun analyze(literalText: String, offsetInLiteral: Int): SpelCompletionSite? {
        if (offsetInLiteral <= 0 || offsetInLiteral > literalText.length) {
            return null
        }
        val expressionStart = unclosedExpressionStart(literalText, offsetInLiteral) ?: return null
        if (insideString(literalText, expressionStart, offsetInLiteral)) {
            return null
        }

        var wordOffset = offsetInLiteral
        while (wordOffset > expressionStart && isWordPart(literalText[wordOffset - 1])) {
            wordOffset--
        }
        val word = literalText.substring(wordOffset, offsetInLiteral)
        val head = literalText.substring(expressionStart, wordOffset).trimEnd()

        if (TYPE_PREFIX.containsMatchIn(head)) {
            return SpelCompletionSite(SpelCompletionMode.TYPE, word, wordOffset, wordOffset)
        }
        if (head.endsWith(".")) {
            // `?.` 的接收者在 `?` 之前
            val receiver = head.dropLast(1).removeSuffix("?").trimEnd()
            val qualifier = receiver.substring(receiverStart(receiver)).trim()
            if (qualifier.isEmpty()) {
                return null
            }
            return SpelCompletionSite(SpelCompletionMode.MEMBER, word, wordOffset, wordOffset, qualifier)
        }
        if (head.endsWith("#")) {
            val hashOffset = expressionStart + head.length - 1
            return SpelCompletionSite(SpelCompletionMode.VARIABLE, word, wordOffset, hashOffset)
        }
        if (head.isEmpty() || ROOT_BOUNDARY.containsMatchIn(head.takeLast(2))) {
            return SpelCompletionSite(SpelCompletionMode.ROOT, word, wordOffset, wordOffset)
        }
        return null
    }

    /** 最后一个包含光标的未闭合 `#{` 的表达式起点；没有时返回 null。 */
    private fun unclosedExpressionStart(text: String, offset: Int): Int? {
        var from = 0
        var result: Int? = null
        while (true) {
            val index = text.indexOf("#{", from)
            if (index < 0 || index + 2 > offset) {
                break
            }
            if (isOpenAt(text, index, offset)) {
                result = index + 2
            }
            from = index + 2
        }
        return result
    }

    /** 从 `#{` 开始扫描到光标；花括号未配平时视为仍在片段内。 */
    private fun isOpenAt(text: String, fragmentStart: Int, offset: Int): Boolean {
        var depth = 0
        var quote: Char? = null
        var index = fragmentStart
        while (index < offset) {
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
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth <= 0) {
                        return false
                    }
                }
            }
            index++
        }
        return depth > 0
    }

    /** 光标是否位于片段内的字符串字面量中。 */
    private fun insideString(text: String, from: Int, to: Int): Boolean {
        var quote: Char? = null
        var index = from
        while (index < to) {
            val current = text[index]
            if (quote != null) {
                when {
                    current == '\\' -> {
                        index += 2
                        continue
                    }

                    current == quote -> quote = null
                }
            } else if (current == '\'' || current == '"') {
                quote = current
            }
            index++
        }
        return quote != null
    }

    private fun isWordPart(char: Char): Boolean =
        Character.isJavaIdentifierPart(char) || char == '$'

    /**
     * `.` 左侧接收者表达式在 [text] 内的起点。
     *
     * 函数参数内的成员访问只取当前参数（`add(request.` → `request`）；
     * 括号成对闭合后回到整个调用表达式（`fn(1).` → `fn(1)`）。
     * 字符串、逗号与运算符都是顶层边界，未闭合的括号不影响起点。
     */
    private fun receiverStart(text: String): Int {
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
                // `::` 是参数类型后缀（`$0::type`），不是三目运算符的分隔符
                current == ':' && text.getOrNull(index + 1) == ':' -> {
                    index += 2
                    continue
                }

                // `?.` 安全导航属于接收者表达式的一部分
                current == '?' && text.getOrNull(index + 1) == '.' -> Unit
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

    private companion object {
        val TYPE_PREFIX = Regex("""T\s*\(\s*$""")
        val ROOT_BOUNDARY = Regex("""[(\[,?:+\-*/%!<>=&|^~]""")
        val OPEN_BRACKETS = setOf('(', '[', '{')
        val CLOSE_BRACKETS = mapOf(')' to '(', ']' to '[', '}' to '{')
        val MEMBER_BOUNDARIES = setOf(
            ',', '?', ':', '+', '-', '*', '/', '%', '!', '=', '<', '>', '&', '|', '^', '~'
        )
    }
}
