package com.luckyframework.idea.spel.highlight

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import com.luckyframework.idea.spel.LuckyIcons
import javax.swing.Icon

/**
 * 颜色设置页：分别配置 14 类 SpEL 语法元素的文本样式（design D8）。
 */
class SpelColorSettingsPage : ColorSettingsPage {

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "Lucky HttpClient SpEL"

    override fun getIcon(): Icon = LuckyIcons.Logo

    override fun getHighlighter(): SyntaxHighlighter = PlainSyntaxHighlighter()

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getDemoText(): String = """
        // 请求阶段：Root 变量、函数、属性、字符串、数字与运算符
        @Get("http://#{#host}/api/#{#id}?t=#{#time() + #offset}&name=#{#user.name + 'abc' + 1}")
        User query(@Param("id") String keyword);

        // 响应阶段：响应变量
        @Retryable(condition = "#{${'$'}status${'$'} >= 500}")
        User retry();

        // 类型、Bean 与关键字
        #{T(java.util.ArrayList).class.name == @userMapper.toString() and true}

        // 集合过滤 ?[]/^[]/$[] 与映射 ![]
        #{users.?[active].![name] + users.^[active].$[active]}
    """.trimIndent()

    companion object {

        private val DESCRIPTORS: Array<AttributesDescriptor> = arrayOf(
            AttributesDescriptor("SpEL variable", SpelTextAttributes.VARIABLE),
            AttributesDescriptor("SpEL function", SpelTextAttributes.FUNCTION),
            AttributesDescriptor("SpEL property", SpelTextAttributes.PROPERTY),
            AttributesDescriptor("SpEL root variable", SpelTextAttributes.ROOT_VARIABLE),
            AttributesDescriptor("SpEL type reference", SpelTextAttributes.TYPE),
            AttributesDescriptor("SpEL bean reference", SpelTextAttributes.BEAN),
            AttributesDescriptor("SpEL string", SpelTextAttributes.STRING),
            AttributesDescriptor("SpEL number", SpelTextAttributes.NUMBER),
            AttributesDescriptor("SpEL operator", SpelTextAttributes.OPERATOR),
            AttributesDescriptor("SpEL selection marker", SpelTextAttributes.SELECTION),
            AttributesDescriptor("SpEL projection marker", SpelTextAttributes.PROJECTION),
            AttributesDescriptor("SpEL keyword", SpelTextAttributes.KEYWORD),
            AttributesDescriptor("SpEL delimiter", SpelTextAttributes.DELIMITER),
            AttributesDescriptor("SpEL parentheses", SpelTextAttributes.PARENTHESES)
        )
    }
}
