package com.luckyframework.idea.spel.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiClass
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

    /**
     * 引用文本去掉 `#`/`$`/`$$` 前缀与 `::type` 后缀后的名字。
     *
     * 重命名方法时平台经 MethodReferencesSearch 收集引用，别名引用
     * （`#alias`）也会命中；调用方用它区分「按名字引用」与「按别名引用」。
     */
    internal fun referencedName(): String {
        val current = element.text.substring(rangeInElement.startOffset, rangeInElement.endOffset)
        return current.removeSuffix("::type")
            .removePrefix("\$\$")
            .removePrefix("\$")
            .removePrefix("#")
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
        // 限定类型名（`T(com.example.Api)`）只替换最后一段，保留包名与外部类前缀
        val qualifier = if (base.contains('.')) base.substringBeforeLast('.') + "." else ""
        return prefix + qualifier + newName + if (hasSuffix) typeSuffix else ""
    }

    override fun isReferenceTo(element: PsiElement): Boolean {
        val targets = targetProvider()
        val matched = targets.any { target ->
            when (element) {
                is SpelDeclaration ->
                    target.name == element.name &&
                        target.kindLabel == element.kindLabel &&
                        (element.target == null || target.target == element.target)

                // `@bean` 的跳转目标也是类，但 Bean 名不随类改名：只认类型引用
                is PsiClass ->
                    target.isType && (target.target == element || target.target == element.navigationElement)

                else -> target.target == element || target.target == element.navigationElement
            }
        }
        return matched || super.isReferenceTo(element)
    }
}
