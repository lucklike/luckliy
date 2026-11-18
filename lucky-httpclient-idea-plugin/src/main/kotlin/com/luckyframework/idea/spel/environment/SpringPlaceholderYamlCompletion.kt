package com.luckyframework.idea.spel.environment

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.luckyframework.idea.spel.yaml.SpelYamlSupport
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * YAML 值中 `${key}` 的键补全（YAML 插件存在时加载）。
 */
class SpringPlaceholderYamlCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val scalar = PsiTreeUtil.getParentOfType(
            parameters.position,
            YAMLScalar::class.java,
            false
        ) ?: return
        if (!SpelYamlSupport.isPlaceholderValue(scalar)) {
            return
        }
        val service = SpringEnvironmentService.getInstance(scalar.project)
        if (!service.isEnabledForCopy(scalar, parameters.originalFile)) {
            return
        }
        val offsetInScalar = parameters.offset - scalar.textRange.startOffset
        val placeholder = SpringPlaceholderParser.placeholderAt(scalar.text, offsetInScalar) ?: return
        val model = service.modelForCopy(scalar, parameters.originalFile) ?: return
        val keyPrefix = scalar.text.substring(placeholder.keyStartOffset, offsetInScalar)
        SpringPlaceholderCompletionSupport.addCandidates(
            model,
            keyPrefix,
            scalar.textRange.startOffset + placeholder.keyStartOffset,
            result
        )
    }
}
