package com.luckyframework.idea.spel.environment

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver

/**
 * Java 注解属性中的 `${...}` 高亮与语法错误。
 *
 * 着色只看属性是否标注了 `@SpelAttribute`：着色是源码样式，不依赖 starter
 * 是否在当前模块的 classpath 上，否则引入 starter 前后同一段代码会有两种
 * 颜色。配置键是否存在的警告仍需要环境快照，starter 缺失或索引期间跳过。
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
        // 快照不存在时仍然着色，highlighter 会跳过依赖配置快照的警告
        val model = service.modelFor(literal)
        highlighter.annotate(
            holder,
            literal.text,
            literal.textRange.startOffset,
            SpringPlaceholderParser.parse(literal.text),
            model
        )
    }
}
