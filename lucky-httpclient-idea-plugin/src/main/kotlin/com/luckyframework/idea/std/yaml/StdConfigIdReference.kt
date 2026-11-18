package com.luckyframework.idea.std.yaml

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.luckyframework.idea.std.StdHttpClientIndex

/**
 * `standard-client-configs.<configId>` 键到接口的软引用。
 *
 * 同一 configId 可能被多个接口共用，因此返回多目标；目标不存在时为空。
 */
class StdConfigIdReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val configId: String
) : PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        StdHttpClientIndex.getInstance(element.project)
            .entriesByConfigId(configId)
            .map { PsiElementResolveResult(it.clientClass) as ResolveResult }
            .toTypedArray()

    override fun isReferenceTo(element: PsiElement): Boolean =
        element is PsiClass &&
            StdHttpClientIndex.getInstance(element.project)
                .entriesByConfigId(configId)
                .any { it.clientClass == element }
}
