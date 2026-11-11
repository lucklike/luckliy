package com.luckyframework.idea.spel.highlight

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

/**
 * SpEL 高亮使用的文本属性键（design D8）。
 *
 * 每个键都带平台默认颜色作为 fallback，`colorSettings/Default.xml` 提供可覆盖的默认方案。
 */
object SpelTextAttributes {

    val VARIABLE: TextAttributesKey = key("LUCKY_SPEL_VARIABLE", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)

    val FUNCTION: TextAttributesKey = key("LUCKY_SPEL_FUNCTION", DefaultLanguageHighlighterColors.FUNCTION_CALL)

    val PROPERTY: TextAttributesKey = key("LUCKY_SPEL_PROPERTY", DefaultLanguageHighlighterColors.INSTANCE_FIELD)

    val ROOT_VARIABLE: TextAttributesKey = key("LUCKY_SPEL_ROOT_VARIABLE", DefaultLanguageHighlighterColors.INSTANCE_FIELD)

    val TYPE: TextAttributesKey = key("LUCKY_SPEL_TYPE", DefaultLanguageHighlighterColors.CLASS_NAME)

    val BEAN: TextAttributesKey = key("LUCKY_SPEL_BEAN", DefaultLanguageHighlighterColors.CONSTANT)

    val STRING: TextAttributesKey = key("LUCKY_SPEL_STRING", DefaultLanguageHighlighterColors.STRING)

    val NUMBER: TextAttributesKey = key("LUCKY_SPEL_NUMBER", DefaultLanguageHighlighterColors.NUMBER)

    val OPERATOR: TextAttributesKey = key("LUCKY_SPEL_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)

    val KEYWORD: TextAttributesKey = key("LUCKY_SPEL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    val DELIMITER: TextAttributesKey = key("LUCKY_SPEL_DELIMITER", DefaultLanguageHighlighterColors.BRACES)

    val PARENTHESES: TextAttributesKey = key("LUCKY_SPEL_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)

    fun all(): List<TextAttributesKey> = listOf(
        VARIABLE,
        FUNCTION,
        PROPERTY,
        ROOT_VARIABLE,
        TYPE,
        BEAN,
        STRING,
        NUMBER,
        OPERATOR,
        KEYWORD,
        DELIMITER,
        PARENTHESES
    )

    private fun key(externalName: String, fallback: TextAttributesKey): TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(externalName, fallback)
}
