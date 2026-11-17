package com.luckyframework.idea.spel.intention

import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.inspection.quickfix.SpelAnnotationInsertion
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.symbol.SpelContextBuilder
import com.luckyframework.idea.spel.symbol.SpelImportParser

/**
 * 「提取为 SpEL 变量」（13.3）。
 *
 * 把选中的 SpEL 表达式提升为 `@SpELImport` 变量声明，并在原位置换成
 * 变量引用。选区覆盖整个片段时替换 `#{...}`，只覆盖片段内容时保持外层
 * 定界符；选区是表达式的一部分时只替换该部分为 `#name`。
 */
class SpelExtractVariableIntention : BaseIntentionAction() {

    override fun getText(): String = "提取为 SpEL 变量"

    override fun getFamilyName(): String = "SpEL 重构"

    override fun startInWriteAction(): Boolean = true

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val literal = stringLiteralAt(editor, file) ?: return false
        val selection = selectionIn(editor, literal) ?: return false
        return fragmentOf(literal, selection) != null
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val literal = stringLiteralAt(editor, file) ?: return
        val selection = selectionIn(editor, literal) ?: return
        val fragment = fragmentOf(literal, selection) ?: return
        val document = editor.document
        val literalStart = literal.textRange.startOffset
        val selected = TextRange(selection.startOffset, selection.endOffset)
        val fragmentRange = fragment.textRange.shiftRight(literalStart)
        val contentRange = fragment.expressionRange.shiftRight(literalStart)

        // 选区文本本身就是 Java 字符串中的合法片段，直接复用可保留原转义语义
        val expression = if (selected == fragmentRange) {
            document.getText(selected).removeSurrounding("#{", "}")
        } else {
            document.getText(selected)
        }
        if (expression.isBlank()) {
            return
        }

        val name = freshName(literal)
        val replaceWholeFragment = selected == fragmentRange || selected == contentRange
        val replaceRange = if (replaceWholeFragment) fragmentRange else selected
        val replacement = if (replaceWholeFragment) "#{#$name}" else "#$name"

        document.replaceString(replaceRange.startOffset, replaceRange.endOffset, replacement)
        PsiDocumentManager.getInstance(project).commitDocument(document)

        val updated = PsiTreeUtil.getParentOfType(
            file.findElementAt(replaceRange.startOffset),
            PsiLiteralExpression::class.java,
            false
        ) ?: return
        val owner = SpelAnnotationInsertion.owner(updated, false) ?: return
        SpelAnnotationInsertion.add(
            owner,
            "@${SpelImportParser.SPEL_IMPORT}(var = \"$name=#{$expression}\")"
        )
    }

    /** 选中的表达式所在的片段；嵌套时取最内层。 */
    private fun fragmentOf(literal: PsiLiteralExpression, selection: TextRange): SpelFragment? {
        val start = selection.startOffset - literal.textRange.startOffset
        val end = selection.endOffset - literal.textRange.startOffset
        if (start >= end || start < 0) {
            return null
        }
        fun find(fragments: List<SpelFragment>): SpelFragment? {
            for (fragment in fragments) {
                find(fragment.children)?.let { return it }
                if (fragment.textRange.containsRange(start, end)) {
                    return fragment
                }
            }
            return null
        }
        return find(SpelScanner().scanLiteral(literal.text))
    }

    /** 选择不与现有变量/函数重名的候选名：`t`、`t1`、`t2`…… */
    private fun freshName(literal: PsiLiteralExpression): String {
        val context = SpelContextBuilder().build(literal)
        var index = 0
        while (true) {
            val candidate = if (index == 0) "t" else "t$index"
            // 普通变量与 Root 变量（方法参数）都可能被同名引用，两类都要避开
            val taken = context.anyVariables(candidate).isNotEmpty() ||
                context.functions(candidate, null).isNotEmpty()
            if (!taken) {
                return candidate
            }
            index++
        }
    }
}
