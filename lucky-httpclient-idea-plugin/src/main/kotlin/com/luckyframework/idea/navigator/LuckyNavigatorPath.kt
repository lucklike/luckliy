package com.luckyframework.idea.navigator

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.luckyframework.idea.spel.documentation.SpelDocumentationResolver
import com.luckyframework.idea.spel.environment.SpringEnvironmentModel
import com.luckyframework.idea.spel.environment.SpringEnvironmentService
import com.luckyframework.idea.spel.environment.SpringPlaceholder
import com.luckyframework.idea.spel.environment.SpringPlaceholderParser
import com.luckyframework.idea.spel.highlight.SpelTextAttributes
import com.luckyframework.idea.spel.highlight.SpelTokenAttributes
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.reference.SpelDeclaration

/**
 * path 中一个 `${...}` 的折叠信息。
 *
 * [text] 是环境解析值，[id] 在树中唯一。同一个占位符的每个片段共享
 * 同一个实例，渲染时按 [id] 决定展示配置值还是原表达式。
 */
data class LuckyPathFold(
    val id: String,
    val text: String,
    val attributesKey: TextAttributesKey? = SpelTextAttributes.SPRING_PLACEHOLDER
)

/**
 * path 的一个样式片段。
 *
 * [attributesKey] 为 null 时使用常规文本；[grayed] 用于需要弱化展示的
 * 辅助文字；[documentation] 是悬停 HTML；[navigation] 是单击跳转目标。
 */
data class LuckyPathSegment(
    val text: String,
    val attributesKey: TextAttributesKey? = null,
    val grayed: Boolean = false,
    val documentation: String? = null,
    val navigation: PsiElement? = null,
    val fold: LuckyPathFold? = null
)

/** path 的完整展示：[text] 是纯文本（搜索用），[segments] 是富文本片段。 */
data class LuckyPathPresentation(val text: String, val segments: List<LuckyPathSegment>)

/**
 * 侧边栏 path 富文本的构建器。
 *
 * 运行期 URL 优先于 `func`：`Context#autoExecuteSpELOrFunc` 先取 URL，
 * 结果为空或校验失败才调用 `func`。因此这里同样优先展示 URL，没有 URL
 * 时才展示 `func` 的函数名。
 *
 * 片段来源：
 * - `#{...}`：按 SpEL 词法着色，token 的悬停文档与跳转沿编辑器同一条
 *   [SpelDocumentationResolver] 链路解析；
 * - `${...}`：键名、默认值与定界符分别着色。配置值可解析时生成折叠组，
 *   侧边栏默认显示配置值，点击后展开原表达式；
 * - `func` 的纯函数名：整段按函数着色，并解析到注册的函数声明。
 *
 * 构建过程只读取 PSI 与索引，必须在 read action 中执行。
 */
object LuckyNavigatorPath {

    /** 配置值预览的截断长度，与编辑器折叠保持一致。 */
    private const val PREVIEW_LIMIT = 80

    /** 带 scheme 的完整地址，不参与相对路径的 `/` 补全。 */
    private val ABSOLUTE_URL = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")

    /**
     * 方法行的 path 展示；URL 与 `func` 都没有时返回 null，由调用方回退
     * 到 YAML 配置或 `/`。
     *
     * [withPathPrefix] 为 true 时（接口 path）相对路径统一以 `/` 开头，
     * 地址栏习惯便于与方法名区分；baseUrl 这类完整地址传 false。
     */
    fun presentationOf(
        url: HttpValue?,
        func: HttpValue?,
        withPathPrefix: Boolean = true
    ): LuckyPathPresentation? {
        if (url != null) {
            return presentationOfValue(url, funcName = false, withPathPrefix)
        }
        if (func != null) {
            return presentationOfValue(func, funcName = true, withPathPrefix)
        }
        return null
    }

    /** 没有可定位字面量的纯文本展示，不做路径规范化（baseUrl 等完整地址）。 */
    fun plainOf(text: String): LuckyPathPresentation =
        LuckyPathPresentation(text, listOf(LuckyPathSegment(text)))

