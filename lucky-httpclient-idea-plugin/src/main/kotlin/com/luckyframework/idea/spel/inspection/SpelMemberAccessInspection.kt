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
 * SpEL `.` 成员访问检查（specs spel-diagnostics-quickfix）。
 *
 * - 接收者类型可静态解析且成员不存在 → error（`#{user.d}`、
 *   `#{user.m4()}`、`$this$.bag3()` 在运行期一定失败）；
 * - 接收者类型明确是 `Object` → warning，只提示无法静态校验；
 * - `Map`、泛型类型变量、数组与无法推导的接收者保守放行。
 *
 * 只在可确定上下文中生效：字符串必须位于注解元数据表登记过的 SpEL 属性内，
 * 且所在模块依赖 lucky-httpclient。
 */
class SpelMemberAccessInspection : LocalInspectionTool() {

    private val contextBuilder = SpelContextBuilder()

    private val checker = SpelMemberAccessChecker()

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
            // 成员确定不存在是运行期一定失败的访问；Object 接收者只提示无法校验
            val highlight = when (problem.kind) {
                SpelMemberProblemKind.MEMBER_NOT_FOUND -> ProblemHighlightType.GENERIC_ERROR
                SpelMemberProblemKind.MEMBER_UNVERIFIABLE -> ProblemHighlightType.WARNING
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
