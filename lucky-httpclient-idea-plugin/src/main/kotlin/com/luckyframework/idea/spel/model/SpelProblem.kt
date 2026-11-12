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

    /** 片段结束时仍有未闭合的 `(`/`[` */
    UNBALANCED_BRACKETS,

    /** 多余的 `)`/`]`，或与最近的左括号类型不一致 */
    UNEXPECTED_CLOSING_BRACKET,

    /** 函数调用中缺少参数表达式（前导、连续或结尾逗号） */
    EMPTY_ARGUMENT,

    /** `.`/`?.` 后缺少成员名，或 `.` 前缺少表达式 */
    MISSING_MEMBER_NAME,

    /** 其余不符合官方 SpEL 语法的问题（多余表达式、缺少操作数、三目缺冒号等） */
    SYNTAX_ERROR
}

data class SpelProblem(
    val kind: SpelProblemKind,
    val range: TextRange,
    val message: String
)
