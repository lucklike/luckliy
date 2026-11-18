package com.luckyframework.idea.std

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil

/**
 * 缺失配置的 `@StdHttpClient` 接口上提供「生成 StdHttpClient 配置」意图动作。
 *
 * 与缺失行标记的点击行为一致：选择目标配置文件后写入最小化配置。
 * 只在光标最内层声明是接口（而非方法/字段）且接口没有配置时出现。
 */
class StdGenerateConfigIntention : IntentionAction, LowPriorityAction {

    override fun getText(): String = "生成 StdHttpClient 配置"

    override fun getFamilyName(): String = "Lucky HttpClient"

    override fun startInWriteAction(): Boolean = false

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file !is PsiJavaFile || !StdConfigGenerator.isAvailable(project)) {
            return false
        }
        val psiClass = clientClassAt(editor, file) ?: return false
        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass) ?: return false
        return entry.configNodes.isEmpty()
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        if (editor == null || file == null) {
            return
        }
        val psiClass = clientClassAt(editor, file) ?: return
        val entry = StdHttpClientIndex.getInstance(project).entryOf(psiClass) ?: return
        if (entry.configNodes.isNotEmpty()) {
            return
        }
        StdConfigGenerator.chooseTarget(project, editor, entry.configId)
    }

    /**
     * 光标处最内层的声明节点必须是接口。
     *
     * 用 [PsiMember] 作为最近声明：光标在方法体或字段上时最内层是方法/字段，
     * 意图不会在这些位置出现。
     */
    private fun clientClassAt(editor: Editor, file: PsiFile): PsiClass? {
        val element = file.findElementAt(editor.caretModel.offset) ?: return null
        val member = PsiTreeUtil.getParentOfType(element, PsiMember::class.java, false) ?: return null
        val psiClass = member as? PsiClass ?: return null
        if (psiClass.isAnnotationType || psiClass.isInterface.not()) {
            return null
        }
        return psiClass
    }
}
