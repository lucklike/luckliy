package com.luckyframework.idea.spel.model

import com.intellij.openapi.util.TextRange

/**
 * 扫描阶段能确定的基础语法问题。
 *
 * 范围相对所在字符串字面量的源码文本（包含起始引号）。
 */
enum class SpelProblemKind {
    UNCLOSED_EXPRESSION,
    UNCLOSED_STRING,
    UNBALANCED_BRACKETS
}

data class SpelProblem(
    val kind: SpelProblemKind,
    val range: TextRange,
    val message: String
)
