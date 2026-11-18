package com.luckyframework.idea.spel.environment

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver

/**
 * Java 注解属性中的 `${...}` 高亮与语法错误。
 *
 * 与 SpEL 一致，只有标注了 `@SpelAttribute` 的属性参与：未标注的属性
 * 运行期不会经过 `SpringSpELConvert`，`${...}` 只是普通文本。
 */
class SpringPlaceholderAnnotator : Annotator {

    private val resolver = SpelAnnotationResolver()

    private val highlighter = SpringPlaceholderHighlighter()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val literal = element as? PsiLiteralExpression ?: return
        if (!literal.text.contains("\${")) {
            return
        }
        if (!resolver.isSpelLiteral(literal)) {
            return
        }
        val service = SpringEnvironmentService.getInstance(literal.project)
        if (!service.isEnabledFor(literal)) {
            return
        }
        val model = service.modelFor(literal) ?: return
        highlighter.annotate(
            holder,
            literal.text,
            literal.textRange.startOffset,
            SpringPlaceholderParser.parse(literal.text),
            model
        )
    }
}
