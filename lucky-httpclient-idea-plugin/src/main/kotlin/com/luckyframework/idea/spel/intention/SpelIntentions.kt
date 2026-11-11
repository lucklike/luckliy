package com.luckyframework.idea.spel.intention

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil

/**
 * 「包裹为 SpEL 表达式」（12.6）。
 *
 * 把字符串字面量内的选中文本包成 `#{...}`；已包裹的选区不重复提供。
 */
class SpelWrapIntention : BaseIntentionAction() {

    override fun getText(): String = "包裹为 SpEL 表达式"

    override fun getFamilyName(): String = "SpEL 表达式"

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val literal = stringLiteralAt(editor, file) ?: return false
        val selection = selectionIn(editor, literal) ?: return false
        val selected = editor.document.getText(selection)
        return !(selected.startsWith("#{") && selected.endsWith("}"))
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val literal = stringLiteralAt(editor, file) ?: return
        val selection = selectionIn(editor, literal) ?: return
        val document = editor.document
        val selected = document.getText(selection)
        document.replaceString(selection.startOffset, selection.endOffset, "#{$selected}")
    }
}

/**
 * 「取消 SpEL 包裹」（12.6）。
 *
 * 把选中的 `#{...}` 还原为内部表达式；只处理完整覆盖选区的情况。
 */
class SpelUnwrapIntention : BaseIntentionAction() {

    override fun getText(): String = "取消 SpEL 包裹"

    override fun getFamilyName(): String = "SpEL 表达式"

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val literal = stringLiteralAt(editor, file) ?: return false
        val selection = selectionIn(editor, literal) ?: return false
        return isWrapped(editor.document.getText(selection))
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val literal = stringLiteralAt(editor, file) ?: return
        val selection = selectionIn(editor, literal) ?: return
        val document = editor.document
        val selected = document.getText(selection)
        if (!isWrapped(selected)) {
            return
        }
        document.replaceString(
            selection.startOffset,
            selection.endOffset,
            selected.substring(2, selected.length - 1)
        )
    }
}

private fun isWrapped(text: String): Boolean =
    text.length > 3 && text.startsWith("#{") && text.endsWith("}")

/** 选区起点/光标所在字符串字面量（仅 String 值）。 */
internal fun stringLiteralAt(editor: Editor, file: PsiFile): PsiLiteralExpression? {
    if (file !is PsiJavaFile) {
        return null
    }
    val model = editor.selectionModel
    val offset = (if (model.hasSelection()) model.selectionStart else editor.caretModel.offset)
        .coerceIn(0, maxOf(0, file.textLength - 1))
    val element = file.findElementAt(offset) ?: return null
    val literal = PsiTreeUtil.getParentOfType(element, PsiLiteralExpression::class.java, false)
        ?: return null
    return literal.takeIf { it.value is String }
}

/** 选区必须完整落在字面量内容中（不含起始/结束引号）。 */
internal fun selectionIn(editor: Editor, literal: PsiLiteralExpression): TextRange? {
    val model = editor.selectionModel
    if (!model.hasSelection()) {
        return null
    }
    val text = literal.text
    val quoted = text.length >= 2 && text.first() == '"' && text.last() == '"'
    val from = literal.textRange.startOffset + if (quoted) 1 else 0
    val to = literal.textRange.endOffset - if (quoted) 1 else 0
    val start = model.selectionStart
    val end = model.selectionEnd
    if (start < from || end > to || start >= end) {
        return null
    }
    return TextRange(start, end)
}
