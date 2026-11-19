package com.luckyframework.idea.spel.highlight

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType

/**
 * SpEL token → 高亮属性。
 *
 * 编辑器高亮与侧边栏 path 富文本共用本映射，避免两处着色规则漂移。
 * token 范围与传入顺序保持一致，返回列表与 [tokens] 一一对应。
 */
internal object SpelTokenAttributes {

    fun attributesOf(tokens: List<SpelToken>): List<TextAttributesKey?> {
        val elementBrackets = elementBrackets(tokens)
        return tokens.mapIndexed { index, token ->
            attributesOf(token.type, tokens, index, elementBrackets)
        }
    }

    /**
     * 与选择/投影标记配对的 `]` 的高亮属性。
     *
     * 过滤标记 `?[`/`^[`/`$[` 与配对 `]` 使用 [SpelTextAttributes.SELECTION]，
     * 映射标记 `![` 与配对 `]` 使用 [SpelTextAttributes.PROJECTION]，
     * 否则 `users.?[age > 1]` 的开括号与闭括号颜色不一致。
     * 普通下标（`list[0]`）不在此列，仍由括号色统一着色。
     */
    private fun elementBrackets(tokens: List<SpelToken>): Array<TextAttributesKey?> {
        val attributes = arrayOfNulls<TextAttributesKey>(tokens.size)
        val stack = ArrayList<TextAttributesKey?>()
        for ((index, token) in tokens.withIndex()) {
            when (token.type) {
                SpelTokenType.SELECTION -> stack += SpelTextAttributes.SELECTION
                SpelTokenType.PROJECTION -> stack += SpelTextAttributes.PROJECTION
                SpelTokenType.LBRACKET -> stack += null
                SpelTokenType.RBRACKET -> if (stack.isNotEmpty()) {
                    attributes[index] = stack.removeAt(stack.lastIndex)
                }

                else -> Unit
            }
        }
        return attributes
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
        elementBrackets: Array<TextAttributesKey?>
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
        SpelTokenType.SELECTION -> SpelTextAttributes.SELECTION
        SpelTokenType.PROJECTION -> SpelTextAttributes.PROJECTION
        SpelTokenType.KEYWORD -> SpelTextAttributes.KEYWORD
        SpelTokenType.RBRACKET -> elementBrackets[index] ?: SpelTextAttributes.PARENTHESES

        SpelTokenType.LPAREN, SpelTokenType.RPAREN,
        SpelTokenType.LBRACKET -> SpelTextAttributes.PARENTHESES
        else -> null
    }
}
