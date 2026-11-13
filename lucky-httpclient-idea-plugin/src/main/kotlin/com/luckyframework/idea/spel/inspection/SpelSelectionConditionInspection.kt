package com.luckyframework.idea.spel.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.inspection.quickfix.SpelSuppressFix
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelValueKind
import com.luckyframework.idea.spel.symbol.SpelContextBuilder

/**
 * SpEL 条件表达式类型检查。
 *
 * 两类位置的条件结果必须是 `boolean`/`Boolean`：
 * - 选择表达式 `?[...]`/`^[...]`/`$[...]` 的过滤条件；
 * - 条件属性（`@If#condition`、`@Retryable#condition` 等元数据登记为
 *   `IF_CONDITION` 的属性），包含裸表达式 `condition = "false"` 与单个
 *   `#{...}` 片段。
 *
 * 类型能静态推导且不是布尔 → error（`#{users.?[name]}`、
 * `@If(condition = "#{1+2}")` 在运行期一定失败）；比较/逻辑表达式的结果
 * 一定是 boolean；无法推导的条件（未知符号等）保守放行。
 *
 * 只在可确定上下文中生效：字符串必须位于注解元数据表登记过的 SpEL 属性内，
 * 且所在模块依赖 lucky-httpclient。
 */
class SpelSelectionConditionInspection : LocalInspectionTool() {

    private val contextBuilder = SpelContextBuilder()

    private val checker = SpelSelectionConditionChecker()

    private val annotationResolver = SpelAnnotationResolver()

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {
            override fun visitLiteralExpression(expression: PsiLiteralExpression) {
                inspect(holder, expression)
            }
        }

    override fun isSuppressedFor(element: PsiElement): Boolean =
        super.isSuppressedFor(element) || SpelSuppression.isSuppressed(element, shortName)

    private fun inspect(holder: ProblemsHolder, literal: PsiLiteralExpression) {
        if (!LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)) {
            return
        }
        val conditionAttribute = isConditionAttribute(literal)
        val context = contextBuilder.build(literal)
        if (!context.known && !conditionAttribute) {
            return
        }
        if (conditionAttribute) {
            report(holder, literal, checker.checkCondition(literal.text, context, literal.project))
        }
        if (!literal.text.contains("#{") || !context.known) {
            return
        }
        report(holder, literal, checker.check(literal.text, context, literal.project))
    }

    /** 字面量是否位于 `IF_CONDITION` 属性（`@If#condition`、`@Retryable#condition` 等）。 */
    private fun isConditionAttribute(literal: PsiLiteralExpression): Boolean {
        if (annotationResolver.resolveLiteral(literal)?.kind == SpelValueKind.IF_CONDITION) {
            return true
        }
        val pair = PsiTreeUtil.getParentOfType(literal, PsiNameValuePair::class.java, true) ?: return false
        if ((pair.name ?: "value") != "condition") {
            return false
        }
        val annotation = pair.parent?.parent as? PsiAnnotation ?: return false
        return annotation.qualifiedName == IF_ANNOTATION
    }

    private fun report(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        problems: List<SpelConditionProblem>
    ) {
        for (problem in problems) {
            holder.registerProblem(
                literal,
                problem.message,
                ProblemHighlightType.GENERIC_ERROR,
                problem.range,
                SpelSuppressFix(shortName)
            )
        }
    }

    private companion object {
        const val IF_ANNOTATION = "com.luckyframework.httpclient.proxy.spel.If"
    }
}
