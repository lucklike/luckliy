package com.luckyframework.idea.spel.documentation

import com.intellij.model.Pointer
import com.intellij.pom.Navigatable
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.luckyframework.idea.spel.reference.SpelDeclaration

/**
 * 补全列表选中项的右侧文档（默认方法提示样式）。
 *
 * 与悬停文档共用 [SpelDeclaration] 的内容，但只渲染 IDEA 默认方法提示框
 * 展示的部分：所属类、签名、描述、参数与返回值，不重复来源、作用域等元信息。
 */
class SpelCompletionDocumentationTarget(private val declaration: SpelDeclaration) : DocumentationTarget {

    override fun createPointer(): Pointer<out DocumentationTarget> = Pointer.hardPointer(this)

    override fun computePresentation(): TargetPresentation =
        TargetPresentation.builder(declaration.presentation.presentableText ?: declaration.kindLabel).presentation()

    override fun computeDocumentation(): DocumentationResult =
        DocumentationResult.documentation(
            SpelDocumentationRenderer.renderCompletion(
                declaration.documentation ?: SpelDocumentationResolver.fallback(declaration),
                declaration.target
            )
        )

    override val navigatable: Navigatable? get() = declaration
}
