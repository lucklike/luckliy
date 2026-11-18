package com.luckyframework.idea.spel.lexer

import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.environment.SpringPlaceholderParser
import com.luckyframework.idea.spel.model.SpelFragment

/**
 * 语法分析使用的词法单元类型。
 *
 * 与着色用的 [SpelTokenType] 独立：这里严格按 Spring 官方 SpEL Tokenizer 的
 * token 划分，只服务于语法校验，不影响引用收集与补全。
 */
enum class SpelSyntaxTokenType {
    IDENTIFIER,

    /** `#name` 变量或函数引用 */
    VARIABLE,

    /** `@name` 或 `@'name'` Bean 引用 */
    BEAN,

    STRING,
    NUMBER,
    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    LCURLY,
    RCURLY,
    DOT,

    /** 安全导航 `?.` */
    SAFE_NAVI,

    COMMA,
    COLON,
    QUESTION,

    /** Elvis 运算符 `?:` */
    ELVIS,

    /** 集合投影 `![` */
    PROJECTION,

    /** 集合选择 `?[` */
    SELECTION,

    /** 集合第一个选择 `^[` */
    SELECT_FIRST,

    /** 集合最后一个选择 `$[` */
    SELECT_LAST,

    /** 其余运算符，文本保留 */
    OPERATOR,

    /** 无法识别的字符 */
    UNKNOWN
}

data class SpelSyntaxToken(
    val type: SpelSyntaxTokenType,
    val text: String,
    val range: TextRange
)

/**
 * 按官方 SpEL Tokenizer 规则切分表达式内容。
 *
 * 与官方一致的几个关键点：
 * - `![`、`?[`、`^[`、`$[` 是投影/选择标记，`?.`、`?:` 是安全导航与 Elvis；
 * - `$name$`、`$` 属于 Java 标识符字符，`#name`、`@bean` 是独立引用；
 * - 支持整数、长整型、十六进制、实数、科学计数法与类型后缀；
 * - 字符串支持单/双引号与反斜杠转义，单引号内 `''` 表示一个单引号。
 *
 * 框架扩展：嵌套的 `#{...}` 子片段在父表达式中是内层求值结果的占位，
 * 整体作为一个操作数，由子片段自己递归校验；参数类型组合
 * `name::type`、`$n::type` 是框架注册的变量名，同样整体作为一个操作数。
 *
 * token 范围相对所在字符串字面量的源码文本，与 [SpelFragment.expressionRange] 同一坐标系。
 */
class SpelSyntaxLexer {

