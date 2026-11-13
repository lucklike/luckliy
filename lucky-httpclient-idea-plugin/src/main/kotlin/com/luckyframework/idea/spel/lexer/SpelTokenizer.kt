package com.luckyframework.idea.spel.lexer

import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType

/**
 * 最小 SpEL 词法分析器。
 *
 * 只负责把片段内容切成带源码偏移的 token，不校验语义：
 * - `#name` 按后一个字符分为函数（`(`）、命名空间（`.`）与普通变量；
 * - `$name$` 形式的响应变量整体识别为 Root 变量；
 * - 裸标识符在 `.` 之后视为属性访问，后跟 `(` 视为函数调用，否则视为 Root 变量；
 * - `new` 之后的裸类型名与 `instanceof` 之后的 `T(...)` 按类型引用着色；
 * - `T(...)` 整体作为类型引用；
 * - `?[`/`^[`/`$[` 与 `![` 紧邻时整体识别为选择/投影标记，
 *   标记与 `[` 之间的空白不成立（`? [` 不是选择表达式）；
 * - 未知字符降级为标识符/运算符，不产生错误。
 *
 * token 范围相对所在字符串字面量的源码文本。
 */
class SpelTokenizer {

    fun tokenize(literalText: String, fragment: SpelFragment): List<SpelToken> {
        val from = fragment.expressionRange.startOffset.coerceIn(0, literalText.length)
        val to = fragment.expressionRange.endOffset.coerceIn(from, literalText.length)
        val tokens = ArrayList<SpelToken>()
        var index = from
        var afterDot = false
        var afterNew = false
        var afterInstanceof = false
        // `new java.util.ArrayList<String>` 的参数内部仍属于类型上下文
        var typeArgumentDepth = 0

        while (index < to) {
            val current = literalText[index]
            when {
                current.isWhitespace() -> index++

                current == '\'' || current == '"' -> {
                    val end = readString(literalText, index, to)
                    tokens += token(SpelTokenType.STRING, literalText, index, end)
                    index = end
                    afterDot = false
                    afterNew = false
                    afterInstanceof = false
                }

                // 选择/投影标记：`?[`/`^[`/`$[` 与 `![` 必须紧邻，
                // `$body$[0]` 中的 `$` 已先被识别为 Root 变量，不会误判
                current in SELECTION_MARKERS && index + 1 < to && literalText[index + 1] == '[' -> {
                    val type = if (current == '!') {
                        SpelTokenType.PROJECTION
                    } else {
                        SpelTokenType.SELECTION
                    }
                    tokens += token(type, literalText, index, index + 2)
                    index += 2
                    afterDot = false
                    afterNew = false
                    afterInstanceof = false
                }

                current == '$' -> {
                    // `$body$`、`$status$` 是响应变量，整体识别避免拆成 `$`+`body`+`$`
                    val end = readIdentifier(literalText, index + 1, to)
                    if (end > index + 1) {
                        val tail = if (end < to && literalText[end] == '$') end + 1 else end
                        tokens += token(SpelTokenType.ROOT_VARIABLE, literalText, index, tail)
                        index = tail
                    } else {
                        tokens += token(SpelTokenType.IDENTIFIER, literalText, index, index + 1)
                        index++
                    }
                    afterDot = false
                    afterNew = false
                    afterInstanceof = false
                }

                current == '#' -> {
                    val end = readIdentifier(literalText, index + 1, to)
                    if (end == index + 1) {
                        tokens += token(SpelTokenType.VARIABLE, literalText, index, end)
                        index = end
                        afterDot = false
                        afterNew = false
                        afterInstanceof = false
                        continue
                    }
                    val next = nextSignificant(literalText, end, to)
                    val type = when {
                        next >= 0 && literalText[next] == '(' -> SpelTokenType.FUNCTION
                        next >= 0 && literalText[next] == '.' -> SpelTokenType.NAMESPACE
                        else -> SpelTokenType.VARIABLE
                    }
                    tokens += token(type, literalText, index, end)
                    index = end
                    afterDot = false
                    afterNew = false
                    afterInstanceof = false
                }

                current == '@' -> {
                    val end = readIdentifier(literalText, index + 1, to)
                    tokens += token(SpelTokenType.BEAN, literalText, index, maxOf(end, index + 1))
                    index = maxOf(end, index + 1)
                    afterDot = false
                    afterNew = false
                    afterInstanceof = false
                }

                current.isDigit() -> {
                    val end = readNumber(literalText, index, to)
                    tokens += token(SpelTokenType.NUMBER, literalText, index, end)
                    index = end
                    afterDot = false
                    afterNew = false
                    afterInstanceof = false
                }

                current.isJavaIdentifierStart() -> {
                    val end = readIdentifier(literalText, index, to)
                    val word = literalText.substring(index, end)
                    if (word == "T" && nextSignificant(literalText, end, to).let {
                            it >= 0 && literalText[it] == '('
                        }
                    ) {
                        val typeEnd = readTypeReference(literalText, index, end, to)
                        tokens += token(SpelTokenType.TYPE_REFERENCE, literalText, index, typeEnd)
                        index = typeEnd
                        afterDot = false
                        afterNew = false
                        afterInstanceof = false
                        continue
                    }
                    val next = nextSignificant(literalText, end, to)
                    val type = when {
                        // `new`/`instanceof` 之后的点链（`new java.util.ArrayList`）保持类型上下文
                        afterNew || afterInstanceof -> SpelTokenType.TYPE_REFERENCE
                        // `.` 之后的单词始终是成员名：`eq`/`ge`/`and` 等文本运算符
                        // 也是合法属性名（`#{add(1, 3).ge}` 是成员访问，不是比较）
                        afterDot -> SpelTokenType.IDENTIFIER
                        word in KEYWORDS -> {
                            if (word == "new") {
                                afterNew = true
                            }
                            if (word == "instanceof") {
                                afterInstanceof = true
                            }
                            SpelTokenType.KEYWORD
                        }

                        next >= 0 && literalText[next] == '(' -> SpelTokenType.FUNCTION
                        else -> SpelTokenType.ROOT_VARIABLE
                    }
                    tokens += token(type, literalText, index, end)
                    index = end
                    afterDot = false
                }

                else -> {
                    val (type, end) = readOperatorOrPunctuation(literalText, index, to)
                    tokens += token(type, literalText, index, end)
                    // `?.` 安全导航后的标识符同样是成员访问
                    afterDot = type == SpelTokenType.DOT ||
                        (type == SpelTokenType.OPERATOR && literalText.substring(index, end) == "?.")
                    val operator = literalText.substring(index, end)
                    // 点链（`new java.util.ArrayList`）与泛型实参保持类型上下文
                    when {
                        afterNew && operator == "<" -> typeArgumentDepth++
                        typeArgumentDepth > 0 && operator == ">" -> typeArgumentDepth--
                        typeArgumentDepth > 0 && operator == "," -> Unit
                        type == SpelTokenType.DOT -> Unit
                        else -> {
                            afterNew = false
                            afterInstanceof = false
                            typeArgumentDepth = 0
                        }
                    }
                    index = end
                }
            }
        }

        return tokens
    }

