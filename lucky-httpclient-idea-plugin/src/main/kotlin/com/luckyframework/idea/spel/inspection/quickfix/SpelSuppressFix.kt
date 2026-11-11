package com.luckyframework.idea.spel.inspection.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.luckyframework.idea.spel.inspection.SpelSuppression

/**
 * 快速修复：为单条检查结果添加 `@SuppressWarnings` 抑制标记（12.7）。
 *
 * 标记使用检查的 `shortName`，由检查自身在 `isSuppressedFor` 中识别。
 */
class SpelSuppressFix(private val inspectionId: String) : LocalQuickFix {

    override fun getFamilyName(): String = "抑制此提示"

    override fun getName(): String = "抑制此提示（@SuppressWarnings）"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val owner = SpelSuppression.owner(descriptor.psiElement) ?: return
        SpelSuppression.addSuppression(owner, inspectionId)
    }
}
