package com.luckyframework.idea.spel.inspection.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiLiteralExpression
import com.luckyframework.idea.spel.symbol.SpelImportParser

/**
 * 快速修复：为未解析变量插入 `@SpELImport(var = "name=null")`（12.4）。
 *
 * 变量名原样保留；默认落在当前方法上，也可选择当前类。
 */
class SpelCreateVariableFix(
    private val variableName: String,
    private val preferClass: Boolean
) : LocalQuickFix {

    override fun getFamilyName(): String = "创建 SpEL 变量声明"

    override fun getName(): String = if (preferClass) {
        "创建 SpEL 变量声明（当前类）"
    } else {
        "创建 SpEL 变量声明（当前方法）"
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val literal = descriptor.psiElement as? PsiLiteralExpression ?: return
        val owner = SpelAnnotationInsertion.owner(literal, preferClass) ?: return
        val escaped = variableName.replace("\\", "\\\\").replace("\"", "\\\"")
        SpelAnnotationInsertion.add(
            owner,
            "@${SpelImportParser.SPEL_IMPORT}(var = \"$escaped=null\")"
        )
    }
}
