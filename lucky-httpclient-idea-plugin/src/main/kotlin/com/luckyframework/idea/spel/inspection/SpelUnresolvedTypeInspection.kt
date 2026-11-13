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
 * `T(...)` 与 `new` 之后的类型引用检查。
 *
 * - `T(com.example.User)` 括号内的类型不存在 → error；
 * - `new com.example.Pair(...)` 构造的类型不存在 → error；
 * - `#value instanceof T(com.example.User)` 判断的类型不存在 → error。
 *
 * SpEL 的 `instanceof` 右操作数必须是 `T(...)`，裸类名由语法检查报告。
 *
 * 只在可确定上下文中生效：字符串必须位于注解元数据表登记过的 SpEL 属性内，
 * 且所在模块依赖 lucky-httpclient。
 */
class SpelUnresolvedTypeInspection : LocalInspectionTool() {

    private val contextBuilder = SpelContextBuilder()

    private val checker = SpelTypeReferenceChecker()

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
            // 类型不存在时表达式在运行期一定失败，按错误报告
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
