package com.luckyframework.idea.spel.documentation

import com.intellij.openapi.diagnostic.Logger
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.luckyframework.idea.spel.reference.SpelDeclaration

/**
 * 补全列表选中项的文档目标（`documentation.psiTargetProvider`）。
 *
 * 函数/方法候选的 lookup element 携带虚拟声明，平台经
 * `SpelDocumentationProvider.getDocumentationElementForLookupItem` 取出后，
 * 由这里产出 [SpelCompletionDocumentationTarget]，在右侧文档区展示
 * 与 IDEA 默认方法提示框一致的签名、参数与返回值。
 */
class SpelPsiDocumentationTargetProvider : PsiDocumentationTargetProvider {

    private val log = Logger.getInstance("com.luckyframework.idea.spel.doc")

    /**
     * 2026.3 的接口默认实现会直接抛异常，必须与复数方法一起实现，
     * 否则平台一旦改调单数入口就会得到一片空白文档。
     */
    override fun documentationTarget(
        element: PsiElement,
        originalElement: PsiElement?
    ): DocumentationTarget? =
        (element as? SpelDeclaration)?.let { SpelCompletionDocumentationTarget(it) }

    override fun documentationTargets(
        element: PsiElement,
        originalElement: PsiElement?
    ): List<DocumentationTarget> {
        log.debug(
            "[SpelDoc] psiTargets element=${element.javaClass.name} hit=${element is SpelDeclaration} " +
                "original=${originalElement?.javaClass?.name}"
        )
        return (element as? SpelDeclaration)?.let { listOf(SpelCompletionDocumentationTarget(it)) } ?: emptyList()
    }
}
