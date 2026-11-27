package com.luckyframework.idea.debug.ui

import com.google.gson.JsonParser
import com.intellij.icons.AllIcons
import com.intellij.json.JsonFileType
import com.intellij.json.editor.folding.JsonFoldingBuilder
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.luckyframework.idea.debug.invoke.DebugJsonFormatter
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.datatransfer.StringSelection
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPanel

/**
 * Result 页的只读结果视图。
 *
 * 正文不再用 `JTextArea` 渲染，而是换成与 Logs 相同的平台编辑器：
 * 字体、前景/背景色都取自编辑器 color scheme，两个页签视觉一致。
 * 编辑器左侧固定显示行号；JSON 结果还会装上平台 JSON 折叠区域
 * （`{ 4 keys }` 占位符）与语法高亮，折叠箭头可直接点击。
 *
 * 元信息（Return type / Result kind / Elapsed）从正文挪到正文上方：
 * 这样行号从 JSON 第一行开始计数，复制结果也不会把元信息带进剪贴板。
 */
internal class DebugResultView(private val project: Project) : Disposable {

    private val disposable = Disposer.newDisposable("lucky.debug.result")

    /**
     * 结果 JSON 背后的虚拟 PSI 文件。
     *
     * 折叠区域由 JSON folding builder 基于 PSI 构建；纯 document 没有 PsiFile，
     * 编辑器只会高亮、不会出现折叠箭头。
     */
    private val jsonPsiFile = PsiFileFactory.getInstance(project)
        .createFileFromText(JSON_FILE_NAME, JsonFileType.INSTANCE, "")

    private val jsonFoldingBuilder = JsonFoldingBuilder()

    /** 合并连续的折叠重建请求。 */
    private val foldingPending = AtomicBoolean(false)

    /** 正文软换行开关；与 Logs 一致，默认单行显示，长行横向滚动。 */
    private var wrapLines = false

    /** 字段注释开关；默认关闭，开启后在 JSON 字段行尾显示 Javadoc 说明。 */
    private var showComments = false

    /** 当前结果的字段路径 → 单行说明；没有文档的字段不进入结果。 */
    private var fieldComments: Map<String, String> = emptyMap()

    /** 当前展示的正文卡片，决定工具栏动作是否可用。 */
    private var currentCard = CARD_PLAIN

    private val jsonField = object : EditorTextField(
        jsonPsiFile.viewProvider.document,
        project,
        JsonFileType.INSTANCE
    ) {
        override fun onEditorAdded(editor: Editor) {
            super.onEditorAdded(editor)
            // 编辑器首次创建时文档可能已有内容（结果先于显示写入）
            installFoldRegions(editor)
        }
    }.apply { configureEditor() }

    private val plainField = EditorTextField("", project, PlainTextFileType.INSTANCE)
        .apply { configureEditor() }

    /** 编辑器统一配置：行号 + 折叠栏 + 编辑器字体。 */
    private fun EditorTextField.configureEditor() {
        // 只读 viewer：可选中/复制/折叠，但不能输入
        setViewer(true)
        setOneLineMode(false)
        minimumSize = Dimension(0, 0)
        addSettingsProvider { editor ->
            // 与 Logs 控制台看齐：字体、前景/背景色都取全局编辑器 scheme；
            // EditorTextField 新建的编辑器在未显示时不一定继承它
            editor.colorsScheme = EditorColorsManager.getInstance().globalScheme
            editor.settings.apply {
                isLineNumbersShown = true
                isFoldingOutlineShown = true
                // 结果默认全部展开，折叠全部按钮由用户手动触发
                isAutoCodeFoldingEnabled = false
                isUseSoftWraps = wrapLines
                isCaretRowShown = false
                isWhitespacesShown = false
                isRightMarginShown = false
                additionalLinesCount = 0
                additionalColumnsCount = 0
            }
            editor.foldingModel.isFoldingEnabled = true
            // EditorTextField 默认隐藏滚动条；结果超出可视区后需要能滚动查看
            editor.setVerticalScrollbarVisible(true)
            // 默认单行显示，长行需要横向滚动条
            editor.setHorizontalScrollbarVisible(true)
        }
    }

    // ===== 元信息行 =====

    /** 元信息字体：与编辑器正文同一字体，小一号；仅展示英文。 */
    private val metaFont: Font = runCatching {
        val scheme = EditorColorsManager.getInstance().globalScheme
        Font(scheme.editorFontName, Font.PLAIN, (scheme.editorFontSize - 1).coerceAtLeast(11))
    }.getOrDefault(UIUtil.getLabelFont())

