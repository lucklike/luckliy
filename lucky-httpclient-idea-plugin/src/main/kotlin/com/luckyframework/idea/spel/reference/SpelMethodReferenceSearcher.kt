package com.luckyframework.idea.spel.reference

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.util.Processor

/**
 * 方法的 Find Usages 与 unused 判定入口（10.7/13.2）。
 *
 * 平台 `JavaFindUsagesHelper` 对 `PsiMethod` 使用 [MethodReferencesSearch]，
 * 不会执行 `referencesSearch` 扩展点；只注册后者时，被 `#{#fn(...)}` 或
 * 函数名属性 `func = "fn"` 引用的方法仍会被标记为未使用，Find Usages 与
 * 重命名预览也收集不到注解里的用法。
 *
 * 搜索逻辑复用 [SpelReferenceSearcher]，保证引用匹配规则单一。
 */
class SpelMethodReferenceSearcher(
    private val searcher: SpelReferenceSearcher = SpelReferenceSearcher()
) : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {

    override fun processQuery(
        queryParameters: MethodReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>
    ) {
        searcher.processMethodUsages(
            queryParameters.method,
            queryParameters.effectiveSearchScope,
            consumer
        )
    }
}
