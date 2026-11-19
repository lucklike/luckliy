package com.luckyframework.idea.spel.yaml

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.completion.SpelCompletionAnalyzer
import com.luckyframework.idea.spel.completion.SpelCompletionProvider
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * YAML 标量中的 SpEL 补全。
 *
 * 候选生成复用 [SpelCompletionProvider]，符号视图只包含当前文件的
 * `springElImport` 声明与内置符号。
 */
class SpelYamlCompletionProvider : CompletionProvider<CompletionParameters>() {

    private val analyzer = SpelCompletionAnalyzer()

    private val completion = SpelCompletionProvider()

    private val importParser = SpelYamlImportParser()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val scalar = SpelYamlSupport.scalarAt(parameters.position) ?: return
        if (!SpelYamlSupport.isExpressionValue(scalar)) {
            return
        }
        if (!LuckyHttpClientPresence.getInstance(scalar.project)
                .isEnabledForCopy(scalar, parameters.originalFile)
        ) {
            return
        }
        val site = analyzer.analyze(
            scalar.text,
            parameters.offset - scalar.textRange.startOffset
        ) ?: return
        val file = scalar.containingFile ?: return
        completion.addCandidates(
            site,
            SpelYamlContextBuilder.cachedContextOf(scalar),
            scalar,
            importParser.packages(file),
            result,
            parameters.editor
        )
    }
}
