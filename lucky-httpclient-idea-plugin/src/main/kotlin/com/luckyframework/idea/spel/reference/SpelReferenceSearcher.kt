package com.luckyframework.idea.spel.reference

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.TextOccurenceProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor

/**
 * SpEL 引用的查找用法与重命名收集（10.7/13.1/13.2）。
 *
 * 平台默认按词搜索时只对出现位置所在的最内层元素取引用，拿不到注册在
 * `PsiLiteralExpression` 上的 SpEL 引用；这里按符号名搜索后回到字面量
 * 逐个校验 `isReferenceTo`，从而支持跨文件收集。
 *
 * 除虚拟声明 [SpelDeclaration] 外，还覆盖重命名入口 [SpelRenameDeclaration]
 * 以及被 SpEL 引用的方法参数与可导入的静态方法。
 */
class SpelReferenceSearcher :
    QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val target = queryParameters.elementToSearch
        val request = searchRequestOf(target) ?: return
        val project = target.project
        if (!project.isInitialized || request.name.isEmpty()) {
            return
        }
        val searchScope = searchScopeOf(queryParameters, target, request)
        val processor = object : TextOccurenceProcessor {
            override fun execute(element: PsiElement, offsetInElement: Int): Boolean {
                val literal = element as? PsiLiteralExpression
                    ?: PsiTreeUtil.getParentOfType(element, PsiLiteralExpression::class.java, false)
                    ?: return true
                val absolute = element.textRange.startOffset + offsetInElement
                val offsetInLiteral = absolute - literal.textRange.startOffset
                for (reference in literal.references) {
                    if (reference.rangeInElement.containsOffset(offsetInLiteral) &&
                        request.matches(reference)
                    ) {
                        if (!consumer.process(reference)) {
                            return false
                        }
                    }
                }
                return true
            }
        }
        PsiSearchHelper.getInstance(project).processElementsWithWord(
            processor,
            searchScope,
            request.name,
            UsageSearchContext.ANY,
            true
        )
    }

    private fun searchScopeOf(
        queryParameters: ReferencesSearch.SearchParameters,
        target: PsiElement,
        request: SearchRequest
    ): SearchScope {
        request.scope?.let { return it }
        // 虚拟声明无法推导字面量所在的文件范围，平台会给出空 LocalSearchScope，降级到项目范围
        val requested = queryParameters.effectiveSearchScope
        return if (requested is LocalSearchScope && requested.scope.isEmpty()) {
            target.useScope
        } else {
            requested
        }
    }

    private fun searchRequestOf(target: PsiElement): SearchRequest? = when (target) {
        is SpelDeclaration ->
            SearchRequest(target.name, null) { it.isReferenceTo(target) }

        is SpelRenameDeclaration ->
            SearchRequest(target.name, null) { it.isReferenceTo(target.declarationToMatch()) }

        is PsiParameter ->
            SearchRequest(target.name, target.useScope) { it.isReferenceTo(target) }

        is PsiMethod -> importedMethodRequest(target)

        else -> null
    }

    /** 只对可被 `@SpELImport` 导入的 public static 方法收集 `#fn(...)` 引用。 */
    private fun importedMethodRequest(method: PsiMethod): SearchRequest? {
        if (!method.hasModifierProperty(PsiModifier.PUBLIC) ||
            !method.hasModifierProperty(PsiModifier.STATIC)
        ) {
            return null
        }
        // 有别名时引用由别名决定，重命名方法名不应改写 `#alias(...)`
        if (aliasOf(method) != null) {
            return null
        }
        return SearchRequest(method.name, null) { it.isReferenceTo(method) }
    }

    private fun aliasOf(method: PsiMethod): String? =
        (method.getAnnotation(FUNCTION_ALIAS)?.findAttributeValue("value") as? PsiLiteralExpression)
            ?.value as? String

    private data class SearchRequest(
        val name: String,
        val scope: SearchScope?,
        val matches: (PsiReference) -> Boolean
    )

    companion object {
        private const val FUNCTION_ALIAS = "com.luckyframework.httpclient.proxy.spel.FunctionAlias"
    }
}
