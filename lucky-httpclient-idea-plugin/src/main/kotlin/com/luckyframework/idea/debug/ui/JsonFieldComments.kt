package com.luckyframework.idea.debug.ui

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayModel
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.luckyframework.idea.debug.invoke.DebugJsonDocumentation
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import javax.swing.Icon

/**
 * JSON 字段注释：字段行尾的只读说明。
 *
 * 注释不写进 JSON 文本（模板与结果正文保持纯 JSON），而是按 JSON 属性路径
 * 匹配字段文档，用行尾 inlay 绘制 `// 说明`；开关关闭或文本变化时整体重建，
 * 避免过期注释停留在编辑器中。
 */
internal object JsonFieldComments {

    /** 工具栏图标 `[#]`：颜色与平台图标一致，选中背景交给 ActionButton 绘制。 */
    fun icon(): Icon = CommentToggleIcon

    /**
     * 按当前开关重建行尾注释。
     *
     * [comments] 为 JSON 属性路径 → 说明，路径规则与 [DebugJsonDocumentation]
     * 一致；每次先移除旧 inlay，再按最新文本与折叠重建。
     */
    fun refresh(editor: Editor, comments: Map<String, String>, show: Boolean) {
        if (editor.isDisposed) {
            return
        }
        if (!show && editor.getUserData(COMMENTS_INSTALLED) != true) {
            // 关闭且从未装过注释：每次击键都不必扫描 inlay
            return
        }
        val model = editor.inlayModel
        removeAll(model, editor)
        editor.putUserData(COMMENTS_INSTALLED, null)
        if (!show || comments.isEmpty()) {
            return
        }
        val document = editor.document
        if (document.textLength == 0) {
            return
        }
        val project = editor.project
        val psiManager = project?.let { PsiDocumentManager.getInstance(it) } ?: return
        if (!psiManager.isCommitted(document)) {
            // 注释按最新 PSI 路径匹配，未提交时先同步
            psiManager.commitDocument(document)
        }
        val font = commentFont(editor)
        val color = commentColor(editor)
        val entries = ApplicationManager.getApplication().runReadAction(
            Computable<List<Pair<Int, String>>> {
                val psiFile = psiManager.getPsiFile(document) as? JsonFile
                    ?: return@Computable emptyList()
                // 同一行有多个属性时（单行 JSON）只保留首条，避免注释堆叠
                val lineEnds = HashSet<Int>()
                PsiTreeUtil.collectElementsOfType(psiFile, JsonProperty::class.java)
                    .sortedBy { it.textOffset }
                    .mapNotNull { property ->
                        val text = comments[DebugJsonDocumentation.pathOf(property)]
                            ?: return@mapNotNull null
                        val lineEnd = document.getLineEndOffset(
                            document.getLineNumber(property.textOffset)
                        )
                        if (lineEnds.add(lineEnd)) lineEnd to text else null
                    }
            }
        )
        for ((offset, text) in entries) {
            model.addAfterLineEndElement(offset, true, CommentRenderer(text, font, color))
        }
        if (entries.isNotEmpty()) {
            editor.putUserData(COMMENTS_INSTALLED, true)
        }
    }

    /** 编辑器中当前挂着的字段注释数量。 */
    fun count(editor: Editor): Int {
        if (editor.isDisposed) {
            return 0
        }
        val document = editor.document
        return editor.inlayModel
            .getAfterLineEndElementsInRange(0, document.textLength, CommentRenderer::class.java)
            .size
    }

    private fun removeAll(model: InlayModel, editor: Editor) {
        if (editor.isDisposed) {
            return
        }
        val length = editor.document.textLength
        val stale = buildList {
            addAll(model.getAfterLineEndElementsInRange(0, length, CommentRenderer::class.java))
            // 兼容上一版的块状注释，切换版本后旧 inlay 也能被清掉
            addAll(model.getBlockElementsInRange(0, length, CommentRenderer::class.java))
        }
        for (inlay in stale) {
            Disposer.dispose(inlay)
        }
    }

    /**
     * 注释字体：使用 UI 字体而不是编辑器等宽字体。
     *
     * JetBrains Mono 等编辑器字体通常不含中文字形，直接绘制会出现方块；
     * UI 字体自带中文回退，字号与正文小一号。
     */
    private fun commentFont(editor: Editor): Font {
        val size = (editor.colorsScheme.editorFontSize - 1).coerceAtLeast(10)
        return JBFont.label().deriveFont(Font.PLAIN, size.toFloat())
    }

    /** 注释颜色：跟随编辑器 scheme 的行注释色，取不到时用弱化前景色。 */
    private fun commentColor(editor: Editor): Color =
        editor.colorsScheme.getAttributes(DefaultLanguageHighlighterColors.LINE_COMMENT)
            ?.foregroundColor
            ?: UIUtil.getInactiveTextColor()

    /** 行尾注释渲染器：固定显示在行末，不响应鼠标与编辑。 */
    private class CommentRenderer(
        text: String,
        private val font: Font,
        private val color: Color
    ) : EditorCustomElementRenderer {

        private val display = "  // " + text.take(MAX_COMMENT_CHARS) +
            if (text.length > MAX_COMMENT_CHARS) "…" else ""

        override fun calcWidthInPixels(inlay: Inlay<*>): Int {
            val metrics = inlay.editor.contentComponent.getFontMetrics(font)
            return metrics.stringWidth(display)
        }

        override fun paint(
            inlay: Inlay<*>,
            g: Graphics,
            targetRegion: Rectangle,
            textAttributes: TextAttributes
        ) {
            val g2 = g.create() as Graphics2D
            try {
                g2.font = font
                g2.color = color
                val metrics = g2.fontMetrics
                val baseline = targetRegion.y +
                    (targetRegion.height - metrics.height) / 2 + metrics.ascent
                g2.drawString(display, targetRegion.x, baseline)
            } finally {
                g2.dispose()
            }
        }
    }

    /** 行尾注释的最大字符数：超出时截断，完整内容仍在悬停文档中。 */
    private const val MAX_COMMENT_CHARS = 120

    /** 编辑器上是否已挂着本功能的注释 inlay，用于关闭态快速跳过。 */
    private val COMMENTS_INSTALLED = Key.create<Boolean>("lucky.json.fieldComments.installed")

    /** `[#]` 图标：颜色与平台工具栏图标一致，选中态背景由平台 ActionButton 提供。 */
    private object CommentToggleIcon : Icon {

        /** 深色主题纯白、浅色主题深灰，与工具栏图标前景一致。 */
        private val FOREGROUND = JBColor(Color(0x333333), Color(0xFFFFFF))

        override fun getIconWidth(): Int = JBUI.scale(16)

        override fun getIconHeight(): Int = JBUI.scale(16)

        override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                )
                g2.font = Font(Font.SANS_SERIF, Font.BOLD, JBUI.scale(9))
                g2.color = FOREGROUND
                val text = "[#]"
                val metrics = g2.fontMetrics
                val tx = x + (iconWidth - metrics.stringWidth(text)) / 2
                val ty = y + (iconHeight - metrics.height) / 2 + metrics.ascent
                g2.drawString(text, tx, ty)
            } finally {
                g2.dispose()
            }
        }
    }
}