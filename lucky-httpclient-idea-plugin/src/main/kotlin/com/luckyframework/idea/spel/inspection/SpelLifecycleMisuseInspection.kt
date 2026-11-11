package com.luckyframework.idea.spel.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.documentation.SpelDocumentationRenderer
import com.luckyframework.idea.spel.inspection.quickfix.SpelSuppressFix
import com.luckyframework.idea.spel.symbol.SpelContextBuilder
import com.luckyframework.idea.spel.symbol.SpelVisibility

/**
 * 生命周期误用检查（12.3，specs spel-diagnostics-quickfix）。
 *
 * 上下文阶段早于变量可用阶段时报告，例如 URL 注解（请求阶段）引用
 * `$body$`（响应阶段起可用）。只有全部同名符号都不可用时才提示，
 * 避免被方法参数等更近来源遮蔽时误报。
 */
class SpelLifecycleMisuseInspection : LocalInspectionTool() {

    private val contextBuilder = SpelContextBuilder()

    private val usageScanner = SpelUsageScanner()

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {
            override fun visitLiteralExpression(expression: PsiLiteralExpression) {
                inspect(holder, expression)
            }
        }

    override fun isSuppressedFor(element: PsiElement): Boolean =
        super.isSuppressedFor(element) || SpelSuppression.isSuppressed(element, shortName)

    private fun inspect(holder: ProblemsHolder, literal: PsiLiteralExpression) {
        if (!literal.text.contains("#{")) {
            return
        }
        if (!LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)) {
            return
        }
        val context = contextBuilder.build(literal)
        if (!context.known) {
            return
        }
        for (usage in usageScanner.scan(literal, context)) {
            if (usage.symbols.isEmpty() ||
                usage.symbols.any { it.visibility == SpelVisibility.AVAILABLE }
            ) {
                continue
            }
            val label = SpelDocumentationRenderer.stageLabel(usage.symbols.first().lifecycle) ?: continue
            holder.registerProblem(
                literal,
                "${usage.name} 仅在${label}可用",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                usage.range,
                SpelSuppressFix(shortName)
            )
        }
    }
}
