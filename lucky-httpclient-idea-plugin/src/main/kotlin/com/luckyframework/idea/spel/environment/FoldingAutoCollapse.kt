package com.luckyframework.idea.spel.environment

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.ex.FoldingModelEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.util.Alarm

/**
 * 打开文件后自动收起 `${key}` 的解析值折叠区域。
 *
 * IDE 2026.x 会从持久化折叠缓存恢复每个文件的折叠状态；缓存中不存在的新
 * 区域不会套用 `isCollapsedByDefault`，而是保持展开，导致
 * [SpringPlaceholderFoldingBuilder] 生成的区域在界面上看不到配置值。
 *
 * 这里在编辑器创建后短轮询折叠模型，发现覆盖完整 `${...}` 的区域就批量收起；
 * 收起后停止轮询，用户随后手动展开不再被干预。
 */
class FoldingAutoCollapse : EditorFactoryListener {

    private val log = Logger.getInstance(FoldingAutoCollapse::class.java)

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        if (!editor.document.text.contains("\${")) {
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
        val text = editor.document.getText(TextRange(region.startOffset, region.endOffset))
        return isPlaceholderFoldRegion(text, region.placeholderText)
    }

    internal companion object {
        private const val INITIAL_DELAY_MS = 500
        private const val RETRY_DELAY_MS = 300
        private const val MAX_ATTEMPTS = 20

        /** 区域文本是完整的 `${...}` 且占位文本非空：只有本插件会生成这种区域。 */
        fun isPlaceholderFoldRegion(text: String, placeholder: String?): Boolean =
            !placeholder.isNullOrEmpty() && text.startsWith("\${") && text.endsWith("}")
    }
}
