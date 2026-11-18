package com.luckyframework.idea.spel.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil

/**
 * SpEL 符号引用（design D8）：软引用，解析目标是 [SpelDeclaration]。
 *
 * 函数重载与同名成员可能对应多个目标，因此实现
 * [com.intellij.psi.PsiPolyVariantReference] 供跳转提供多目标选择。
 *
 * [renameMode] 区分三类引用文本的改写方式：SpEL 符号要保留 `#`/`$` 前缀与
 * 限定名；`${key}` 环境变量占位符的键名是完整标识，properties 重命名参数
 * 也是完整键名，YAML 重命名参数则只是叶子键名。
 */
class SpelReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val renameMode: RenameMode = RenameMode.SPEL,
    private val targetProvider: () -> List<SpelDeclaration>
) : PsiPolyVariantReferenceBase<PsiElement>(element, rangeInElement, true) {

    /** 引用文本在重命名时的改写方式。 */
    enum class RenameMode {
        /** SpEL 符号：保留 `#`/`$` 前缀、限定名与 `::type` 后缀。 */
        SPEL,

        /** properties 键：引用文本是完整键名，重命名参数也是完整键名。 */
        PROPERTY_FULL,

        /** YAML 键：重命名参数只是叶子键名，需要保留父路径。 */
        PROPERTY_LEAF
    }

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
        val renamed = when (renameMode) {
            RenameMode.PROPERTY_FULL -> newElementName
            RenameMode.PROPERTY_LEAF -> renamedPropertyKey(current, newElementName)
            RenameMode.SPEL -> renameText(current, newElementName)
        }
        if (renamed == current) {
            return element
        }
        return ElementManipulators.handleContentChange(element, rangeInElement, renamed)
    }

    /**
     * YAML 叶子键重命名：重命名参数不含父路径，需要保留旧键名前缀
     * （`spring.datasource.`）。单键写成完整路径（`server.port: 1`）时
     * YAML 插件传入的新名本身含 `.`，直接使用。
     */
    private fun renamedPropertyKey(current: String, newName: String): String =
        if (newName.contains('.') || !current.contains('.')) {
            newName
        } else {
            current.substringBeforeLast('.') + "." + newName
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

                // 配置键重命名传入的声明是 Property/YAMLKeyValue，跳转目标
                // 是键名内部的叶子元素，按祖先关系匹配
                else -> target.target == element || target.target == element.navigationElement ||
                    (target.target != null && PsiTreeUtil.isAncestor(element, target.target, false))
            }
        }
        return matched || super.isReferenceTo(element)
    }
}
