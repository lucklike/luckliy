package com.luckyframework.idea.spel.completion

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver

/**
 * `#{` 编入后、表达式内 `.` 之后以及函数名属性中自动弹出补全。
 *
 * Java 字符串字面量内平台不会自动触发补全，仅注册
 * [SpelCompletionContributor] 时用户必须手动 Ctrl+Space；这里在明确的
 * SpEL 上下文里补上自动弹出，其余位置保持默认行为。
 */
class SpelAutoPopupHandler : TypedHandlerDelegate() {

    private val analyzer = SpelCompletionAnalyzer()

    override fun charTyped(char: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is PsiJavaFile) {
            return Result.CONTINUE
        }
        if (char != '{' && char != '.' && char != '@' &&
            !char.isLetterOrDigit() && char != '_' && char != '$'
        ) {
            return Result.CONTINUE
        }
        // 候选列表已打开时字符由 lookup 接管（typedHandler 不会被回调），
        // 前缀过滤与候选重算由 SpelCompletionProvider 注册的前缀变化重启完成。
        if (LookupManager.getActiveLookup(editor) != null) {
            return Result.CONTINUE
        }
        // typedHandler 触发时文档已变但 PSI 尚未同步，
        // 不提交会看到旧的 literal 文本（仍为 `"#"`）而误判为非 SpEL 上下文。
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        val offset = editor.caretModel.offset
        if (offset <= 0) {
            return Result.CONTINUE
        }
        if (char == '{') {
            // 只有 `#{` 才弹出，普通 `{` 交给平台处理
            val chars = editor.document.charsSequence
            if (offset < 2 || chars[offset - 2] != '#') {
                return Result.CONTINUE
            }
        }
        val element = file.findElementAt(offset - 1) ?: return Result.CONTINUE
        val literal = PsiTreeUtil.getParentOfType(
            element,
            PsiLiteralExpression::class.java,
            false
        ) ?: return Result.CONTINUE
        if (!LuckyHttpClientPresence.getInstance(project).isEnabledFor(literal)) {
            return Result.CONTINUE
        }
        val resolver = SpelAnnotationResolver()
        if (!literal.text.contains("#{")) {
            // 函数名属性（`kind = FUNCTION`）整段就是函数名：输入标识符字符时弹出；
            // `.`/`@`/`{` 在函数名里不是有效输入，不打扰用户。
            // 属性声明已判定为 FUNCTION 才弹出：允许引号未闭合时继续输入。
            if (char != '.' && char != '@' && char != '{' &&
                resolver.isFunctionNameAttribute(literal)
            ) {
                schedulePopup(project, editor)
            }
            return Result.CONTINUE
        }
        // 未标注 @SpelAttribute 的属性运行期不求值：不自动弹出补全
        if (!resolver.isSpelLiteral(literal)) {
            return Result.CONTINUE
        }
        // 只在能识别出 SpEL 片段时弹出（已闭合的 `}` 之后不弹）。
        // 列表已打开时前缀由 lookup 自己过滤，这里只负责重新计算候选，
        // 因此不再限制「标识符只在单词首字符触发」：关掉列表后继续输入
        // （如 `#{md}` 中补 `5`）仍需弹出提示。
        analyzer.analyze(literal.text, offset - literal.textRange.startOffset) ?: return Result.CONTINUE
        schedulePopup(project, editor)
        return Result.CONTINUE
    }

    /**
     * 输入完成后文档已变化，弹出必须推迟到事件处理之外。
     * 字符串字面量内 AutoPopupController 会被 CompletionData 拦截，
     * 因此按显式补全调用（autoPopup=false），与 Ctrl+Space 走同一条路径。
     */
    private fun schedulePopup(project: Project, editor: Editor) {
        ApplicationManager.getApplication().invokeLater {
            if (!editor.isDisposed && !project.isDisposed) {
                CodeCompletionHandlerBase(CompletionType.BASIC, true, false, false)
                    .invokeCompletion(project, editor, 1, false)
            }
        }
    }
}
