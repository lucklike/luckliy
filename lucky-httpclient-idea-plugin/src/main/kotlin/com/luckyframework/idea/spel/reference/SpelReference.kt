package com.luckyframework.idea.spel.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult

/**
 * SpEL 符号引用（design D8）：软引用，解析目标是 [SpelDeclaration]。
 *
 * 函数重载与同名成员可能对应多个目标，因此实现
 * [com.intellij.psi.PsiPolyVariantReference] 供跳转提供多目标选择。
 */
class SpelReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val targetProvider: () -> List<SpelDeclaration>
) : PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement, true) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> =
        targetProvider()
            .map { PsiElementResolveResult(it) as ResolveResult }
            .toTypedArray()

    /**
     * 重命名引用时保留 `#`/`$`/`$$` 前缀与 `::type` 后缀；
     * `$0`/`$$1` 这类位置编号不随声明名变化。
     */
    override fun handleElementRename(newElementName: String): PsiElement {
        val current = element.text.substring(rangeInElement.startOffset, rangeInElement.endOffset)
        val renamed = renameText(current, newElementName)
        if (renamed == current) {
            return element
        }
        return ElementManipulators.handleContentChange(element, rangeInElement, renamed)
    }

    private fun renameText(current: String, newName: String): String {
        val typeSuffix = "::type"
        val hasSuffix = current.endsWith(typeSuffix)
        val head = if (hasSuffix) current.removeSuffix(typeSuffix) else current
        val prefix = when {
            head.startsWith("\$\$") -> "\$\$"
            head.startsWith("\$") -> "\$"
            head.startsWith("#") -> "#"
            else -> ""
        }
        val base = head.removePrefix(prefix)
        if (prefix.startsWith("\$") && base.isNotEmpty() && base.all { it.isDigit() }) {
            return current
        }
        return prefix + newName + if (hasSuffix) typeSuffix else ""
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        val targets = targetProvider()
        val matched = targets.any { target ->
            when (element) {
                is SpelDeclaration ->
                    target.name == element.name &&
                        target.kindLabel == element.kindLabel &&
                        (element.target == null || target.target == element.target)

                else -> target.target == element || target.target == element.navigationElement
            }
        }
        return matched || super.isReferenceTo(element)
    }
}
