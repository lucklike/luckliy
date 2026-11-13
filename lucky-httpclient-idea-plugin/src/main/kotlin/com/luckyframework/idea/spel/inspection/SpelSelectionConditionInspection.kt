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
 * SpEL 过滤条件类型检查。
 *
 * 选择表达式 `?[...]`/`^[...]`/`$[...]` 的条件结果必须是 boolean：
 * 条件类型能静态推导且不是 `boolean`/`Boolean` → error（`#{users.?[name]}`、
 * `#{users.?[1]}` 在运行期一定失败）；无法推导的条件保守放行。
 *
 * 只在可确定上下文中生效：字符串必须位于注解元数据表登记过的 SpEL 属性内，
 * 且所在模块依赖 lucky-httpclient。
 */
class SpelSelectionConditionInspection : LocalInspectionTool() {

    private val contextBuilder = SpelContextBuilder()

    private val checker = SpelSelectionConditionChecker()

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
            holder.registerProblem(
                literal,
                problem.message,
                ProblemHighlightType.GENERIC_ERROR,
                problem.range,
                SpelSuppressFix(shortName)
            )
        }
    }
}