    /** YAML 等配置来源的 path：与注解 path 一样补齐 `/` 前缀。 */
    fun plainPathOf(text: String): LuckyPathPresentation =
        plainOf(if (needsLeadingSlash(text)) "/$text" else text)

    /**
     * 拼接 baseUrl 的两部分：注解 `url`（或 YAML baseUrl）与注解 `path`。
     *
     * 分隔规则与运行期一致：两侧都带 `/` 时只保留一个，任一侧带 `/` 时
     * 直接拼接，都没有时补一个 `/`。任一侧缺失时返回另一侧。
     */
    fun concatOf(
        url: LuckyPathPresentation?,
        path: LuckyPathPresentation?
    ): LuckyPathPresentation? {
        if (url == null || url.text.isEmpty()) {
            return path
        }
        if (path == null || path.text.isEmpty()) {
            return url
        }
        val urlEndsWithSlash = url.text.endsWith("/")
        val pathStartsWithSlash = path.text.startsWith("/")
        val segments = when {
            urlEndsWithSlash && pathStartsWithSlash ->
                url.segments + dropLeadingSlash(path.segments)
            urlEndsWithSlash || pathStartsWithSlash ->
                url.segments + path.segments
            else ->
                url.segments + LuckyPathSegment("/") + path.segments
        }
        return LuckyPathPresentation(segments.joinToString("") { it.text }, segments)
    }

    /** 去掉首段开头多余的 `/`；首段恰好是 `/` 时整段移除。 */
    private fun dropLeadingSlash(segments: List<LuckyPathSegment>): List<LuckyPathSegment> {
        val first = segments.firstOrNull() ?: return segments
        if (first.text == "/") {
            return segments.drop(1)
        }
        if (!first.text.startsWith("/")) {
            return segments
        }
        return listOf(first.copy(text = first.text.substring(1))) + segments.drop(1)
    }

    private fun presentationOfValue(
        value: HttpValue,
        funcName: Boolean,
        withPathPrefix: Boolean
    ): LuckyPathPresentation {
        val segments = ArrayList<LuckyPathSegment>()
        if (!funcName && withPathPrefix && needsLeadingSlash(value.text)) {
            segments += LuckyPathSegment("/")
        }
        val literal = value.literal
        segments += if (literal == null) {
            // 编译期常量没有可定位的字面量，按纯文本展示
            listOf(LuckyPathSegment(value.text))
        } else {
            segmentsOfLiteral(literal, funcName)
        }
        return LuckyPathPresentation(segments.joinToString("") { it.text }, segments)
    }

    /** 相对路径补 `/`；`http://` 等完整地址与已有前缀保持原样。 */
    private fun needsLeadingSlash(text: String): Boolean {
        val trimmed = text.trimStart()
        return trimmed.isNotEmpty() &&
            !trimmed.startsWith("/") &&
            !ABSOLUTE_URL.containsMatchIn(trimmed.take(ABS_URL_PROBE))
    }

    /** 只看开头的 scheme，避免长文本上的正则回溯。 */
    private const val ABS_URL_PROBE = 16

    /** 字面量内容（去掉首尾引号）的片段；扫描坐标与源码文本一致。 */
    private fun segmentsOfLiteral(literal: PsiLiteralExpression, funcName: Boolean): List<LuckyPathSegment> {
        val text = literal.text
        val quoted = text.length >= 2 && text.startsWith("\"") && text.endsWith("\"")
        val from = if (quoted) 1 else 0
        val to = if (quoted) text.length - 1 else text.length
        if (from >= to) {
            return emptyList()
        }
        val hasSpel = text.contains("#{")
        val hasPlaceholder = text.contains("\${")
        if (funcName && !hasSpel && !hasPlaceholder) {
            return functionNameSegments(literal, from, to)
        }
        val decorations = ArrayList<Decoration>()
        val project = literal.project
        // 不包含 `${}` 时不读取配置快照，避免为纯路径触发配置文件扫描
        val model = if (hasPlaceholder) environmentModelOf(literal) else null
        // 占位符优先级最高：`#{}` 内部出现 `${}` 时，键名与折叠仍独立生效
        for (placeholder in SpringPlaceholderParser.parse(text, from, to)) {
            placeholderDecorations(project, literal, text, placeholder, model)
                .forEach { addIfFree(decorations, it) }
        }
        appendSpelDecorations(literal, text, decorations)
        return toSegments(text, from, to, decorations)
    }