    /**
     * 切分表达式内容。
     *
     * 双引号字面量先在还原 Java 转义后的值文本上切分，再把 token 范围映射回
     * 源码偏移；否则源码中的 `\"` 会被误当成普通反斜杠。
     */
    fun lex(sourceText: String, fragment: SpelFragment): List<SpelSyntaxToken> {
        val source = JavaStringEscapeMapper.sourceOf(
            sourceText,
            fragment.expressionRange,
            fragment.valueRange
        )
        val value = source.text
        val from = source.expressionRange.startOffset.coerceIn(0, value.length)
        val to = source.expressionRange.endOffset.coerceIn(from, value.length)
        val children = fragment.children
            .map { it.valueTextRange }
            .filter { it.startOffset >= from && it.endOffset <= to }
            .sortedBy { it.startOffset }
        val tokens = ArrayList<SpelSyntaxToken>()
        var index = from
        var childIndex = 0
        while (index < to) {
            while (childIndex < children.size && children[childIndex].endOffset <= index) {
                childIndex++
            }
            val child = children.getOrNull(childIndex)
            if (child != null && child.startOffset == index) {
                tokens += token(SpelSyntaxTokenType.IDENTIFIER, value, index, child.endOffset)
                index = child.endOffset
                childIndex++
                continue
            }
            val current = value[index]
            val next = if (index + 1 < to) value[index + 1] else NONE
            when {
                current.isWhitespace() -> index++

                current == '\'' || current == '"' -> {
                    val end = endOfString(value, index, to)
                    tokens += token(SpelSyntaxTokenType.STRING, value, index, end)
                    index = end
                }

                current.isDigit() -> {
                    val end = endOfNumber(value, index, to)
                    tokens += token(SpelSyntaxTokenType.NUMBER, value, index, end)
                    index = end
                }

                // `${...}` 在 SpEL 求值前由 Spring 环境变量替换，整体是一个操作数；
                // 运行期替换结果可能是数字、布尔或 Bean 名，语法层按标识符处理。
                // 未闭合时吞掉剩余内容，错误由环境变量高亮单独报告，避免重复诊断。
                current == '$' && next == '{' -> {
                    val end = SpringPlaceholderParser.findEnd(value, index + 2, to)
                    val tokenEnd = if (end > index) end else to
                    tokens += token(SpelSyntaxTokenType.IDENTIFIER, value, index, tokenEnd)
                    index = tokenEnd
                }

                // `$[` 是最后一个元素选择，`$name$` 是标识符
                current == '$' && next == '[' -> {
                    tokens += token(SpelSyntaxTokenType.SELECT_LAST, value, index, index + 2)
                    index += 2
                }

                current.isJavaIdentifierStart() -> {
                    val end = endOfTypeCombination(
                        value,
                        endOfIdentifier(value, index, to),
                        to
                    )
                    tokens += token(SpelSyntaxTokenType.IDENTIFIER, value, index, end)
                    index = end
                }

                current == '#' -> {
                    val nameEnd = endOfIdentifier(value, index + 1, to)
                    val end = if (nameEnd > index + 1) {
                        endOfTypeCombination(value, nameEnd, to)
                    } else {
                        nameEnd
                    }
                    val tokenEnd = if (end > index + 1) end else index + 1
                    tokens += token(SpelSyntaxTokenType.VARIABLE, value, index, tokenEnd)
                    index = tokenEnd
                }

                current == '@' -> {
                    var end = endOfIdentifier(value, index + 1, to)
                    if (end == index + 1 && end < to && (value[end] == '\'' || value[end] == '"')) {
                        end = endOfString(value, end, to)
                    }
                    val tokenEnd = if (end > index + 1) end else index + 1
                    tokens += token(SpelSyntaxTokenType.BEAN, value, index, tokenEnd)
                    index = tokenEnd
                }

                current == '.' -> {
                    tokens += token(SpelSyntaxTokenType.DOT, value, index, index + 1)
                    index++
                }

                current == '?' -> when (next) {
                    '[' -> {
                        tokens += token(SpelSyntaxTokenType.SELECTION, value, index, index + 2)
                        index += 2
                    }

                    '.' -> {
                        tokens += token(SpelSyntaxTokenType.SAFE_NAVI, value, index, index + 2)
                        index += 2
                    }

                    ':' -> {
                        tokens += token(SpelSyntaxTokenType.ELVIS, value, index, index + 2)
                        index += 2
                    }

                    else -> {
                        tokens += token(SpelSyntaxTokenType.QUESTION, value, index, index + 1)
                        index++
                    }
                }

                current == '[' -> {
                    tokens += token(SpelSyntaxTokenType.LBRACKET, value, index, index + 1)
                    index++
                }

                current == ']' -> {
                    tokens += token(SpelSyntaxTokenType.RBRACKET, value, index, index + 1)
                    index++
                }

                current == '{' -> {
                    tokens += token(SpelSyntaxTokenType.LCURLY, value, index, index + 1)
                    index++
                }

                current == '}' -> {
                    tokens += token(SpelSyntaxTokenType.RCURLY, value, index, index + 1)
                    index++
                }

                current == '(' -> {
                    tokens += token(SpelSyntaxTokenType.LPAREN, value, index, index + 1)
                    index++
                }

                current == ')' -> {
                    tokens += token(SpelSyntaxTokenType.RPAREN, value, index, index + 1)
                    index++
                }

                current == ',' -> {
                    tokens += token(SpelSyntaxTokenType.COMMA, value, index, index + 1)
                    index++
                }

                current == ':' -> {
                    tokens += token(SpelSyntaxTokenType.COLON, value, index, index + 1)
                    index++
                }

                current == '!' && next == '[' -> {
                    tokens += token(SpelSyntaxTokenType.PROJECTION, value, index, index + 2)
                    index += 2
                }

                current == '^' && next == '[' -> {
                    tokens += token(SpelSyntaxTokenType.SELECT_FIRST, value, index, index + 2)
                    index += 2
                }

                current == '+' || current == '-' || current == '*' ||
                    current == '/' || current == '%' || current == '!' ||
                    current == '^' || current == '=' || current == '<' ||
                    current == '>' || current == '&' || current == '|' ||
                    current == '~' -> {
                    val length = if (index + 1 < to && (current to next) in TWO_CHAR_OPERATORS) 2 else 1
                    tokens += token(SpelSyntaxTokenType.OPERATOR, value, index, index + length)
                    index += length
                }

                else -> {
                    tokens += token(SpelSyntaxTokenType.UNKNOWN, value, index, index + 1)
                    index++
                }
            }
        }
        return tokens.map {
            SpelSyntaxToken(
                it.type,
                it.text,
                TextRange(
                    source.sourceOffset(it.range.startOffset),
                    source.sourceOffset(it.range.endOffset)
                )
            )
        }
    }

