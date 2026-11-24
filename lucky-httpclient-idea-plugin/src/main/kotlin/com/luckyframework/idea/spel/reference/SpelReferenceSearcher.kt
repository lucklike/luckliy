package com.luckyframework.idea.spel.reference

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiVariable
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.TextOccurenceProcessor
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.luckyframework.idea.spel.environment.SpringPlaceholderReferenceProvider
import com.luckyframework.idea.spel.environment.SpringPlaceholderUsageSearch

/**
 * SpEL 引用的查找用法与重命名收集（10.7/13.1/13.2）。
 *
 * 平台默认按词搜索时只对出现位置所在的最内层元素取引用，拿不到注册在
 * `PsiLiteralExpression` 上的 SpEL 引用；这里按符号名搜索后回到字面量
 * 逐个校验 `isReferenceTo`，从而支持跨文件收集。
 *
 * 命中字面量后只能调用具体的引用提供器（`SearchRequest.referencesOf`），
 * 不能调用 `literal.references`：后者会执行全部 contributor，其中 IntelliLang
 * 的 `@Language` 拼接注入会在内部再次发起 `ReferencesSearch`，重入本搜索器
 * 导致无限递归（StackOverflowError）。
 *
 * 除虚拟声明 [SpelDeclaration] 外，还覆盖重命名入口 [SpelRenameDeclaration]
 * 以及被 SpEL 引用的变量（字段/参数）、类型与可导入的静态方法。
 *
 * 平台对方法的 Find Usages 与 unused 判定不会执行本扩展点，见
 * [SpelMethodReferenceSearcher]。
 */
class SpelReferenceSearcher :
    QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        val target = queryParameters.elementToSearch
        val request = searchRequestOf(target) ?: return
        val searchScope = searchScopeOf(queryParameters, target, request)
        processRequest(target.project, searchScope, request, consumer)
    }

    /**
     * 平台对方法的 Find Usages 与 unused 判定走
     * [com.intellij.psi.search.searches.MethodReferencesSearch]，不会执行
     * `referencesSearch` 扩展点：只注册后者时，被 `#{#fn(...)}` 或
     * `func = "fn"` 引用的方法仍会被标记为未使用，Find Usages 也收集不到
     * 注解里的用法。
     *
     * 与 [importedMethodRequest] 的区别：带 `@FunctionAlias` 的方法按别名搜索，
     * 否则只被别名引用的方法会被误判未使用；方法重命名仍走
     * [ReferencesSearch]，别名引用不会被改写。
     */
    internal fun processMethodUsages(
        method: PsiMethod,
        scope: SearchScope,
        consumer: Processor<in PsiReference>
    ) {
        if (!isImportableStatic(method)) {
            return
        }
        // 平台在未使用判定时可能传入空 LocalSearchScope，空范围搜不到任何用法，回退到方法使用范围
        val effectiveScope = if (scope is LocalSearchScope && scope.scope.isEmpty()) {
            method.useScope
        } else {
            scope
        }
        for (request in methodUsageRequests(method)) {
            if (!processRequest(method.project, effectiveScope, request, consumer)) {
                return
            }
        }
    }

    private fun methodUsageRequests(method: PsiMethod): List<SearchRequest> {
        val alias = aliasOf(method)
        if (alias.isNullOrEmpty()) {
            return listOf(SearchRequest(method.name, null) { it.isReferenceTo(method) })
        }
        return listOf(SearchRequest(alias, null) { it.isReferenceTo(method) })
    }

    private fun processRequest(
        project: Project,
        scope: SearchScope,
        request: SearchRequest,
        consumer: Processor<in PsiReference>
    ): Boolean {
        if (!project.isInitialized || request.name.isEmpty()) {
            return true
        }
        val processor = object : TextOccurenceProcessor {
            override fun execute(element: PsiElement, offsetInElement: Int): Boolean {
                val literal = element as? PsiLiteralExpression
                    ?: PsiTreeUtil.getParentOfType(element, PsiLiteralExpression::class.java, false)
                    ?: return true
                val absolute = element.textRange.startOffset + offsetInElement
                val offsetInLiteral = absolute - literal.textRange.startOffset
                for (reference in request.referencesOf(literal)) {
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
        return PsiSearchHelper.getInstance(project).processElementsWithWord(
            processor,
            scope,
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

        // 字段与方法参数都会成为 `#name` 的解析目标；只注册方法搜索时
        // 被 `#{#field}` 引用的字段会被误判未使用、重命名也不会更新引用。
        is PsiVariable ->
            target.name?.let { SearchRequest(it, target.useScope) { ref -> ref.isReferenceTo(target) } }

        // `T(Type)`/`new Type` 中的类型引用；`@bean` 虽也指向类，
        // 但 Bean 名不随类名变化，由 [SpelReference.isReferenceTo] 排除
        is PsiClass ->
            target.name?.let { SearchRequest(it, target.useScope) { ref -> ref.isReferenceTo(target) } }

        is PsiMethod -> importedMethodRequest(target)

        // properties 文件的键名也是 `${key}` 的声明：重命名配置键时要收集注解
        // 里的用法。不直接引用 Properties 插件的类，避免未安装时类加载失败。
        else -> propertyRequest(target)
    }

    private fun propertyRequest(target: PsiElement): SearchRequest? {
        val file = target.containingFile ?: return null
        if (!file.name.endsWith(".properties", ignoreCase = true)) {
            return null
        }
        val key = (target as? PsiNamedElement)?.name?.takeIf { it.isNotBlank() } ?: return null
        return SearchRequest(
            SpringPlaceholderUsageSearch.searchWordOf(key),
            null,
            { literal -> SpringPlaceholderReferenceProvider.referencesOf(literal) }
        ) { reference -> SpringPlaceholderUsageSearch.placeholderKeyOf(reference) == key }
    }

    /** 只对可被 `@SpELImport` 导入的 public static 方法收集 `#fn(...)` 引用。 */
    private fun importedMethodRequest(method: PsiMethod): SearchRequest? {
        if (!isImportableStatic(method)) {
            return null
        }
        // 有别名时引用由别名决定，重命名方法名不应改写 `#alias(...)`
        if (aliasOf(method) != null) {
            return null
        }
        return SearchRequest(method.name, null) { it.isReferenceTo(method) }
    }

    /** 可被 SpEL 导入/按平铺名引用的方法：public static。 */
    private fun isImportableStatic(method: PsiMethod): Boolean =
        method.hasModifierProperty(PsiModifier.PUBLIC) &&
            method.hasModifierProperty(PsiModifier.STATIC)

    private fun aliasOf(method: PsiMethod): String? =
        (method.getAnnotation(FUNCTION_ALIAS)?.findAttributeValue("value") as? PsiLiteralExpression)
            ?.value as? String

    private data class SearchRequest(
        val name: String,
        val scope: SearchScope?,
        /** 命中字面量后从哪个提供器取引用；禁止回退到全量的 `literal.references`。 */
        val referencesOf: (PsiLiteralExpression) -> Array<PsiReference> =
            { literal -> SpelReferenceProvider.referencesOf(literal) },
        val matches: (PsiReference) -> Boolean
    )

    companion object {
        private const val FUNCTION_ALIAS = "com.luckyframework.httpclient.proxy.spel.FunctionAlias"
    }
}