    /**
     * `func` 的纯函数名：整段作为一个函数片段。
     *
     * 与编辑器一致，`@SpelAttribute(kind = FUNCTION)` 的裸函数名由
     * [SpelDocumentationResolver] 解析到注册的函数声明（含重载选择）。
     */
    private fun functionNameSegments(
        literal: PsiLiteralExpression,
        from: Int,
        to: Int
    ): List<LuckyPathSegment> {
        val text = literal.text
        var start = from
        var end = to
        while (start < end && text[start].isWhitespace()) {
            start++
        }
        while (end > start && text[end - 1].isWhitespace()) {
            end--
        }
        if (start >= end) {
            return emptyList()
        }
        val declaration = declarationAt(literal, start)
        return listOf(
            LuckyPathSegment(
                text = text.substring(start, end),
                attributesKey = SpelTextAttributes.FUNCTION,
                documentation = declaration?.let { tooltipOf(it) },
                navigation = declaration?.target
            )
        )
    }

    /** 环境配置快照；starter 未引入或索引期间返回 null（不折叠、不提示来源）。 */
    private fun environmentModelOf(literal: PsiLiteralExpression): SpringEnvironmentModel? =
        runCatching { SpringEnvironmentService.getInstance(literal.project).modelFor(literal) }.getOrNull()

    private fun placeholderDecorations(
        project: Project,
        literal: PsiLiteralExpression,
        text: String,
        placeholder: SpringPlaceholder,
        model: SpringEnvironmentModel?
    ): List<Decoration> {
        val decorations = ArrayList<Decoration>()
        val fold = foldOf(literal, placeholder, model)
        decorations += Decoration(
            range = TextRange(placeholder.startOffset, (placeholder.startOffset + 2).coerceAtMost(text.length)),
            attributesKey = SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER,
            fold = fold
        )
        if (placeholder.closed) {
            decorations += Decoration(
                range = TextRange((placeholder.endOffset - 1).coerceAtLeast(placeholder.startOffset), placeholder.endOffset),
                attributesKey = SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER,
                fold = fold
            )
        }
        if (placeholder.key.isNotEmpty()) {
            decorations += Decoration(
                range = placeholder.keyRange,
                attributesKey = SpelTextAttributes.SPRING_PLACEHOLDER,
                documentation = propertyTooltip(placeholder.key, model),
                navigation = propertyNavigation(project, placeholder.key, model),
                fold = fold
            )
        }
        val defaultRange = placeholder.defaultRange
        if (defaultRange != null) {
            decorations += Decoration(
                range = TextRange(placeholder.keyEndOffset, (placeholder.keyEndOffset + 1).coerceAtMost(text.length)),
                attributesKey = SpelTextAttributes.SPRING_PLACEHOLDER_DELIMITER,
                fold = fold
            )
            if (!defaultRange.isEmpty) {
                decorations += Decoration(
                    range = defaultRange,
                    attributesKey = SpelTextAttributes.SPRING_PLACEHOLDER_DEFAULT,
                    fold = fold
                )
            }
        }
        return decorations
    }

    /** 可折叠的配置值；键未解析、默认值也没有时不折叠，避免把不完整结果当成确定值。 */
    private fun foldOf(
        literal: PsiLiteralExpression,
        placeholder: SpringPlaceholder,
        model: SpringEnvironmentModel?
    ): LuckyPathFold? {
        if (model == null || !placeholder.closed || placeholder.key.isEmpty()) {
            return null
        }
        val value = runCatching { model.resolvePlaceholder(placeholder) }.getOrNull() ?: return null
        if (value.isEmpty()) {
            return null
        }
        val file = literal.containingFile?.virtualFile?.path ?: literal.containingFile?.name ?: "?"
        val id = "$file:${literal.textRange.startOffset}:${placeholder.startOffset}"
        return LuckyPathFold(id, previewOf(value))
    }

