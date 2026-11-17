package com.luckyframework.idea.spel.refactoring

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenameHandler

/**
 * 注解字符串内 SpEL 名字的重命名入口（13.1/13.2）。
 *
 * `@FunctionAlias("sha")` 的别名与 `@SpELImport(var = "token=...")` 的变量名
 * 位于字符串字面量内部，平台默认的重命名可用性依赖目标元素推断，Rename
 * 动作可能显示为灰色。这里在光标位于名字片段内时显式声明可用，实际重命名
 * 交给 [RenameDialog] 与 [SpelRenameProcessor] 处理。
 *
 * 只在名字片段内接管；光标位于引号或其他位置时不可用，不影响普通重命名。
 */
class SpelAliasRenameHandler(
    private val processor: SpelRenameProcessor = SpelRenameProcessor()
) : RenameHandler {

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean =
        literalAt(dataContext) != null

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext) {
        val targetEditor = editor ?: dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val targetFile = file ?: dataContext.getData(CommonDataKeys.PSI_FILE) ?: return
        val literal = literalAt(targetEditor, targetFile) ?: return
        RenameDialog(project, literal, null, targetEditor).show()
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val file = dataContext.getData(CommonDataKeys.PSI_FILE) ?: return
        val literal = literalAt(editor, file) ?: return
        RenameDialog(project, literal, null, editor).show()
    }

    /** 多个 handler 同时可用时菜单展示的标题；唯一 handler 时平台直接调用 [invoke]。 */
    override fun toString(): String = "重命名 SpEL 名称"

    private fun literalAt(dataContext: DataContext): PsiLiteralExpression? {
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return null
        val file = dataContext.getData(CommonDataKeys.PSI_FILE) ?: return null
        return literalAt(editor, file)
    }

    private fun literalAt(editor: Editor, file: PsiFile): PsiLiteralExpression? {
        val element = file.findElementAt(editor.caretModel.offset) ?: return null
        val literal = PsiTreeUtil.getParentOfType(element, PsiLiteralExpression::class.java, false)
            ?: return null
        val declaration = processor.targetAt(literal) ?: return null
        val offset = editor.caretModel.offset - literal.textRange.startOffset
        // 光标必须落在名字片段内：注解值可能带前缀或表达式，整段替换会破坏内容
        return literal.takeIf { declaration.nameRange.containsOffset(offset) }
    }
}