    private fun token(type: SpelSyntaxTokenType, text: String, start: Int, end: Int): SpelSyntaxToken =
        SpelSyntaxToken(type, text.substring(start, end), TextRange(start, end))

    private fun endOfString(text: String, start: Int, limit: Int): Int {
        val quote = text[start]
        var index = start + 1
        while (index < limit) {
            when {
                text[index] == '\\' -> index += 2
                text[index] == quote -> {
                    // 单引号字符串内连续两个单引号表示一个单引号字符
                    if (quote == '\'' && index + 1 < limit && text[index + 1] == '\'') {
                        index += 2
                    } else {
                        return index + 1
                    }
                }

                else -> index++
            }
        }
        return limit
    }

    private fun endOfIdentifier(text: String, start: Int, limit: Int): Int {
        var index = start
        while (index < limit && text[index].isJavaIdentifierPart()) {
            index++
        }
        return index
    }

    /**
     * 紧跟在名称后的 `::type` 是框架参数类型组合的一部分，与名称合并成一个操作数。
     * 只有完整匹配且后面不再跟标识符字符时才合并，避免把 `t::typeX` 当成一个名字。
     */
    private fun endOfTypeCombination(text: String, start: Int, limit: Int): Int {
        val end = start + TYPE_COMBINATION.length
        if (end > limit || !text.regionMatches(start, TYPE_COMBINATION, 0, TYPE_COMBINATION.length)) {
            return start
        }
        return if (end == limit || !text[end].isJavaIdentifierPart()) end else start
    }

    private fun endOfNumber(text: String, start: Int, limit: Int): Int {
        if (text[start] == '0' && start + 1 < limit && (text[start + 1] == 'x' || text[start + 1] == 'X')) {
            var index = start + 2
            while (index < limit && (text[index].isDigit() || text[index].lowercaseChar() in 'a'..'f')) {
                index++
            }
            return endOfNumberSuffix(text, index, limit)
        }
        var index = start
        while (index < limit && text[index].isDigit()) {
            index++
        }
        if (index + 1 < limit && text[index] == '.' && text[index + 1].isDigit()) {
            index++
            while (index < limit && text[index].isDigit()) {
                index++
            }
        }
        if (index < limit && (text[index] == 'e' || text[index] == 'E')) {
            var exponent = index + 1
            if (exponent < limit && (text[exponent] == '+' || text[exponent] == '-')) {
                exponent++
            }
            if (exponent < limit && text[exponent].isDigit()) {
                while (exponent < limit && text[exponent].isDigit()) {
                    exponent++
                }
                index = exponent
            }
        }
        return endOfNumberSuffix(text, index, limit)
    }

    private fun endOfNumberSuffix(text: String, start: Int, limit: Int): Int =
        if (start < limit && text[start] in "lLfFdD") start + 1 else start

    private companion object {
        const val NONE = '\u0000'

        /** 框架参数类型组合后缀：`name::type`、`$n::type`。 */
        const val TYPE_COMBINATION = "::type"

        val TWO_CHAR_OPERATORS = setOf(
            '=' to '=',
            '!' to '=',
            '<' to '=',
            '>' to '=',
            '&' to '&',
            '|' to '|',
            '+' to '+',
            '-' to '-',
            '*' to '*'
        )
    }
}
