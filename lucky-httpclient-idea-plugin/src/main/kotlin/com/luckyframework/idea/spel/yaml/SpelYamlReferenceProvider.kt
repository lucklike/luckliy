package com.luckyframework.idea.spel.yaml

import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.reference.SpelNavigation
import com.luckyframework.idea.spel.reference.SpelReferenceCollector
import com.luckyframework.idea.spel.symbol.SpelContext
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * 为 YAML 标量中的 SpEL 片段创建引用。
 *
 * 上下文来自当前文件的 `springElImport` 与内置符号，按 PSI 修改计数缓存。
 */
class SpelYamlReferenceProvider(
    private val scanner: SpelScanner = SpelScanner(),
    private val tokenizer: SpelTokenizer = SpelTokenizer(),
    private val contextBuilder: SpelYamlContextBuilder = SpelYamlContextBuilder()
) : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val scalar = element as? YAMLScalar ?: return PsiReference.EMPTY_ARRAY
        if (!SpelYamlSupport.isExpressionValue(scalar)) {
            return PsiReference.EMPTY_ARRAY
        }
        if (DumbService.isDumb(scalar.project) ||
            !LuckyHttpClientPresence.getInstance(scalar.project).isEnabledFor(scalar)
        ) {
            return PsiReference.EMPTY_ARRAY
        }

        val navigation = SpelNavigation(scalar.project, contextOf(scalar), anchor = scalar)
        return SpelReferenceCollector.collect(scalar, scalar.text, navigation, scanner, tokenizer)
            .toTypedArray()
    }

    private fun contextOf(scalar: YAMLScalar): SpelContext =
        CachedValuesManager.getCachedValue(scalar) {
            CachedValueProvider.Result.create(
                contextBuilder.build(scalar),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
}
