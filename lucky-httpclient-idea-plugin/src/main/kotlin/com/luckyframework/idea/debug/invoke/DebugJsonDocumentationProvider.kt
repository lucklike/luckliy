package com.luckyframework.idea.debug.invoke

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.psi.PsiElement

/**
 * JSON 参数字段的旧版文档链。
 *
 * 2026.3 优先询问 [DebugJsonDocumentationTargetProvider]，全部目标提供器返回空时
 * 才回退到这里；两个入口共用 [DebugJsonDocumentation] 的字段索引，内容保持一致。
 */
class DebugJsonDocumentationProvider : DocumentationProvider {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? =
        fieldOf(element, originalElement)?.let { DebugJsonDocumentation.htmlOf(it) }

    /** 新版平台从 [DocumentationProvider.DocumentationParts] 取文档，第一段为正文。 */
    override fun getDocumentationParts(
        element: PsiElement,
        originalElement: PsiElement?
    ): DocumentationProvider.DocumentationParts? =
        fieldOf(element, originalElement)?.let {
            DocumentationProvider.DocumentationParts(DebugJsonDocumentation.htmlOf(it), null)
        }

    private fun fieldOf(element: PsiElement?, originalElement: PsiElement?): DebugParameter? =
        sequenceOf(element, originalElement)
            .filterNotNull()
            .firstNotNullOfOrNull { DebugJsonDocumentation.parameterAt(it) }
}
