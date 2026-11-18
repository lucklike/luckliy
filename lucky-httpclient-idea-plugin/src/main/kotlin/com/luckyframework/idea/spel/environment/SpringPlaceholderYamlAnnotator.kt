package com.luckyframework.idea.spel.environment

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.luckyframework.idea.spel.yaml.SpelYamlSupport
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * YAML 值中的 `${...}` 高亮与语法错误。
 *
 * 与 SpEL 的 YAML 支持共用值位置判定；只在 starter 依赖存在时注册能力，
 * 避免与 Spring 插件对普通配置项的占位符处理产生重复诊断。
 */
class SpringPlaceholderYamlAnnotator : Annotator {

    private val highlighter = SpringPlaceholderHighlighter()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val scalar = element as? YAMLScalar ?: return
        if (!SpelYamlSupport.isPlaceholderValue(scalar)) {
            return
        }
        val service = SpringEnvironmentService.getInstance(scalar.project)
        if (!service.isEnabledFor(scalar)) {
            return
        }
        val model = service.modelFor(scalar) ?: return
        highlighter.annotate(
            holder,
            scalar.text,
            scalar.textRange.startOffset,
            SpringPlaceholderParser.parse(scalar.text),
            model
        )
    }
}
