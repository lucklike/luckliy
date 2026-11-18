package com.luckyframework.idea.std.yaml

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.luckyframework.idea.std.StdHttpClientIndex

/**
 * `method-configs.<apiId>` 键到接口方法的软引用。
 *
 * 重载方法共享同一个 apiId，返回全部同名目标；目标不存在时为空。
 */
class StdApiIdReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val configId: String,
    private val apiId: String
) : PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        StdHttpClientIndex.getInstance(element.project)
            .entriesByConfigId(configId)
            .flatMap { entry -> entry.methods.filter { it.apiId == apiId }.map { it.method } }
            .map { PsiElementResolveResult(it) as ResolveResult }
            .toTypedArray()

    override fun isReferenceTo(element: PsiElement): Boolean =
        element is PsiMethod &&
            StdHttpClientIndex.getInstance(element.project)
                .entriesByConfigId(configId)
                .any { entry -> entry.methods.any { it.method == element && it.apiId == apiId } }
}
