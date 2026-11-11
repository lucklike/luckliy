package com.luckyframework.idea.spel.yaml

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.highlight.SpelFragmentHighlighter
import com.luckyframework.idea.spel.lexer.SpelScanner
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * YAML 标量中的 SpEL 高亮与基础语法错误。
 *
 * 与 Java 注解共用 [SpelFragmentHighlighter]；仅在 YAML 插件存在时由
 * `yaml-spel-support.xml` 注册。
 */
class SpelYamlAnnotator : Annotator {

    private val scanner = SpelScanner()

    private val highlighter = SpelFragmentHighlighter()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val scalar = element as? YAMLScalar ?: return
        if (!SpelYamlSupport.isExpressionValue(scalar)) {
            return
        }
        if (!LuckyHttpClientPresence.getInstance(scalar.project).isEnabledFor(scalar)) {
            return
        }

        highlighter.annotate(
            holder,
            scalar.text,
            scalar.textRange.startOffset,
            scanner.scanLiteral(scalar.text)
        )
    }
}
