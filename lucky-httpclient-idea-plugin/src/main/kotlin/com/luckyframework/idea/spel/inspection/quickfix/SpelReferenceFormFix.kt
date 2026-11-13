package com.luckyframework.idea.spel.inspection.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

/**
 * 快速修复：在 `#name` 与裸 `name` 之间切换变量引用形式。
 *
 * Root 变量（方法参数、`@SpELImport(root = ...)`）只能用裸名称引用，
 * 普通变量（`@SpELImport(var = ...)`）必须带 `#`；内置函数只支持裸
 * `name(...)` 调用。修复按问题范围增删一个 `#`，不改变名称本身。
 */
sealed class SpelReferenceFormFix(private val addHash: Boolean) : LocalQuickFix {

    override fun getFamilyName(): String = "调整 SpEL 引用形式"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val range = descriptor.textRangeInElement ?: return
        val file = element.containingFile ?: return
        val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
        val offset = element.textRange.startOffset + range.startOffset
        if (addHash) {
            document.insertString(offset, "#")
        } else {
            if (offset >= element.textRange.endOffset || document.charsSequence[offset] != '#') {
                return
            }
            document.deleteString(offset, offset + 1)
        }
        PsiDocumentManager.getInstance(project).commitDocument(document)
    }

    /** 去掉 `#`，改为裸名称引用。 */
    class RemoveHash : SpelReferenceFormFix(false) {
        override fun getName(): String = "改为裸名称引用"
    }

    /** 补上 `#`，改为普通变量引用。 */
    class AddHash : SpelReferenceFormFix(true) {
        override fun getName(): String = "改为 #name 引用"
    }
}
