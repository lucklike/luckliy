package com.luckyframework.idea.spel.environment

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.highlight.SpelTextAttributes

/**
 * `${...}` 的着色与语法/配置检查。
 *
 * 与 SpEL 片段高亮分开注册：SpEL 词法把 `${...}` 当作一个整体操作数跳过，
 * 不会为内部字符生成 token，因此这里可以安全地覆盖键名、默认值与定界符。
 */
class SpringPlaceholderHighlighter {

    fun annotate(
        holder: AnnotationHolder,
        literalText: String,
        base: Int,
        placeholders: List<SpringPlaceholder>,
        model: SpringEnvironmentModel? = null
    ) {
        for (placeholder in placeholders) {
            highlightDelimiters(holder, literalText, base, placeholder)
            if (!placeholder.closed) {
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "环境变量表达式未闭合，缺少 '}'"
                ).range(absolute(base, placeholder.range)).create()
                continue
            }
            if (placeholder.key.isEmpty()) {
                val errorRange = if (placeholder.keyRange.isEmpty) placeholder.range else placeholder.keyRange
                holder.newAnnotation(
                    HighlightSeverity.ERROR,
                    "环境变量名不能为空"
                ).range(absolute(base, errorRange)).create()
                continue
            }
            checkConfiguration(holder, base, placeholder, model)
        }
    }

    /**
     * 未提供默认值且无法解析的键给出警告。
     *
     * `model.resolve` 覆盖系统属性、环境变量与当前激活 profile 的生效值；
     * 未激活 profile 文件里真实存在的键不按未定义处理，只检查是否为完整键
     * （`spring.datasource` 之类型下有子项，单独使用无效）。
     */
    private fun checkConfiguration(
        holder: AnnotationHolder,
        base: Int,
        placeholder: SpringPlaceholder,
        model: SpringEnvironmentModel?
    ) {
        if (model == null || placeholder.defaultText != null) {
            return
        }
        if (model.resolve(placeholder.key) != null) {
            return
        }
        if (model.definitionsOf(placeholder.key).isNotEmpty()) {
            return
        }
        val children = model.childKeysOf(placeholder.key)
        val message = if (children.isNotEmpty()) {
            "配置键「${placeholder.key}」不是完整的配置键，缺少后续层级（如 ${children.first()}）"
        } else {
            "未定义的配置键「${placeholder.key}」，且未提供默认值"
        }
        holder.newAnnotation(HighlightSeverity.WARNING, message)
            .range(absolute(base, placeholder.keyRange))
            .create()
    }

    private fun highlightDelimiters(
        holder: AnnotationHolder,
        literalText: String,
        base: Int,
        placeholder: SpringPlaceholder
    ) {
        highlight(
            holder,
            base,
            TextRange(placeholder.startOffset, (placeholder.startOffset + 2).coerceAtMost(literalText.length)),
            SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER
        )
        if (placeholder.closed) {
            highlight(
                holder,
                base,
                TextRange((placeholder.endOffset - 1).coerceAtLeast(placeholder.startOffset), placeholder.endOffset),
                SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER
            )
        }
        if (placeholder.key.isNotEmpty()) {
            highlight(holder, base, placeholder.keyRange, SpelTextAttributes.SPRING_PLACEHOLDER)
        }
        val defaultRange = placeholder.defaultRange
        if (defaultRange != null) {
            highlight(
                holder,
                base,
                TextRange(placeholder.keyEndOffset, (placeholder.keyEndOffset + 1).coerceAtMost(literalText.length)),
                SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER
            )
            if (!defaultRange.isEmpty) {
                highlight(holder, base, defaultRange, SpelTextAttributes.SPRING_PLACEHOLDER_DEFAULT)
            }
        }
    }

    private fun highlight(
        holder: AnnotationHolder,
        base: Int,
        range: TextRange,
        attributes: com.intellij.openapi.editor.colors.TextAttributesKey
    ) {
        if (range.isEmpty) {
            return
        }
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(absolute(base, range))
            .textAttributes(attributes)
            .create()
    }

    private fun absolute(base: Int, range: TextRange): TextRange =
        TextRange(base + range.startOffset, base + range.endOffset)
}
