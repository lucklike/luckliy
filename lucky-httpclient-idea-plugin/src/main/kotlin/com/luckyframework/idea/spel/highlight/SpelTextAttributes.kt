package com.luckyframework.idea.spel.highlight

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

/**
 * SpEL 高亮使用的文本属性键（design D8）。
 *
 * 所有键都带平台默认颜色作为 fallback；`${...}` 三个键的具体样式由
 * `colorSettings/Default.xml`、`colorSettings/Darcula.xml` 通过
 * additionalTextAttributes 注册，未注册的主题沿用 fallback 样式。
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

    /** 集合过滤（选择）标记 `?[`/`^[`/`$[` 及其配对 `]`。 */
    val SELECTION: TextAttributesKey = key("LUCKY_SPEL_SELECTION", DefaultLanguageHighlighterColors.KEYWORD)

    /** 集合映射（投影）标记 `![` 及其配对 `]`。 */
    val PROJECTION: TextAttributesKey = key("LUCKY_SPEL_PROJECTION", DefaultLanguageHighlighterColors.KEYWORD)

    val KEYWORD: TextAttributesKey = key("LUCKY_SPEL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)

    val DELIMITER: TextAttributesKey = key("LUCKY_SPEL_DELIMITER", DefaultLanguageHighlighterColors.BRACES)

    val PARENTHESES: TextAttributesKey = key("LUCKY_SPEL_PARENTHESES", DefaultLanguageHighlighterColors.PARENTHESES)

    /** Spring 环境变量占位符 `${key}` 的键名：米色文字 + 灰色下划线；未注册主题回退到字段样式。 */
    val SPRING_PLACEHOLDER: TextAttributesKey = key(
        "LUCKY_SPRING_PLACEHOLDER",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD
    )

    /** `${key:default}` 中的默认值：与字符串字面量同色；未注册主题回退到字符串样式。 */
    val SPRING_PLACEHOLDER_DEFAULT: TextAttributesKey = key(
        "LUCKY_SPRING_PLACEHOLDER_DEFAULT",
        DefaultLanguageHighlighterColors.STRING
    )

    /** `${`、`}` 与键值分隔 `:`：关键字橙；未注册主题回退到关键字样式。 */
    val SPRING_PLACEHOLDER_DELIMITER: TextAttributesKey = key(
        "LUCKY_SPRING_PLACEHOLDER_DELIMITER",
        DefaultLanguageHighlighterColors.KEYWORD
    )

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
        SELECTION,
        PROJECTION,
        KEYWORD,
        DELIMITER,
        PARENTHESES,
        SPRING_PLACEHOLDER,
        SPRING_PLACEHOLDER_DEFAULT,
        SPRING_PLACEHOLDER_DELIMITER
    )

    private fun key(externalName: String, fallback: TextAttributesKey): TextAttributesKey =
        TextAttributesKey.createTextAttributesKey(externalName, fallback)
}
