package com.luckyframework.idea.spel.documentation

import com.intellij.model.Pointer
import com.intellij.pom.Navigatable
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.luckyframework.idea.spel.reference.SpelDeclaration

/**
 * SpEL 符号的文档目标。
 *
 * 2026.3 起悬停与 Quick Doc 走 `DocumentationTarget` 体系，
 * 平台的 `PsiElementDocumentationTarget` 只处理真实 PSI，虚拟声明需要
 * 由 [SpelDocumentationTargetProvider] 产出本类实例才能展示文档。
 */
class SpelDocumentationTarget(private val declaration: SpelDeclaration) : DocumentationTarget {

    override fun createPointer(): Pointer<out DocumentationTarget> = Pointer.hardPointer(this)

    override fun computePresentation(): TargetPresentation =
        TargetPresentation.builder(declaration.presentation.presentableText ?: declaration.kindLabel).presentation()

    /** 悬停预览只展示简短标题，完整内容由 [computeDocumentation] 提供。 */
    override fun computeDocumentationHint(): String = buildString {
        append(declaration.kindLabel)
        declaration.presentation.presentableText?.takeIf { it.isNotBlank() }?.let {
            append(' ').append(it)
        }
    }

    override fun computeDocumentation(): DocumentationResult =
        DocumentationResult.documentation(SpelDocumentationRenderer.render(documentation()))

    override val navigatable: Navigatable? get() = declaration

    private fun documentation(): SpelDocumentation =
        declaration.documentation ?: SpelDocumentationResolver.fallback(declaration)
}
