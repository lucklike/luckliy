package com.luckyframework.idea.spel.environment

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Alarm
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelInjection
import com.luckyframework.idea.spel.metadata.SpelInjectionScope

/**
 * 打开文件后自动收起本插件生成的折叠区域：`${key}` 的解析值与 `@Env` 注入名的真实配置值。
 *
 * IDE 2026.x 会从持久化折叠缓存恢复每个文件的折叠状态；缓存中不存在的新
 * 区域不会套用 `isCollapsedByDefault`，而是保持展开，导致
 * [SpringPlaceholderFoldingBuilder] 生成的区域在界面上看不到配置值。
 *
 * 这里在编辑器创建后短轮询折叠模型，发现覆盖完整 `${...}` 或 `@Env` 注入名的
 * 区域就批量收起；收起后停止轮询，用户随后手动展开不再被干预。
 */
class FoldingAutoCollapse : EditorFactoryListener {

    private val log = Logger.getInstance(FoldingAutoCollapse::class.java)

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val text = editor.document.text
        // 与 SpringPlaceholderFoldingBuilder 的预筛保持一致：
        // `@Var(type = VarType.ENVIRONMENT)` 写的是全大写枚举名
        if (!text.contains("\${") && !text.contains("Env") && !text.contains("ENVIRONMENT")) {
            return
        }
        val fileName = FileDocumentManager.getInstance().getFile(editor.document)?.name ?: "<unnamed>"
        val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD)
        val task = object : Runnable {
            private var attempts = 0

            override fun run() {
                if (editor.isDisposed) {
                    return
                }
                attempts++
                if (collapse(editor, fileName) || attempts >= MAX_ATTEMPTS) {
                    return
                }
                alarm.addRequest(this, RETRY_DELAY_MS)
            }
        }
        alarm.addRequest(task, INITIAL_DELAY_MS)
    }

    /** 收起本插件生成的区域；返回 false 表示区域还没构建出来，需要继续轮询。 */
    private fun collapse(editor: Editor, fileName: String): Boolean {
        val model = editor.foldingModel as? FoldingModelEx ?: return true
        val targets = model.allFoldRegions.filter { it.isExpanded && isPlaceholderRegion(editor, it) }
        if (targets.isEmpty()) {
            return false
        }
        model.runBatchFoldingOperation {
            targets.forEach { it.isExpanded = false }
        }
        log.info("[LuckyFold] auto-collapse file=$fileName count=${targets.size}")
        return true
    }

    private fun isPlaceholderRegion(editor: Editor, region: FoldRegion): Boolean {
        val range = TextRange(region.startOffset, region.endOffset)
        val text = editor.document.getText(range)
        if (isPlaceholderFoldRegion(text, region.placeholderText)) {
            return true
        }
        return isEnvInjectionRegion(editor, region, range)
    }

    /** `@Env(...)`/`@Var(type = ENVIRONMENT)` 的注入名区域：按 PSI 精确识别，避免误收其他折叠。 */
    private fun isEnvInjectionRegion(editor: Editor, region: FoldRegion, range: TextRange): Boolean {
        if (region.placeholderText.isNullOrEmpty()) {
            return false
        }
        val project = editor.project ?: return false
        val literal = literalAt(project, editor, range.startOffset) ?: return false
        return isEnvInjectionFoldRegion(literal, RESOLVER.resolveInjection(literal), range)
    }

    private fun literalAt(project: Project, editor: Editor, offset: Int): PsiLiteralExpression? {
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) ?: return null
        val leaf = psiFile.findElementAt(offset) ?: return null
        return PsiTreeUtil.getParentOfType(leaf, PsiLiteralExpression::class.java, false)
    }

    internal companion object {
        private const val INITIAL_DELAY_MS = 500
        private const val RETRY_DELAY_MS = 300
        private const val MAX_ATTEMPTS = 20

        private val RESOLVER = SpelAnnotationResolver()

        /** 区域文本是完整的 `${...}` 且占位文本非空：只有本插件会生成这种区域。 */
        fun isPlaceholderFoldRegion(text: String, placeholder: String?): Boolean =
            !placeholder.isNullOrEmpty() && text.startsWith("\${") && text.endsWith("}")

        /**
         * 区域恰好覆盖注入名字面量引号内的文本，且该字面量是显式书写的 ENV 注入名：
         * 与 [EnvInjectionFoldRegions] 生成折叠区域的条件保持一致。
         */
        fun isEnvInjectionFoldRegion(
            literal: PsiLiteralExpression,
            injection: SpelInjection?,
            range: TextRange
        ): Boolean {
            if (injection == null || injection.scope != SpelInjectionScope.ENV || !injection.explicitName) {
                return false
            }
            if (injection.referenceTarget != literal) {
                return false
            }
            val nameRange = ElementManipulators.getValueTextRange(literal).shiftRight(literal.textRange.startOffset)
            return nameRange == range
        }
    }
}
