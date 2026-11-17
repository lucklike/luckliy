package com.luckyframework.idea.spel.inspection.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLiteralExpression

/**
 * 快速修复：把函数名属性值改为同名方法声明的 `@FunctionAlias` 别名。
 *
 * 函数名属性按方法名/别名解析函数表，带 `@FunctionAlias` 的方法只登记
 * 别名：此时按方法名书写必然解析失败，导入方法所属类也不会生效，
 * 正确做法是把值改成别名。
 */
class SpelUseFunctionAliasFix(
    private val functionName: String,
    private val alias: String
) : LocalQuickFix {

    override fun getFamilyName(): String = "改用函数别名"

    override fun getName(): String = "改用别名 '$alias'"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val literal = descriptor.psiElement as? PsiLiteralExpression ?: return
        // 高亮按快照登记，写入前确认字面量仍是待修复的函数名，避免误改
        if (literal.value != functionName) {
            return
        }
        val range = ElementManipulators.getValueTextRange(literal)
        ElementManipulators.getManipulator(literal)?.handleContentChange(literal, range, alias)
    }
}