    /** 配置键的悬停文档：最终值与来源文件；未解析时说明当前环境没有该键。 */
    private fun propertyTooltip(key: String, model: SpringEnvironmentModel?): String {
        val resolved = model?.let { runCatching { it.resolve(key) }.getOrNull() }
        val body = if (resolved?.value != null) {
            buildString {
                append("Value: ").append(escape(resolved.value))
                append("<br/>Source: ").append(escape(resolved.label))
            }
        } else {
            "Key is not defined in the active configuration"
        }
        return "<html><body><b>${escape(key)}</b><br/>" +
            "<font color='gray'>Spring property</font><br/>$body</body></html>"
    }

    /** 配置键的跳转目标：排在首位的定义（profile 优先级最高）的键名位置。 */
    private fun propertyNavigation(
        project: Project,
        key: String,
        model: SpringEnvironmentModel?
    ): PsiElement? {
        val definition = model?.let { runCatching { it.definitionsOf(key) }.getOrNull() }
            ?.firstOrNull() ?: return null
        val file = runCatching { PsiManager.getInstance(project).findFile(definition.file) }.getOrNull()
            ?: return null
        return file.findElementAt(definition.keyOffset) ?: file
    }

    /** `#{...}` 的定界符与 token；文档/跳转只对会产生引用的 token 解析。 */
    private fun appendSpelDecorations(
        literal: PsiLiteralExpression,
        text: String,
        decorations: MutableList<Decoration>
    ) {
        val fragments = SpelScanner().scanLiteral(text)
        // 先占定界符，再放 token：内层 `#{` 会被词法拆成 `#` 与 `{`，不能被覆盖
        for (fragment in flatten(fragments)) {
            addIfFree(
                decorations,
                Decoration(
                    range = TextRange(fragment.textRange.startOffset, fragment.textRange.startOffset + 2),
                    attributesKey = SpelTextAttributes.DELIMITER
                )
            )
            if (fragment.closed) {
                addIfFree(
                    decorations,
                    Decoration(
                        range = TextRange(fragment.textRange.endOffset - 1, fragment.textRange.endOffset),
                        attributesKey = SpelTextAttributes.DELIMITER
                    )
                )
            }
        }
        for (fragment in fragments) {
            appendTokenDecorations(literal, text, fragment, decorations)
        }
    }

    /** 片段及其全部嵌套片段，用于先放置所有 `#{` / `}` 定界符。 */
    private fun flatten(fragments: List<SpelFragment>): List<SpelFragment> =
        fragments.flatMap { listOf(it) + flatten(it.children) }

    private fun appendTokenDecorations(
        literal: PsiLiteralExpression,
        text: String,
        fragment: SpelFragment,
        decorations: MutableList<Decoration>
    ) {
        val tokens = SpelTokenizer().tokenize(text, fragment)
        val attributes = SpelTokenAttributes.attributesOf(tokens)
        for ((index, token) in tokens.withIndex()) {
            val key = attributes[index] ?: continue
            val declaration = if (documented(token)) declarationAt(literal, token.range.startOffset) else null
            addIfFree(
                decorations,
                Decoration(
                    range = token.range,
                    attributesKey = key,
                    documentation = declaration?.let { tooltipOf(it) },
                    navigation = declaration?.target
                )
            )
        }
    }

    /** 只有可能对应符号声明的 token 才解析文档，运算符与数字不进入 PSI 引用链。 */
    private fun documented(token: SpelToken): Boolean = token.type in DOCUMENTED_TOKENS

    /**
     * token 起点的 SpEL 声明；与编辑器悬停/Quick Doc 共用解析规则。
     *
     * `url` / `func` 属性上的 `@SpelAttribute` 元数据决定是否参与解析，
     * 未标注或运行期不求值时返回 null（只保留语法着色）。
     */
    private fun declarationAt(literal: PsiLiteralExpression, offsetInLiteral: Int): SpelDeclaration? {
        val file = literal.containingFile ?: return null
        return runCatching {
            SpelDocumentationResolver.declarationAt(
                file,
                literal.textRange.startOffset + offsetInLiteral,
                literal
            )
        }.getOrNull()
    }

