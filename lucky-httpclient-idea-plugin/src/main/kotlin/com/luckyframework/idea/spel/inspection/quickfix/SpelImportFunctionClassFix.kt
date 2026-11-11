package com.luckyframework.idea.spel.inspection.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteralExpression
import com.luckyframework.idea.spel.symbol.SpelImportParser

/**
 * 快速修复：把静态方法所属类加入 `@SpELImport`（12.5）。
 *
 * 同一个类可能同时提供多个同名函数，由检查为每个候选类生成一个修复。
 */
class SpelImportFunctionClassFix(
    private val functionName: String,
    private val targetClass: PsiClass
) : LocalQuickFix {

    override fun getFamilyName(): String = "导入 SpEL 函数类"

    override fun getName(): String =
        "导入函数类 '${targetClass.name ?: targetClass.qualifiedName}'（#${functionName}）"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val literal = descriptor.psiElement as? PsiLiteralExpression ?: return
        val qualifiedName = targetClass.qualifiedName ?: return
        val owner = SpelAnnotationInsertion.owner(literal, false) ?: return
        SpelAnnotationInsertion.add(owner, "@${SpelImportParser.SPEL_IMPORT}($qualifiedName.class)")
    }
}
