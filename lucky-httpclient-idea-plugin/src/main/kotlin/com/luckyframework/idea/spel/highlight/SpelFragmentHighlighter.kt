package com.luckyframework.idea.spel.highlight

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.lexer.SpelSyntaxParser
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelFragment

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
        val attributes = SpelTokenAttributes.attributesOf(tokens)
        for ((index, token) in tokens.withIndex()) {
            val key = attributes[index] ?: continue
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(base + token.range.startOffset, base + token.range.endOffset))
                .textAttributes(key)
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
}