    /** 悬停文档：声明标题、类别与注释摘要（与 Quick Doc 同源，紧凑 HTML）。 */
    private fun tooltipOf(declaration: SpelDeclaration): String {
        val presentation = declaration.presentation
        val title = escape(presentation.presentableText ?: declaration.kindLabel)
        val location = presentation.locationString?.takeIf { it.isNotBlank() }
        val description = declaration.documentation?.description
        return buildString {
            append("<html><body><b>").append(title).append("</b>")
            if (location != null) {
                append("<br/><font color='gray'>").append(escape(location)).append("</font>")
            }
            if (!description.isNullOrBlank()) {
                append("<br/>").append(escape(description))
            }
            append("</body></html>")
        }
    }

    /**
     * 片段与已有装饰发生重叠时丢弃：占位符与定界符优先于词法 token。
     *
     * 必须用 [TextRange.intersectsStrict]：`intersects` 把相邻区间（如 `#{` 与
     * 随后的 token）也视为相交，会把紧贴定界符的 token 全部丢掉。
     */
    private fun addIfFree(decorations: MutableList<Decoration>, decoration: Decoration) {
        if (decoration.range.isEmpty) {
            return
        }
        if (decorations.any { it.range.intersectsStrict(decoration.range) }) {
            return
        }
        decorations += decoration
    }

    /** 把装饰套到文本区间上，空隙补齐为普通文本。 */
    private fun toSegments(
        text: String,
        from: Int,
        to: Int,
        decorations: List<Decoration>
    ): List<LuckyPathSegment> {
        val sorted = decorations
            .map { it.copy(range = TextRange(it.range.startOffset.coerceIn(from, to), it.range.endOffset.coerceIn(from, to))) }
            .filter { !it.range.isEmpty }
            .sortedBy { it.range.startOffset }
        val segments = ArrayList<LuckyPathSegment>()
        var cursor = from
        for (decoration in sorted) {
            if (decoration.range.startOffset < cursor) {
                continue
            }
            if (decoration.range.startOffset > cursor) {
                segments += LuckyPathSegment(text.substring(cursor, decoration.range.startOffset))
            }
            segments += LuckyPathSegment(
                text = text.substring(decoration.range.startOffset, decoration.range.endOffset),
                attributesKey = decoration.attributesKey,
                documentation = decoration.documentation,
                navigation = decoration.navigation,
                fold = decoration.fold
            )
            cursor = decoration.range.endOffset
        }
        if (cursor < to) {
            segments += LuckyPathSegment(text.substring(cursor, to))
        }
        return segments
    }

    private fun previewOf(value: String): String {
        val singleLine = value.replace('\n', ' ').replace('\r', ' ').trim()
        return if (singleLine.length <= PREVIEW_LIMIT) {
            singleLine
        } else {
            singleLine.take(PREVIEW_LIMIT) + "…"
        }
    }

    private fun escape(text: String): String = StringUtil.escapeXmlEntities(text)

    private class Decoration(
        val range: TextRange,
        val attributesKey: TextAttributesKey?,
        val documentation: String? = null,
        val navigation: PsiElement? = null,
        val fold: LuckyPathFold? = null
    ) {
        fun copy(range: TextRange): Decoration = Decoration(range, attributesKey, documentation, navigation, fold)
    }

    /** 参与文档与跳转的 token：符号、成员与类型。 */
    private val DOCUMENTED_TOKENS = setOf(
        SpelTokenType.VARIABLE,
        SpelTokenType.ROOT_VARIABLE,
        SpelTokenType.FUNCTION,
        SpelTokenType.NAMESPACE,
        SpelTokenType.IDENTIFIER,
        SpelTokenType.TYPE_REFERENCE,
        SpelTokenType.BEAN
    )
}
