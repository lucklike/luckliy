package com.luckyframework.idea.spel.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.inspection.quickfix.SpelSuppressFix
import com.luckyframework.idea.spel.symbol.SpelContextBuilder

/**
 * SpEL 函数调用的参数检查（specs spel-diagnostics-quickfix）。
 *
 * - 参数数量与所有可解析重载都不匹配 → error（运行期一定失败）；
 * - 参数类型与参数声明肯定不兼容 → error，例如把字符串字面量传给
 *   `Integer` 参数、把 `ApiDescribe` 传给 `String` 参数；
 * - 参数表达式的结果类型无法推导 → warning，提醒用户无法静态校验。
 *
 * 只在可确定上下文中生效：字符串必须位于注解元数据表登记过的 SpEL 属性内，
 * 且所在模块依赖 lucky-httpclient。
 */
class SpelFunctionCallInspection : LocalInspectionTool() {

    private val contextBuilder = SpelContextBuilder()

    private val checker = SpelFunctionCallChecker()

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
        for (problem in checker.check(literal.text, context, literal.project)) {
            // 数量与确定类型冲突是运行期一定失败的调用；无法推导只给警告
            val highlight = when (problem.kind) {
                SpelCallProblemKind.ARGUMENT_COUNT -> ProblemHighlightType.GENERIC_ERROR
                SpelCallProblemKind.ARGUMENT_TYPE -> ProblemHighlightType.GENERIC_ERROR
                SpelCallProblemKind.ARGUMENT_TYPE_UNKNOWN -> ProblemHighlightType.WARNING
            }
            holder.registerProblem(
                literal,
                problem.message,
                highlight,
                problem.range,
                SpelSuppressFix(shortName)
            )
        }
    }
}
