package com.luckyframework.idea.spel.reference

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
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
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelContextBuilder

/**
 * 为字符串字面量中的 SpEL 片段创建引用（design D8）。
 *
 * 扫描、词法与引用收集由 [SpelReferenceCollector] 提供，YAML 侧共用；
 * 符号视图由 [SpelContextBuilder] 统一提供，未解析引用只返回空目标。
 *
 * `kind = FUNCTION` 的属性值可以不写 `#{}`，整段就是一个函数名：运行期
 * `Context#autoExecuteSpELOrFunc` 先解析属性值，不含 `#{}` 时原样作为函数名
 * 从变量表取 `Method`，因此这里把整段建为一个引用。
 */
class SpelReferenceProvider(
    private val scanner: SpelScanner = SpelScanner(),
    private val tokenizer: SpelTokenizer = SpelTokenizer(),
    private val contextBuilder: SpelContextBuilder = SpelContextBuilder(),
    private val resolver: SpelAnnotationResolver = SpelAnnotationResolver()
) : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val literal = element as? PsiLiteralExpression ?: return PsiReference.EMPTY_ARRAY
        val hasFragment = literal.text.contains("#{")
        // 函数名属性整段不含 `#{}`，但同样参与解析；其它普通字符串直接跳过
        if (!hasFragment && !resolver.isFunctionNameLiteral(literal)) {
            return PsiReference.EMPTY_ARRAY
        }
        if (DumbService.isDumb(literal.project) ||
            !LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)
        ) {
            return PsiReference.EMPTY_ARRAY
        }

        val symbols = contextOf(literal)
        // 不在 SpEL 求值上下文（普通字符串、未标注 @SpelAttribute 的属性）：
        // 不建立任何引用，避免悬停/跳转把普通文本当成 SpEL 符号。
        if (!symbols.known) {
            return PsiReference.EMPTY_ARRAY
        }

        if (!hasFragment) {
            return functionNameReferences(literal, symbols)
        }
        val navigation = SpelNavigation(literal.project, symbols, anchor = literal)
        return SpelReferenceCollector.collect(literal, literal.text, navigation, scanner, tokenizer)
            .toTypedArray()
    }

    /**
     * 函数名字面量整体作为一个引用，目标为同名函数。
     *
     * 空内容（属性未填写）不建引用；重载命中多个目标时由
     * [SpelReference] 的多目标解析提供选择。
     */
    private fun functionNameReferences(
        literal: PsiLiteralExpression,
        symbols: SpelContext
    ): Array<PsiReference> {
        val name = resolver.functionNameOf(literal) ?: return PsiReference.EMPTY_ARRAY
        if (name.isEmpty()) {
            return PsiReference.EMPTY_ARRAY
        }
        val range = TextRange(1, literal.textLength - 1)
        if (range.isEmpty) {
            return PsiReference.EMPTY_ARRAY
        }
        val navigation = SpelNavigation(literal.project, symbols, anchor = literal)
        return arrayOf(SpelReference(literal, range) { navigation.functionNames(name) })
    }

    /** 按 PSI 修改计数缓存上下文，避免高亮与引用重复构建符号层。 */
    internal fun contextOf(literal: PsiLiteralExpression): SpelContext =
        CachedValuesManager.getCachedValue(literal) {
            CachedValueProvider.Result.create(
                contextBuilder.build(literal),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
}
