package com.luckyframework.idea.spel.lexer

import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelProblem
import com.luckyframework.idea.spel.model.SpelProblemKind

/**
 * 轻量 SpEL 片段扫描器。
 *
 * 职责：
 * - 识别 `#{` 开头、与花括号配对的 `}` 结束的片段，同一字符串可包含多个片段；
 * - 正确处理嵌套花括号、单/双引号字符串与反斜杠转义；
 * - 识别 `@if(...):`、`@max(n):` 前缀与双反引号嵌套标记（前缀不属于表达式内容）；
 * - 递归识别内容中嵌套的片段，但片段内部字符串字面量里的 `#{` 是
 *   普通文本（`#{'#{Hello World}'}` 只求值一次），不产生子片段；
 * - 产出未闭合表达式、未闭合字符串、括号不配平、多余或类型不一致的
 *   右括号四类基础问题。
 *
 * 实现不依赖 spring-expression，遇到未知语法只降级为普通内容，不产生错误。
 */
class SpelScanner {

    fun scanLiteral(literalText: String): List<SpelFragment> {
        val mapped = JavaStringEscapeMapper.map(literalText)
        return scanValue(mapped, 0, mapped.value.length)
    }

    /**
     * 扫描 `[from, to)` 内的片段。
     *
     * [inExpression] 表示范围是否位于某个 `#{...}` 片段内部：片段内部的
     * 字符串字面量里出现的 `#{` 是普通文本，运行期不会再次求值，
     * 必须跳过，否则 `#{'#{Hello World}'}` 的内层会被误当作子片段。
     * 模板文本（片段外部）中的引号只是普通字符，不参与跳过。
     */
    private fun scanValue(
        mapped: JavaStringEscapeMapper.Mapped,
        from: Int,
        to: Int,
        inExpression: Boolean = false
    ): List<SpelFragment> {
        val value = mapped.value
        val fragments = ArrayList<SpelFragment>()
        var index = from
        var quote: Char? = null

        while (index < to - 1) {
            val current = value[index]
            if (inExpression) {
                if (quote != null) {
                    when {
                        current == '\\' -> index += 2
                        current == quote -> {
                            quote = null
                            index++
                        }

                        else -> index++
                    }
                    continue
                }
                if (current == '\'' || current == '"') {
                    quote = current
                    index++
                    continue
                }
            }
            if (current != '#' || value[index + 1] != '{') {
                index++
                continue
            }

            val contentStart = index + 2
            val match = matchFragment(value, index, to)
            val contentEnd = if (match.closed) match.end else to
            val children = scanValue(mapped, contentStart, contentEnd, inExpression = true)
            val problems = problemsOf(mapped, index, contentEnd, to, match)

            fragments += SpelFragment(
                textRange = TextRange(
                    mapped.sourceOffset(index),
                    if (match.closed) mapped.sourceOffset(match.end) + 1 else mapped.sourceOffset(to)
                ),
                expressionRange = TextRange(
                    mapped.sourceOffset(contentStart),
                    mapped.sourceOffset(contentEnd)
                ),
                valueTextRange = TextRange(
                    index,
                    if (match.closed) match.end + 1 else to
                ),
                valueRange = TextRange(contentStart, contentEnd),
                content = value.substring(contentStart, contentEnd),
                closed = match.closed,
                conditional = hasIfPrefix(value, index, from),
                maxDepth = maxDepthOf(value, index, from),
                nested = isBacktickNested(value, index, match, to),
                children = children,
                problems = problems
            )

            index = if (match.closed) match.end + 1 else to
        }

        return fragments
    }

    private fun problemsOf(
        mapped: JavaStringEscapeMapper.Mapped,
        fragmentStart: Int,
        contentEnd: Int,
        limit: Int,
        match: Match
    ): List<SpelProblem> {
        val problems = ArrayList<SpelProblem>()
        if (!match.closed) {
            problems += SpelProblem(
                SpelProblemKind.UNCLOSED_EXPRESSION,
                TextRange(mapped.sourceOffset(fragmentStart), mapped.sourceOffset(limit)),
                "SpEL 表达式未闭合，缺少匹配的 '}'"
            )
        }
        if (match.unclosedQuote >= 0) {
            problems += SpelProblem(
                SpelProblemKind.UNCLOSED_STRING,
                TextRange(mapped.sourceOffset(match.unclosedQuote), mapped.sourceOffset(contentEnd)),
                "字符串字面量未闭合"
            )
        }
        val closing = match.closingProblem
        if (closing != null) {
            problems += SpelProblem(
                SpelProblemKind.UNEXPECTED_CLOSING_BRACKET,
                TextRange(mapped.sourceOffset(closing.offset), mapped.sourceOffset(closing.offset + 1)),
                closing.message
            )
        } else if (match.unmatchedBracket >= 0) {
            problems += SpelProblem(
                SpelProblemKind.UNBALANCED_BRACKETS,
                TextRange(mapped.sourceOffset(match.unmatchedBracket), mapped.sourceOffset(contentEnd)),
                "圆括号或方括号不配平"
            )
        }
        return problems
    }

