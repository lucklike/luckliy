package com.luckyframework.idea.spel.refactoring

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenameJavaMethodProcessor
import com.luckyframework.idea.spel.reference.SpelReference

/**
 * 带 `@FunctionAlias` 的方法重命名保护（13.2）。
 *
 * 平台重命名方法时经 [com.intellij.psi.search.searches.MethodReferencesSearch]
 * 收集引用；插件为未使用判定补充的搜索会把 `#alias(...)` 与别名形式的函数名
 * 属性值也带回来。这些引用的文本是别名而不是方法名，若原样应用重命名会把
 * 别名改写成方法名，所以这里过滤掉文本不是方法名本身的 SpEL 引用——别名的
 * 改名仍由 [SpelRenameProcessor] 包装的别名声明负责。
 *
 * 其余行为全部继承平台处理器，避免改变 Java 方法重命名语义。
 */
class SpelMethodRenameProcessor : RenameJavaMethodProcessor() {

    override fun canProcessElement(element: PsiElement): Boolean =
        element is PsiMethod && aliasOf(element) != null

    override fun findReferences(
        element: PsiElement,
        searchScope: SearchScope,
        searchInCommentsAndStrings: Boolean
    ): Collection<PsiReference> {
        val references = super.findReferences(element, searchScope, searchInCommentsAndStrings)
        val method = element as? PsiMethod ?: return references
        return references.filterNot { reference ->
            reference is SpelReference && reference.referencedName() != method.name
        }
    }

    private fun aliasOf(method: PsiMethod): String? =
        (method.getAnnotation(FUNCTION_ALIAS)?.findAttributeValue("value") as? PsiLiteralExpression)
            ?.value as? String

    companion object {
        private const val FUNCTION_ALIAS = "com.luckyframework.httpclient.proxy.spel.FunctionAlias"
    }
}
