package com.luckyframework.idea.spel.environment

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
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.yaml.SpelYamlSupport

/**
 * 输入 `${` 或继续输入键名时自动弹出环境变量键补全。
 *
 * Java/YAML 字符串字面量内平台不会自动触发补全；这里在 `${` 起始位置
 * 与键名字符输入时弹出，未处于键名范围（如默认值）时保持默认行为。
 */
class SpringPlaceholderAutoPopupHandler : TypedHandlerDelegate() {

    private val resolver = SpelAnnotationResolver()

    override fun charTyped(char: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file !is PsiJavaFile || !isPlaceholderTriggerChar(char)) {
            return Result.CONTINUE
        }
        if (LookupManager.getActiveLookup(editor) != null) {
            return Result.CONTINUE
        }
        val document = editor.document
        val offset = editor.caretModel.offset
        if (!startsPlaceholder(char, document.charsSequence, offset)) {
            return Result.CONTINUE
        }
        PsiDocumentManager.getInstance(project).commitDocument(document)
        val element = file.findElementAt(offset - 1) ?: return Result.CONTINUE
        val literal = PsiTreeUtil.getParentOfType(
            element,
            PsiLiteralExpression::class.java,
            false
        ) ?: return Result.CONTINUE
        val offsetInLiteral = offset - literal.textRange.startOffset
        if (SpringPlaceholderParser.placeholderAt(literal.text, offsetInLiteral) == null) {
            return Result.CONTINUE
        }
        val service = SpringEnvironmentService.getInstance(project)
        if (!service.isEnabledFor(literal) || !resolver.isSpelLiteral(literal)) {
            return Result.CONTINUE
        }
        schedulePopup(project, editor)
        return Result.CONTINUE
    }
}

/**
 * YAML 值中 `${` 与键名字符的自动弹出（YAML 插件存在时加载）。
 *
 * YAML 文件在普通 Java 项目中很常见，因此只在标量已经属于
 * `${...}` 键名范围时触发，不对普通 YAML 值弹补全。
 */
class SpringPlaceholderYamlAutoPopupHandler : TypedHandlerDelegate() {

    override fun charTyped(char: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (!isPlaceholderTriggerChar(char)) {
            return Result.CONTINUE
        }
        if (LookupManager.getActiveLookup(editor) != null) {
            return Result.CONTINUE
        }
        val document = editor.document
        val offset = editor.caretModel.offset
        if (!startsPlaceholder(char, document.charsSequence, offset)) {
            return Result.CONTINUE
        }
        PsiDocumentManager.getInstance(project).commitDocument(document)
        val element = file.findElementAt(offset - 1) ?: return Result.CONTINUE
        val scalar = SpelYamlSupport.scalarAt(element) ?: return Result.CONTINUE
        if (!SpelYamlSupport.isPlaceholderValue(scalar)) {
            return Result.CONTINUE
        }
        val offsetInScalar = offset - scalar.textRange.startOffset
        if (SpringPlaceholderParser.placeholderAt(scalar.text, offsetInScalar) == null) {
            return Result.CONTINUE
        }
        val service = SpringEnvironmentService.getInstance(project)
        if (!service.isEnabledFor(scalar)) {
            return Result.CONTINUE
        }
        schedulePopup(project, editor)
        return Result.CONTINUE
    }
}

/** 键名中会持续触发补全的字符；`{` 只在紧跟 `$` 时算 `${`。 */
private fun isPlaceholderTriggerChar(char: Char): Boolean =
    char == '{' || char.isLetterOrDigit() || char == '_' || char == '.' || char == '-'

/**
 * 已输入的字符是否可能处于 `${` 之后。
 *
 * `{` 必须紧跟 `$`；其他键名字符只在光标前窗口内出现过 `${` 时继续，
 * 避免在普通字符串中每次输入都提交文档、解析 PSI。
 */
private fun startsPlaceholder(char: Char, text: CharSequence, offset: Int): Boolean {
    if (char == '{') {
        return offset >= 2 && text[offset - 2] == '$'
    }
    val from = (offset - PLACEHOLDER_WINDOW).coerceAtLeast(0)
    return text.substring(from, offset).contains("\${")
}

/** 光标前用于判断占位符上下文的文本窗口长度。 */
private const val PLACEHOLDER_WINDOW = 512

private fun schedulePopup(project: Project, editor: Editor) {
    ApplicationManager.getApplication().invokeLater {
        if (!editor.isDisposed && !project.isDisposed) {
            CodeCompletionHandlerBase(CompletionType.BASIC, true, false, false)
                .invokeCompletion(project, editor, 1, false)
        }
    }
}