    /** 编辑器背景：与 Logs 控制台同一 scheme 背景。 */
    private val editorBackground: Color
        get() = runCatching { EditorColorsManager.getInstance().globalScheme.defaultBackground }
            .getOrDefault(UIUtil.getPanelBackground())

    /** 第一行：Return type（过长时裁剪，完整值在 tooltip）。 */
    private val returnTypeLabel = EllipsisLabel().apply {
        font = metaFont
        border = JBUI.Borders.empty(1, 8, 1, 8)
    }

    /** 第二行左侧：结果类型徽章（JSON / TEXT / ERROR）。 */
    private val kindBadge = KindBadge(metaFont)

    /** 第二行左侧：耗时。 */
    private val elapsedLabel = JBLabel().apply {
        font = metaFont
        foreground = UIUtil.getLabelDisabledForeground()
    }

    // ===== 正文 =====

    private val cards = JPanel(CardLayout()).apply {
        add(jsonField, CARD_JSON)
        add(plainField, CARD_PLAIN)
    }

    // ===== 工具栏 =====

    private val formatAction = object : AnAction(
        "Format JSON",
        "Reformat the JSON result with two-space indentation",
        AllIcons.Actions.ReformatCode
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = formatJson()

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = currentCard == CARD_JSON && jsonField.document.textLength > 0
        }
    }

    /** 字段注释开关：工具栏第二个图标，默认关闭。 */
    private val commentAction = object : ToggleAction(
        "Show Field Comments",
        "Show Javadoc comments after JSON fields",
        JsonFieldComments.icon()
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = showComments

        override fun setSelected(e: AnActionEvent, state: Boolean) = setShowComments(state)

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = currentCard == CARD_JSON && jsonField.document.textLength > 0
            e.presentation.icon = JsonFieldComments.icon()
        }
    }

    private val collapseAction = object : AnAction(
        "Collapse All",
        "Fold every JSON object and array",
        AllIcons.Actions.Collapseall
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = setAllRegionsExpanded(false)

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = currentCard == CARD_JSON && foldRegionCount() > 0
        }
    }

    private val expandAction = object : AnAction(
        "Expand All",
        "Unfold every folded JSON object and array",
        AllIcons.Actions.Expandall
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = setAllRegionsExpanded(true)

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = currentCard == CARD_JSON && foldRegionCount() > 0
        }
    }

    private val wrapAction = object : ToggleAction(
        "Wrap Lines",
        "Wrap long result lines; disabled keeps one line and scrolls horizontally",
        AllIcons.Actions.ToggleSoftWrap
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun isSelected(e: AnActionEvent): Boolean = wrapLines

        override fun setSelected(e: AnActionEvent, state: Boolean) = setWrapLines(state)
    }

    private val copyAction = object : AnAction(
        "Copy Result",
        "Copy the result body to the clipboard",
        AllIcons.Actions.Copy
    ) {
        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

        override fun actionPerformed(e: AnActionEvent) = copyResult()

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabled = hasText()
        }
    }

    /** 结果工具栏：与 Logs 一样竖排在正文右侧。 */
    private val toolbar = ActionManager.getInstance()
        .createActionToolbar(
            "LuckyHttpClient.DebugResult",
            DefaultActionGroup().apply {
                add(formatAction)
                add(commentAction)
                add(collapseAction)
                add(expandAction)
                add(wrapAction)
                add(copyAction)
            },
            false
        )
        .apply { setTargetComponent(this@DebugResultView.cards) }

    private val metaBottomRow = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
        isOpaque = false
        add(kindBadge)
        add(elapsedLabel)
    }

    private val metaPanel = JPanel(BorderLayout()).apply {
        isOpaque = true
        background = editorBackground
        border = JBUI.Borders.empty(2, 6, 2, 2)
        add(returnTypeLabel, BorderLayout.NORTH)
        add(metaBottomRow, BorderLayout.CENTER)
        isVisible = false
    }

    /** 正文容器：编辑器居中，工具栏竖排在右侧（与 Logs 一致）。 */
    private val bodyPanel = JPanel(BorderLayout()).apply {
        add(cards, BorderLayout.CENTER)
        add(toolbar.component, BorderLayout.EAST)
    }

    private val rootPanel = JPanel(BorderLayout()).apply {
        add(metaPanel, BorderLayout.NORTH)
        add(bodyPanel, BorderLayout.CENTER)
    }

    /** Result 页使用的组件；提示条由调用方放在它上方。 */
    val component: JPanel get() = rootPanel

    init {
        jsonField.setDisposedWith(disposable)
        plainField.setDisposedWith(disposable)
    }

    /**
     * 成功结果：JSON 走语法高亮 + 折叠，其余文本走普通编辑器。
     *
     * [comments] 为返回类型字段的路径 → 说明，字段注释开关开启时绘制到行尾。
     */
    fun showSuccess(
        returnType: String?,
        kind: String,
        body: String,
        elapsedMs: Long,
        comments: Map<String, String> = emptyMap()
    ) {
        metaPanel.isVisible = true
        returnTypeLabel.foreground = UIUtil.getLabelForeground()
        val type = returnType?.takeIf { it.isNotBlank() } ?: "?"
        returnTypeLabel.text = "Return type: $type"
        returnTypeLabel.toolTipText = "Return type: $type"
        kindBadge.text = kind.uppercase(Locale.ROOT)
        applyBadge(badgePalette(kind))
        updateElapsed(elapsedMs)
        fieldComments = comments
        // 只有真正可解析的 JSON 才装折叠区域，文本结果保持纯净
        showBody(body, json = isJsonKind(kind) && looksLikeJson(body))
        refreshMeta()
    }

    /** 失败结果：第一行是错误标题，正文是消息与堆栈。 */
    fun showFailure(title: String, body: String, elapsedMs: Long) {
        fieldComments = emptyMap()
        metaPanel.isVisible = true
        returnTypeLabel.foreground = errorForeground()
        returnTypeLabel.text = title
        returnTypeLabel.toolTipText = title
        kindBadge.text = ERROR_BADGE_TEXT
        applyBadge(ERROR_PALETTE)
        updateElapsed(elapsedMs)
        showBody(body, json = false)
        refreshMeta()
    }

    /** 普通提示（容器未启动、启动失败等）：不展示元信息行。 */
    fun showMessage(text: String) {
        fieldComments = emptyMap()
        metaPanel.isVisible = false
        showBody(text, json = false)
        refreshMeta()
    }

    fun clear() = showMessage("")

    var wrapLinesEnabled: Boolean
        get() = wrapLines
        set(value) = setWrapLines(value)

    /**
     * 正文替换。
     *
     * JSON 走 [jsonField]，其余走 [plainField]；两者字体与行号行为一致，
     * 卡片切换不影响工具栏状态。
     */
    private fun showBody(text: String, json: Boolean) {
        currentCard = if (json) CARD_JSON else CARD_PLAIN
        if (json) {
            jsonField.setText(text)
            scheduleFoldRegions(jsonField.getEditor(false))
        } else {
            plainField.setText(text)
            // 文本结果没有 JSON 字段，移除残留的行尾注释
            refreshComments()
        }
        (cards.layout as CardLayout).show(cards, currentCard)
        // 新结果从第一行开始阅读；编辑器延迟创建时由 onEditorAdded 负责定位
        val editor = if (json) jsonField.getEditor(false) else plainField.getEditor(false)
        runCatching { editor?.scrollingModel?.scrollVertically(0) }
        toolbar.updateActionsAsync()
    }

    /** 元信息显隐后重新布局，提示条与正文仍然贴紧。 */
    private fun refreshMeta() {
        rootPanel.revalidate()
        rootPanel.repaint()
    }

    private fun updateElapsed(elapsedMs: Long) {
        elapsedLabel.isVisible = elapsedMs > 0
        elapsedLabel.text = if (elapsedMs > 0) "Elapsed: $elapsedMs ms" else ""
    }

    private fun applyBadge(palette: BadgePalette) {
        kindBadge.styleBadge(palette.background, palette.foreground)
    }

    /** 结果类型徽章配色：JSON 蓝、ERROR 红、其余中性灰。 */
    private fun badgePalette(kind: String): BadgePalette = when (kind.lowercase(Locale.ROOT)) {
        "json", "response" -> JSON_PALETTE
        ERROR_BADGE_TEXT.lowercase(Locale.ROOT) -> ERROR_PALETTE
        else -> NEUTRAL_PALETTE
    }

    private fun errorForeground(): Color = runCatching {
        EditorColorsManager.getInstance().globalScheme
            .getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES)?.foregroundColor
    }.getOrNull() ?: JBColor.RED

    /** 只把结构化结果交给 JSON 编辑器；普通文本保持原样。 */
    private fun isJsonKind(kind: String): Boolean {
        val normalized = kind.lowercase(Locale.ROOT)
        return normalized == "json" || normalized == "response"
    }

    /**
     * 结果是否值得装 JSON 折叠。
     *
     * 大文本建 PSI 与折叠区域会卡住 EDT，超过阈值直接按普通文本展示；
     * 解析失败（launcher 回退原文）也按普通文本处理。
     */
    private fun looksLikeJson(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_JSON_CHARS) {
            return false
        }
        if (trimmed[0] != '{' && trimmed[0] != '[') {
            return false
        }
        return runCatching { JsonParser.parseString(trimmed) }.isSuccess
    }

    // ===== 折叠 =====

    private fun installFoldRegions(editor: Editor) {
        if (editor.isDisposed) {
            return
        }
        val document = editor.document
        if (document.textLength == 0) {
            refreshComments(editor)
            return
        }
        val psiManager = PsiDocumentManager.getInstance(project)
        if (!psiManager.isCommitted(document)) {
            // 折叠范围必须和最新文本对齐，先提交 PSI
            psiManager.commitDocument(document)
        }
        val ranges = ApplicationManager.getApplication().runReadAction(
            Computable<List<Triple<Int, Int, String>>> {
                val psiFile = psiManager.getPsiFile(document)
                if (psiFile !is JsonFile) {
                    return@Computable emptyList()
                }
                jsonFoldingBuilder.buildFoldRegions(psiFile.node, document)
                    .mapNotNull { descriptor ->
                        val range = descriptor.range
                        if (range.isEmpty || range.endOffset > document.textLength) {
                            return@mapNotNull null
                        }
                        val placeholder = descriptor.placeholderText
                            ?: jsonFoldingBuilder.getPlaceholderText(descriptor.element)
                            ?: "..."
                        Triple(range.startOffset, range.endOffset, placeholder)
                    }
            }
        )
        val foldingModel = editor.foldingModel
        ApplicationManager.getApplication().runReadAction(
            Runnable {
                foldingModel.runBatchFoldingOperation {
                    for (region in foldingModel.allFoldRegions.toList()) {
                        foldingModel.removeFoldRegion(region)
                    }
                    for ((start, end, placeholder) in ranges) {
                        foldingModel.addFoldRegion(start, end, placeholder)
                    }
                }
            }
        )
        // 折叠区域装好后「折叠/展开全部」才可用
        refreshComments(editor)
        toolbar.updateActionsAsync()
    }

    /** 按开关重建字段行尾注释；编辑器尚未创建时跳过。 */
    private fun refreshComments(editor: Editor? = jsonField.getEditor(false)) {
        if (editor == null) {
            return
        }
        JsonFieldComments.refresh(editor, fieldComments, showComments && currentCard == CARD_JSON)
    }

    /** 切换字段注释：立即重建 inlay，并刷新工具栏图标状态。 */
    private fun setShowComments(value: Boolean) {
        if (showComments == value) {
            return
        }
        showComments = value
        refreshComments()
        toolbar.updateActionsAsync()
    }

    private fun scheduleFoldRegions(editor: Editor?) {
        if (editor == null || !foldingPending.compareAndSet(false, true)) {
            return
        }
        ApplicationManager.getApplication().invokeLater {
            foldingPending.set(false)
            if (!editor.isDisposed) {
                installFoldRegions(editor)
            }
        }
    }

    private fun foldRegionCount(): Int =
        jsonField.getEditor(false)?.foldingModel?.allFoldRegions?.size ?: 0

    private fun setAllRegionsExpanded(expanded: Boolean) {
        val editor = jsonField.getEditor(false) ?: return
        ApplicationManager.getApplication().runReadAction(
            Runnable {
                editor.foldingModel.runBatchFoldingOperation {
                    for (region in editor.foldingModel.allFoldRegions) {
                        region.isExpanded = expanded
                    }
                }
            }
        )
        toolbar.updateActionsAsync()
    }

    // ===== 工具栏动作 =====

    private fun currentText(): String =
        if (currentCard == CARD_JSON) jsonField.text else plainField.text

    /** 只判断文档长度，避免 action 每次更新都复制整段正文。 */
    private fun hasText(): Boolean =
        (if (currentCard == CARD_JSON) jsonField.document else plainField.document).textLength > 0

    /** 美化：按 JSON 词法重排，保留注释；幂等文本不做无谓的文档改写。 */
    private fun formatJson() {
        if (currentCard != CARD_JSON) {
            return
        }
        val text = jsonField.text
        val formatted = DebugJsonFormatter.format(text)
        if (formatted == text) {
            return
        }
        jsonField.setText(formatted)
        scheduleFoldRegions(jsonField.getEditor(false))
    }

    private fun copyResult() {
        val text = currentText()
        if (text.isEmpty()) {
            return
        }
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }

    private fun setWrapLines(value: Boolean) {
        wrapLines = value
        for (field in listOf(jsonField, plainField)) {
            field.getEditor(false)?.settings?.isUseSoftWraps = value
        }
        toolbar.updateActionsAsync()
    }

    override fun dispose() {
        Disposer.dispose(disposable)
    }

    // ===== 测试钩子 =====

    internal fun ensureEditorForTest(): Editor? {
        val editor = jsonField.getEditor(true) ?: return null
        installFoldRegions(editor)
        return editor
    }

    internal fun foldRegionCountForTest(): Int = foldRegionCount()

    internal fun jsonModeForTest(): Boolean = currentCard == CARD_JSON

    internal fun lineNumbersShownForTest(): Boolean =
        jsonField.getEditor(true)?.settings?.isLineNumbersShown ?: false

    internal fun fontFamilyForTest(): String? =
        jsonField.getEditor(true)?.colorsScheme?.editorFontName

    internal fun textForTest(): String = currentText()

    /** 字段注释开关的当前状态；默认关闭。 */
    internal fun commentsEnabledForTest(): Boolean = showComments

    /** 切换字段注释开关，验证行尾注释数量。 */
    internal fun setCommentsForTest(value: Boolean) = setShowComments(value)

    /** JSON 编辑器中的字段注释 inlay 数量。 */
    internal fun commentInlayCountForTest(): Int =
        jsonField.getEditor(true)?.let { JsonFieldComments.count(it) } ?: 0

    /** 正文工具条是否竖排在编辑器右侧。 */
    internal fun toolbarOnRightForTest(): Boolean =
        (bodyPanel.layout as? BorderLayout)?.getLayoutComponent(BorderLayout.EAST) === toolbar.component

    private class EllipsisLabel : JBLabel() {
        init {
            isOpaque = false
        }

        /** 长 Return type 不参与最小宽度计算，避免把面板撑宽。 */
        override fun getMinimumSize(): Dimension = Dimension(0, preferredSize.height)
    }

    /** 结果类型徽章：圆角底色 + 小号粗体文字。 */
    private class KindBadge(font: Font) : JBLabel() {
        private var badgeBackground: Color = NEUTRAL_PALETTE.background

        init {
            isOpaque = false
            this.font = font.deriveFont(Font.BOLD)
            border = JBUI.Borders.empty(0, 6)
        }

        fun styleBadge(background: Color, foreground: Color) {
            badgeBackground = background
            this.foreground = foreground
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g2.color = badgeBackground
                val arc = height
                g2.fillRoundRect(0, 0, width - 1, height - 1, arc, arc)
            } finally {
                g2.dispose()
            }
            super.paintComponent(g)
        }
    }

    private data class BadgePalette(val background: Color, val foreground: Color)

    private companion object {
        private const val CARD_JSON = "json"
        private const val CARD_PLAIN = "plain"
        private const val JSON_FILE_NAME = "lucky-result.json"
        private const val ERROR_BADGE_TEXT = "ERROR"

        /** 超过该字符数的结果不建 PSI/折叠区域，避免 EDT 卡顿。 */
        private const val MAX_JSON_CHARS = 1_000_000

        private val JSON_PALETTE = BadgePalette(
            JBColor(Color(0xE3F0FF), Color(0x1E3A5F)),
            JBColor(Color(0x0B5CAD), Color(0x8AB4F8))
        )
        private val ERROR_PALETTE = BadgePalette(
            JBColor(Color(0xFFE9E6), Color(0x4A2020)),
            JBColor(Color(0xC0392B), Color(0xF28B82))
        )
        private val NEUTRAL_PALETTE = BadgePalette(
            JBColor(Color(0xEDEDED), Color(0x3A3D41)),
            UIUtil.getLabelForeground()
        )
    }
}