    private fun token(type: SpelTokenType, text: String, start: Int, end: Int): SpelToken =
        SpelToken(type, TextRange(start, end), text.substring(start, end))

    private fun readString(text: String, start: Int, limit: Int): Int {
        val quote = text[start]
        var cursor = start + 1
        while (cursor < limit) {
            when {
                text[cursor] == '\\' -> cursor += 2
                text[cursor] == quote -> return cursor + 1
                else -> cursor++
            }
        }
        return limit
    }

    private fun readIdentifier(text: String, start: Int, limit: Int): Int {
        var cursor = start
        while (cursor < limit && text[cursor].isJavaIdentifierPart()) {
            cursor++
        }
        return cursor
    }

    private fun readNumber(text: String, start: Int, limit: Int): Int {
        var cursor = start
        while (cursor < limit && (text[cursor].isDigit() || text[cursor] == '.')) {
            cursor++
        }
        if (cursor < limit && (text[cursor] == 'L' || text[cursor] == 'l' ||
                    text[cursor] == 'F' || text[cursor] == 'f' ||
                    text[cursor] == 'D' || text[cursor] == 'd')
        ) {
            cursor++
        }
        return cursor
    }

    private fun readTypeReference(text: String, start: Int, afterName: Int, limit: Int): Int {
        var depth = 0
        var cursor = afterName
        while (cursor < limit) {
            when (text[cursor]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) {
                        return cursor + 1
                    }
                }
            }
            cursor++
        }
        return limit
    }

    private fun nextSignificant(text: String, start: Int, limit: Int): Int {
        var cursor = start
        while (cursor < limit && text[cursor].isWhitespace()) {
            cursor++
        }
        return if (cursor < limit) cursor else -1
    }

    private fun readOperatorOrPunctuation(text: String, start: Int, limit: Int): Pair<SpelTokenType, Int> {
        val two = if (start + 1 < limit) text.substring(start, start + 2) else ""
        if (two in TWO_CHAR_OPERATORS) {
            return SpelTokenType.OPERATOR to start + 2
        }
        return when (text[start]) {
            '.' -> SpelTokenType.DOT to start + 1
            ',' -> SpelTokenType.COMMA to start + 1
            '(' -> SpelTokenType.LPAREN to start + 1
            ')' -> SpelTokenType.RPAREN to start + 1
            '[' -> SpelTokenType.LBRACKET to start + 1
            ']' -> SpelTokenType.RBRACKET to start + 1
            ':' -> SpelTokenType.COLON to start + 1
            '?' -> SpelTokenType.QUESTION to start + 1
            '+', '-', '*', '/', '%', '!', '=', '<', '>', '&', '|', '^', '~' ->
                SpelTokenType.OPERATOR to start + 1

            else -> SpelTokenType.IDENTIFIER to start + 1
        }
    }

    private companion object {
        val KEYWORDS = setOf(
            "true", "false", "null", "new",
            "and", "or", "not", "eq", "ne", "lt", "le", "gt", "ge",
            "div", "mod", "instanceof", "matches", "between"
        )
        val TWO_CHAR_OPERATORS = setOf(
            "==", "!=", "<=", ">=", "&&", "||", "?:", "?.", "**", "::"
        )

        /** 选择/投影的标记字符；`?` 另有 `QUESTION` 与 `?.`/`?:` 的用途。 */
        val SELECTION_MARKERS = setOf('?', '!', '^', '$')
    }
}
