package com.luckyframework.idea.spel.reference

import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelContextBuilder

/**
 * 为字符串字面量中的 SpEL 片段创建引用（design D8）。
 *
 * 扫描、词法与引用收集由 [SpelReferenceCollector] 提供，YAML 侧共用；
 * 符号视图由 [SpelContextBuilder] 统一提供，未解析引用只返回空目标。
 */
class SpelReferenceProvider(
    private val scanner: SpelScanner = SpelScanner(),
    private val tokenizer: SpelTokenizer = SpelTokenizer(),
    private val contextBuilder: SpelContextBuilder = SpelContextBuilder()
) : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val literal = element as? PsiLiteralExpression ?: return PsiReference.EMPTY_ARRAY
        if (!literal.text.contains("#{")) {
            return PsiReference.EMPTY_ARRAY
        }
        if (DumbService.isDumb(literal.project) ||
            !LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)
        ) {
            return PsiReference.EMPTY_ARRAY
        }

        val navigation = SpelNavigation(literal.project, contextOf(literal), anchor = literal)
        return SpelReferenceCollector.collect(literal, literal.text, navigation, scanner, tokenizer)
            .toTypedArray()
    }

    /** 按 PSI 修改计数缓存上下文，避免高亮与引用重复构建符号层。 */
    internal fun contextOf(literal: PsiLiteralExpression): SpelContext =
        CachedValuesManager.getCachedValue(literal) {
            CachedValueProvider.Result.create(
                contextBuilder.build(literal, true),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
}
