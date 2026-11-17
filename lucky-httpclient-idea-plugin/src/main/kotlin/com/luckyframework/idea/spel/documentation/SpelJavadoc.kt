package com.luckyframework.idea.spel.documentation

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocCommentOwner

/**
 * 从 PSI 的 Javadoc 提取的完整文档内容。
 *
 * 与只取首段单行摘要的 [docCommentOf] 不同，这里保留段落、`<pre>` 代码块
 * 与 HTML 结构，供悬停文档与补全预览按 IDEA 默认样式展示完整说明；
 * `@param`/`@return`/`@throws` 等块级标签拆到独立字段，由渲染器放入表格区域。
 */
internal data class SpelJavadoc(
    /** 正文 HTML；没有正文时为 null。 */
    val content: String? = null,
    /** `@param` 说明，以参数名为键。 */
    val parameters: Map<String, String> = emptyMap(),
    /** `@return` 说明。 */
    val returnDescription: String? = null,
    /** `@throws`/`@exception` 说明（异常类型 → 说明）。 */
    val exceptions: List<Pair<String, String>> = emptyList(),
    /** `@since` 起始版本。 */
    val since: String? = null,
    /** `@deprecated` 废弃说明。 */
    val deprecated: String? = null,
    /** `@see` 参见项。 */
    val seeAlso: List<String> = emptyList()
) {

    val isEmpty: Boolean
        get() = content == null && parameters.isEmpty() && returnDescription == null &&
            exceptions.isEmpty() && since == null && deprecated == null && seeAlso.isEmpty()
}

/**
 * 解析成员的 Javadoc。
 *
 * 注释缺失或只有标记（空的文档注释）时返回 null，调用方回退到符号层描述。
 * 行内标签（`{@code}`、`{@link}`、`{@literal}` 等）渲染为 HTML，
 * 常见排版标签（`<p>`、`<pre>`、`<li>` 等）原样保留，其余尖括号内容转义，
 * 与 IDEA 内置 JavaDoc 文档的展示行为一致。
 */
internal fun javadocOf(member: PsiDocCommentOwner): SpelJavadoc? {
    val lines = member.docComment?.text?.let(::docLines) ?: return null
    if (lines.isEmpty()) {
        return null
    }
    val body = ArrayList<String>()
    val parameters = LinkedHashMap<String, String>()
    var returnDescription: String? = null
    val exceptions = ArrayList<Pair<String, String>>()
    var since: String? = null
    var deprecated: String? = null
    val seeAlso = ArrayList<String>()

    var index = 0
    // 正文：第一个块级标签之前的所有行
    while (index < lines.size && !lines[index].startsWith("@")) {
        body += lines[index]
        index++
    }
    while (index < lines.size) {
        val line = lines[index]
        if (!line.startsWith("@")) {
            index++
            continue
        }
        val tag = line.substring(1).substringBefore(' ').trim()
        val text = StringBuilder(line.substringAfter(' ', "").trim())
        index++
        // 标签说明可以换行续写，直到下一个块级标签
        while (index < lines.size && !lines[index].startsWith("@")) {
            val next = lines[index]
            if (next.isNotEmpty()) {
                if (text.isNotEmpty()) text.append(' ')
                text.append(next)
            }
            index++
        }
        val content = text.toString().trim()
        when (tag) {
            "param" -> {
                val name = content.substringBefore(' ').trim()
                // 类型参数（@param <T>）在方法形参中没有对应项，不进入参数表
                if (name.isNotEmpty() && !name.startsWith("<")) {
                    parameters[name] = content.substringAfter(' ', "").trim()
                }
            }

            "return" -> if (content.isNotEmpty()) returnDescription = content
            "throws", "exception" -> {
                val type = content.substringBefore(' ').trim()
                if (type.isNotEmpty()) {
                    exceptions += type to content.substringAfter(' ', "").trim()
                }
            }

            "since" -> if (content.isNotEmpty()) since = content
            "deprecated" -> deprecated = content.ifEmpty { "已废弃" }
            "see" -> if (content.isNotEmpty()) seeAlso += content
        }
    }
    return SpelJavadoc(
        content = renderBody(body).takeIf { it.isNotBlank() },
        parameters = parameters,
        returnDescription = returnDescription,
        exceptions = exceptions,
        since = since,
        deprecated = deprecated,
        seeAlso = seeAlso
    ).takeIf { !it.isEmpty }
}

/** 去掉注释包装与行首 `*`，保留空行用于分段。 */
private fun docLines(text: String): List<String> =
    text.removePrefix("/**").removeSuffix("*/")
        .lineSequence()
        .map { it.trim().removePrefix("*").trim() }
        .dropWhile { it.isEmpty() }
        .toList()
        .dropLastWhile { it.isEmpty() }

