package com.luckyframework.idea.spel.model

import com.intellij.openapi.util.TextRange

/**
 * SpEL 词法单元类型。
 */
enum class SpelTokenType {
    VARIABLE,
    ROOT_VARIABLE,
    FUNCTION,
    NAMESPACE,
    TYPE_REFERENCE,
    BEAN,
    STRING,
    NUMBER,
    OPERATOR,
    KEYWORD,
    IDENTIFIER,
    DOT,
    COMMA,
    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    COLON,
    QUESTION,

    /** 集合选择标记 `?[`/`^[`/`$[`，方括号紧随标记 */
    SELECTION,

    /** 集合投影标记 `![`，方括号紧随标记 */
    PROJECTION
}

/**
 * 一个词法单元，范围相对于所在字符串字面量的源码文本。
 */
data class SpelToken(
    val type: SpelTokenType,
    val range: TextRange,
    val text: String
)
