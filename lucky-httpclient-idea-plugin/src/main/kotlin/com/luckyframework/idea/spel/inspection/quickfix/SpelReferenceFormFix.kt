package com.luckyframework.idea.spel.inspection.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager

/**
 * 快速修复：调整变量/函数的引用形式。
 *
 * Root 变量（方法参数、`@SpELImport(root = ...)`）只能用裸名称引用，
 * 普通变量（`@SpELImport(var = ...)`）必须带 `#`；带命名空间的函数只能
 * `#ns.name(...)` 调用。前两种按问题范围增删一个 `#`，命名空间函数
 * 在 `#` 后补 `ns.`，都不改变名称本身。
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

    /**
     * 命名空间函数写成 `#name(...)`：在 `#` 后补 `namespace.`，
     * 如 `#sha('a')` → `#crypto.sha('a')`。
     */
    class QualifyNamespace(private val namespace: String, private val name: String) : LocalQuickFix {

        override fun getFamilyName(): String = "调整 SpEL 引用形式"

        override fun getName(): String = "改为 #$namespace.$name 引用"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement ?: return
            val range = descriptor.textRangeInElement ?: return
            val file = element.containingFile ?: return
            val document = PsiDocumentManager.getInstance(project).getDocument(file) ?: return
            val offset = element.textRange.startOffset + range.startOffset
            if (offset >= element.textRange.endOffset || document.charsSequence[offset] != '#') {
                return
            }
            document.insertString(offset + 1, "$namespace.")
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }
}