/** 正文转 HTML：空行分段、`<pre>` 原样保留并转义、单行内联标签转换。 */
private fun renderBody(lines: List<String>): String {
    val html = StringBuilder()
    val paragraph = ArrayList<String>()

    fun flush() {
        if (paragraph.isEmpty()) {
            return
        }
        val text = paragraph.joinToString(" ").trim()
        paragraph.clear()
        if (text.isEmpty()) {
            return
        }
        if (html.isNotEmpty()) {
            html.append("<p>")
        }
        html.append(renderInline(text))
    }

    var index = 0
    while (index < lines.size) {
        val trimmed = lines[index].trim()
        if (trimmed.startsWith("<pre>")) {
            flush()
            if (html.isNotEmpty()) {
                html.append("<p>")
            }
            val code = StringBuilder(trimmed.removePrefix("<pre>").substringBefore("</pre>"))
            var closed = trimmed.contains("</pre>")
            while (!closed && ++index < lines.size) {
                val next = lines[index]
                closed = next.contains("</pre>")
                code.append('\n').append(if (closed) next.substringBefore("</pre>") else next)
            }
            html.append("<pre>")
                .append(StringUtil.escapeXmlEntities(code.toString().trim('\n')))
                .append("</pre>")
            index++
            continue
        }
        if (trimmed.isEmpty()) {
            flush()
            index++
            continue
        }
        paragraph += trimmed
        index++
    }
    flush()
    return html.toString()
}

/**
 * 单行文本转 HTML：先把行内标签与白名单 HTML 标签替换为占位符，
 * 转义其余内容后再还原，避免用户注释里的普通尖括号被当成标签。
 */
private fun renderInline(text: String): String {
    val tokens = ArrayList<String>()
    val withInline = INLINE_TAG.replace(text) { match ->
        tokens += inlineHtml(match)
        placeholder(tokens.lastIndex)
    }
    val withTags = HTML_TAG.replace(withInline) { match ->
        if (match.groupValues[1].lowercase() in ALLOWED_TAGS) {
            tokens += match.value
            placeholder(tokens.lastIndex)
        } else {
            match.value
        }
    }
    return restorePlaceholders(StringUtil.escapeXmlEntities(withTags), tokens)
}

/** 行内标签 → HTML 片段；内容统一转义。 */
private fun inlineHtml(match: MatchResult): String {
    val tag = match.groupValues[1]
    val body = match.groupValues[2].trim()
    return when (tag) {
        "code" -> "<code>" + StringUtil.escapeXmlEntities(body) + "</code>"
        "literal" -> StringUtil.escapeXmlEntities(body)
        "link", "linkplain" -> {
            val reference = body.substringBefore(' ').trim()
            val label = body.substringAfter(' ', "").trim()
                .ifEmpty { reference.substringAfterLast('#').ifEmpty { reference } }
            if (tag == "linkplain") {
                StringUtil.escapeXmlEntities(label)
            } else {
                "<code>" + StringUtil.escapeXmlEntities(label) + "</code>"
            }
        }

        "value" -> "<code>" + StringUtil.escapeXmlEntities(body.substringAfterLast('#')) + "</code>"
        else -> ""
    }
}

private fun placeholder(index: Int): String = "\u0000$index\u0001"

private fun restorePlaceholders(text: String, tokens: List<String>): String =
    PLACEHOLDER.replace(text) { match -> tokens.getOrElse(match.groupValues[1].toInt()) { "" } }

/** Javadoc 行内标签；`{@inheritDoc}`/`{@docRoot}` 在独立文档里没有意义，替换为空。 */
private val INLINE_TAG = Regex("""\{@(code|literal|link|linkplain|value|inheritDoc|docRoot)\s*([^}]*)}""")

private val PLACEHOLDER = Regex("\u0000(\\d+)\u0001")

/** 常见排版标签：原样保留，其余尖括号内容按纯文本转义。 */
private val HTML_TAG = Regex("""</?([a-zA-Z][a-zA-Z0-9]*)(\s[^<>]*?)?/?>""")

private val ALLOWED_TAGS = setOf(
    "p", "br", "b", "strong", "i", "em", "u", "s", "strike", "sub", "sup",
    "code", "pre", "ul", "ol", "li", "dl", "dt", "dd", "blockquote", "hr",
    "table", "thead", "tbody", "tr", "td", "th", "caption", "span", "a",
    "h1", "h2", "h3", "h4", "h5", "h6"
)
