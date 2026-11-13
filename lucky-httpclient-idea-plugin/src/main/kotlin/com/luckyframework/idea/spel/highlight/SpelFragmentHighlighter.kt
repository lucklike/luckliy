package com.luckyframework.idea.spel.highlight

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.lexer.SpelSyntaxParser
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType

/**
 * 把扫描出的 SpEL 片段渲染为高亮与错误标注。
 *
 * Java 字符串字面量与 YAML 标量共用本实现；[literalText] 是承载片段的完整
 * 元素文本（含引号），[base] 是该元素在文件中的起始偏移。语法错误来自
 * [SpelSyntaxParser]（按官方 SpEL 语法规则），扫描阶段的结构问题仍由
 * [SpelFragment.problems] 报告。
 */
class SpelFragmentHighlighter(
    private val tokenizer: SpelTokenizer = SpelTokenizer(),
    private val syntax: SpelSyntaxParser = SpelSyntaxParser()
) {

    fun annotate(
        holder: AnnotationHolder,
        literalText: String,
        base: Int,
        fragments: List<SpelFragment>
    ) {
        for (fragment in fragments) {
            annotateFragment(holder, literalText, base, fragment)
        }
    }

    private fun annotateFragment(
        holder: AnnotationHolder,
        literalText: String,
        base: Int,
        fragment: SpelFragment
    ) {
        highlightDelimiter(holder, base + fragment.textRange.startOffset, 2)
        if (fragment.closed) {
            highlightDelimiter(holder, base + fragment.textRange.endOffset - 1, 1)
        }

        val tokens = tokenizer.tokenize(literalText, fragment)
        val elementBrackets = elementBracketFlags(tokens)
        for ((index, token) in tokens.withIndex()) {
            val attributes = attributesOf(token.type, tokens, index, elementBrackets) ?: continue
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(base + token.range.startOffset, base + token.range.endOffset))
                .textAttributes(attributes)
                .create()
        }

        for (problem in fragment.problems + syntax.problemsOf(literalText, fragment)) {
            holder.newAnnotation(HighlightSeverity.ERROR, problem.message)
                .range(
                    TextRange(
                        base + problem.range.startOffset,
                        base + problem.range.endOffset
                    )
                )
                .create()
        }

        for (child in fragment.children) {
            annotateFragment(holder, literalText, base, child)
        }
    }

    private fun highlightDelimiter(holder: AnnotationHolder, start: Int, length: Int) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(TextRange(start, start + length))
            .textAttributes(SpelTextAttributes.DELIMITER)
            .create()
    }

    /**
     * 与选择/投影标记配对的 `]`。
     *
     * `?[`/`^[`/`$[`/`![` 整体按运算符着色；与之配对的右方括号必须同色，
     * 否则 `users.?[age > 1]` 的开括号是运算符色、闭括号却是括号色。
     * 普通下标（`list[0]`）不在此列，仍由括号色统一着色。
     */
    private fun elementBracketFlags(tokens: List<SpelToken>): BooleanArray {
        val flags = BooleanArray(tokens.size)
        val stack = ArrayList<Boolean>()
        for ((index, token) in tokens.withIndex()) {
            when (token.type) {
                SpelTokenType.SELECTION, SpelTokenType.PROJECTION -> stack += true
                SpelTokenType.LBRACKET -> stack += false
                SpelTokenType.RBRACKET -> if (stack.isNotEmpty()) {
                    flags[index] = stack.removeAt(stack.lastIndex)
                }

                else -> Unit
            }
        }
        return flags
    }

    /**
     * token 类型到文本属性的映射。
     *
     * `.` 之后的标识符按后一个 token 区分：`(` 前是函数成员，否则是属性访问；
     * 词法层保持 IDENTIFIER 不变，避免影响成员引用的收集。
     */
    private fun attributesOf(
        type: SpelTokenType,
        tokens: List<SpelToken>,
        index: Int,
        elementBrackets: BooleanArray
    ): TextAttributesKey? = when (type) {
        SpelTokenType.VARIABLE -> SpelTextAttributes.VARIABLE
        SpelTokenType.ROOT_VARIABLE -> SpelTextAttributes.ROOT_VARIABLE
        SpelTokenType.FUNCTION, SpelTokenType.NAMESPACE -> SpelTextAttributes.FUNCTION
        SpelTokenType.IDENTIFIER ->
            if (tokens.getOrNull(index + 1)?.type == SpelTokenType.LPAREN) {
                SpelTextAttributes.FUNCTION
            } else {
                SpelTextAttributes.PROPERTY
            }

        SpelTokenType.TYPE_REFERENCE -> SpelTextAttributes.TYPE
        SpelTokenType.BEAN -> SpelTextAttributes.BEAN
        SpelTokenType.STRING -> SpelTextAttributes.STRING
        SpelTokenType.NUMBER -> SpelTextAttributes.NUMBER
        SpelTokenType.OPERATOR -> SpelTextAttributes.OPERATOR
        SpelTokenType.SELECTION, SpelTokenType.PROJECTION -> SpelTextAttributes.OPERATOR
        SpelTokenType.KEYWORD -> SpelTextAttributes.KEYWORD
        SpelTokenType.RBRACKET -> if (elementBrackets[index]) {
            SpelTextAttributes.OPERATOR
        } else {
            SpelTextAttributes.PARENTHESES
        }

        SpelTokenType.LPAREN, SpelTokenType.RPAREN,
        SpelTokenType.LBRACKET -> SpelTextAttributes.PARENTHESES
        else -> null
    }
}