    private class Match(
        val closed: Boolean,
        val end: Int,
        val unclosedQuote: Int,
        val unmatchedBracket: Int,
        val closingProblem: ClosingProblem? = null
    )

    /** 一个多余的右括号，或与最近的左括号类型不一致的右括号。 */
    private class ClosingProblem(val offset: Int, val message: String)

    private fun matchFragment(value: String, start: Int, limit: Int): Match {
        var cursor = start
        var depth = 0
        var quote: Char? = null
        var quoteStart = -1
        val brackets = ArrayList<Int>()
        var closingProblem: ClosingProblem? = null

        while (cursor < limit) {
            val current = value[cursor]
            if (quote != null) {
                when {
                    current == '\\' -> {
                        cursor += 2
                        continue
                    }

                    current == quote -> quote = null
                }
                cursor++
                continue
            }
            when (current) {
                '\'', '"' -> {
                    quote = current
                    quoteStart = cursor
                }

                '{' -> depth++

                '}' -> {
                    depth--
                    if (depth == 0) {
                        return Match(
                            closed = true,
                            end = cursor,
                            unclosedQuote = -1,
                            unmatchedBracket = brackets.firstOrNull() ?: -1,
                            closingProblem = closingProblem
                        )
                    }
                }

                '(', '[' -> brackets.add(cursor)

                ')', ']' -> {
                    val expected = if (current == ')') '(' else '['
                    val top = brackets.lastOrNull()
                    val topChar = top?.let { value[it] }
                    when {
                        topChar == null -> if (closingProblem == null) {
                            closingProblem = ClosingProblem(
                                cursor,
                                "多余的 '$current'，缺少对应的 '$expected'"
                            )
                        }

                        topChar != expected -> {
                            if (closingProblem == null) {
                                closingProblem = ClosingProblem(
                                    cursor,
                                    "括号不匹配：'$topChar' 与 '$current' 不配对"
                                )
                            }
                            brackets.removeAt(brackets.size - 1)
                        }

                        else -> brackets.removeAt(brackets.size - 1)
                    }
                }
            }
            cursor++
        }

        return Match(
            closed = false,
            end = limit,
            unclosedQuote = if (quote != null) quoteStart else -1,
            unmatchedBracket = brackets.firstOrNull() ?: -1,
            closingProblem = closingProblem
        )
    }

    private fun hasIfPrefix(value: String, start: Int, lowerBound: Int): Boolean {
        val prefix = value.substring(maxOf(lowerBound, start - 32), start)
        return IF_PREFIX.containsMatchIn(prefix)
    }

    private fun maxDepthOf(value: String, start: Int, lowerBound: Int): Int? {
        val prefix = value.substring(maxOf(lowerBound, start - 32), start)
        val match = MAX_PREFIX.find(prefix) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun isBacktickNested(value: String, start: Int, match: Match, limit: Int): Boolean {
        val lowerBound = maxOf(0, start - 32)
        val prefix = value.substring(lowerBound, start)
        val maxPrefix = MAX_PREFIX.find(prefix)
        // 双反引号可能包裹整个 `@max(n):#{...}`，锚点需要跳过前缀
        val anchor = if (maxPrefix != null) lowerBound + maxPrefix.range.first else start
        val before = anchor > 0 && value[anchor - 1] == '`'
        val after = match.closed && match.end + 1 < limit && value[match.end + 1] == '`'
        return before && after
    }

    private companion object {
        val IF_PREFIX = Regex("""@if\s*\($""")
        val MAX_PREFIX = Regex("""@max\s*\(\s*(\d+)\s*\)\s*:$""")
    }
}
