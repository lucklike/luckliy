package com.luckyframework.idea.spel.documentation

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.luckyframework.idea.spel.metadata.SpelLifecycle
import com.luckyframework.idea.spel.symbol.SpelSymbol
import com.luckyframework.idea.spel.symbol.SpelSymbolKind
import com.luckyframework.idea.spel.symbol.SpelSymbolOrigin

/**
 * 方法/函数的参数说明，供补全预览按默认样式展示「参数」行。
 *
 * PSI 不可用时（如内置函数缺少框架源码）[name] 退化为参数类型。
 */
data class SpelParameter(
    val name: String,
    val type: String? = null,
    val description: String? = null
)

/**
 * 悬停文档的内容模型（11.x）。
 *
 * [category] 是符号来源或类别（方法参数、内置变量、函数……）；
 * [scope] 描述声明可见范围；[stage] 只对内置变量展示可用阶段；
 * [parameters] 与 [returnDescription] 来自方法 PSI 与 Javadoc，供补全预览使用。
 */
data class SpelDocumentation(
    val name: String,
    val category: String,
    val type: String? = null,
    val signature: String? = null,
    val owner: String? = null,
    val scope: String? = null,
    val namespace: String? = null,
    val description: String? = null,
    val parameters: List<SpelParameter> = emptyList(),
    val returnDescription: String? = null,
    val stage: SpelLifecycle? = null,
    val isRoot: Boolean = false,
    /** 运行期支持的书写形式（`#name`、裸 `name`、`name(...)`）；无限制时为 null。 */
    val referenceForm: String? = null,
    val unresolved: Boolean = false,
    val unresolvedTitle: String = "未解析的 SpEL 符号",
    val unresolvedHint: String = "请检查变量名或当前上下文是否可见"
) {

    companion object {

        /**
         * 从符号层生成文档，[category] 由调用方按来源命名。
         *
         * [target] 是符号对应的真实声明（方法、字段或类）：
         * 方法用它补齐参数说明与返回值说明，所有声明都用它渲染完整 Javadoc 正文；
         * 符号层描述只是没有源码注释时的兑底。
         */
        fun of(symbol: SpelSymbol, category: String, target: PsiElement? = null): SpelDocumentation = SpelDocumentation(
            name = symbol.name,
            category = category,
            type = symbol.type,
            signature = symbol.signature,
            owner = symbol.owner,
            scope = scopeOf(symbol),
            namespace = symbol.namespace?.takeUnless { it.startsWith("__") },
            description = symbol.description?.takeIf { it.isNotBlank() }
                ?: (target as? PsiDocCommentOwner)?.let { docCommentOf(it) },
            stage = symbol.lifecycle.takeIf {
                symbol.origin == SpelSymbolOrigin.BUILTIN && symbol.kind != SpelSymbolKind.FUNCTION
            },
            isRoot = symbol.isRoot,
            referenceForm = referenceFormOf(symbol)
        ).withMethodDetails(target as? PsiMethod)

        /** 未解析符号：名称与可能原因，保证悬停不出现空白文档。 */
        fun unresolved(name: String): SpelDocumentation = SpelDocumentation(
            name = name,
            category = "未解析",
            scope = "当前表达式",
            unresolved = true
        )

        /** 未解析的 `T(类型)`：提示按类型引用场景调整，不再建议检查变量名。 */
        fun unresolvedType(typeName: String): SpelDocumentation = SpelDocumentation(
            name = typeName,
            category = "未解析类型",
            scope = "当前表达式",
            unresolved = true,
            unresolvedTitle = "未解析的 SpEL 类型",
            unresolvedHint = "请检查类型名是否正确，或确认该类已在项目依赖中"
        )

        /**
         * `@Env` 的环境配置键未在当前配置中找到。
         *
         * 环境配置键不是 SpEL 符号，不能套用「未解析的 SpEL 符号」；
         * 带默认值时键缺失属于正常用法，运行期会回退默认值。
         */
        fun environmentKeyNotInConfig(key: String, defaultText: String?): SpelDocumentation =
            SpelDocumentation(
                name = key,
                category = "环境配置键",
                scope = "@Env 参数注入",
                description = when {
                    defaultText == null -> "当前配置中没有找到该环境配置键，且未提供默认值"
                    defaultText.isEmpty() -> "当前配置中没有找到该环境配置键，运行期使用空字符串默认值"
                    else -> "当前配置中没有找到该环境配置键，运行期使用默认值：$defaultText"
                }
            )

        private fun scopeOf(symbol: SpelSymbol): String = when (symbol.origin) {
            SpelSymbolOrigin.PARAMETER -> "当前方法"
            SpelSymbolOrigin.METHOD_IMPORT -> "当前方法（方法级 @SpELImport）"
            SpelSymbolOrigin.CLASS_IMPORT -> "当前类及其子类（类级 @SpELImport）"
            SpelSymbolOrigin.CLASS_STATIC -> "当前类及其父类、父接口的静态方法"
            SpelSymbolOrigin.BUILTIN -> "所有 SpEL 表达式"
            SpelSymbolOrigin.GLOBAL -> "项目全局"
            SpelSymbolOrigin.UNKNOWN -> "当前表达式"
        }

        /**
         * 符号运行期支持的书写形式。
         *
         * 普通变量只能 `#name`，Root 变量（含方法参数）只能裸 `name`，
         * 内置函数只能 `name(...)`；导入/类静态函数两种写法都可用，不展示限制。
         */
        private fun referenceFormOf(symbol: SpelSymbol): String? = when (symbol.kind) {
            SpelSymbolKind.VARIABLE -> "#${symbol.name}"
            SpelSymbolKind.ROOT_VARIABLE -> "${symbol.name}（裸名称）"
            SpelSymbolKind.FUNCTION ->
                if (symbol.origin == SpelSymbolOrigin.BUILTIN) "${symbol.name}(...)" else null

            else -> null
        }
    }
}

/**
 * 把 [SpelDocumentation] 渲染为平台文档渲染器识别的 HTML。
 */
object SpelDocumentationRenderer {

    /**
     * 渲染悬停文档。
     *
     * [target] 是符号对应的真实声明：有 Javadoc 时正文展示完整注释（段落、
     * 代码块、行内标签），块级标签（`@since`/`@throws`/`@see`）进入表格区；
     * 没有注释时回退到符号层描述。
     */
    fun render(documentation: SpelDocumentation, target: PsiElement? = null): String = buildString {
        val javadoc = (target as? PsiDocCommentOwner)?.let { javadocOf(it) }
        append(DocumentationMarkup.DEFINITION_START)
        if (documentation.unresolved) {
            append(escape(documentation.name))
        } else {
            append("<b>").append(escape(documentation.name)).append("</b>")
            documentation.type?.takeIf { it.isNotBlank() }?.let {
                append(" : ").append(escape(it))
            }
        }
        append(DocumentationMarkup.DEFINITION_END)

        append(DocumentationMarkup.CONTENT_START)
        javadoc?.deprecated?.takeIf { it.isNotBlank() }?.let {
            append("<b>已废弃：</b>").append(escape(it)).append("<p>")
        }
        append(
            when {
                documentation.unresolved -> escape(documentation.unresolvedTitle)
                javadoc?.content != null -> javadoc.content
                else -> escape(documentation.description ?: documentation.category)
            }
        )
        append(DocumentationMarkup.CONTENT_END)

        val rows = rowsOf(documentation, javadoc)
        if (rows.isNotEmpty()) {
            append(DocumentationMarkup.SECTIONS_START)
            for ((label, value) in rows) {
                append("<tr><td valign='top'>").append(escape(label))
                append(DocumentationMarkup.SECTION_SEPARATOR)
                append(value).append("</td></tr>")
            }
            append(DocumentationMarkup.SECTIONS_END)
        }
    }

    /**
     * 补全列表选中项的文档预览：按 IDEA 默认方法提示框渲染
     * 所属类、签名、描述、参数与返回值，不重复展示来源、作用域等元信息。
     *
     * [target] 有 Javadoc 时正文展示完整注释；变量等没有方法签名的声明
     * 同样能把字段/类注释作为正文展示。
     */
    fun renderCompletion(documentation: SpelDocumentation, target: PsiElement? = null): String = buildString {
        val javadoc = (target as? PsiDocCommentOwner)?.let { javadocOf(it) }
        append(DocumentationMarkup.DEFINITION_START)
        documentation.owner?.takeIf { it.isNotBlank() }?.let {
            append("<small>").append(escape(it)).append("</small><br/>")
        }
        documentation.type?.takeIf { it.isNotBlank() }?.let {
            append(escape(it)).append(' ')
        }
        append("<b>").append(escape(documentation.signature ?: documentation.name)).append("</b>")
        append(DocumentationMarkup.DEFINITION_END)

        val content = when {
            documentation.unresolved -> null
            javadoc?.content != null -> javadoc.content
            else -> documentation.description?.takeIf { it.isNotBlank() }?.let { escape(it) }
        }
        if (content != null || javadoc?.deprecated != null) {
            append(DocumentationMarkup.CONTENT_START)
            javadoc?.deprecated?.takeIf { it.isNotBlank() }?.let {
                append("<b>已废弃：</b>").append(escape(it)).append("<p>")
            }
            content?.let { append(it) }
            append(DocumentationMarkup.CONTENT_END)
        }

        val rows = ArrayList<Pair<String, String>>()
        parameterList(documentation).takeIf { it.isNotEmpty() }?.let { parameters ->
            rows += "参数" to parameters.joinToString("<br/>") { parameter ->
                val name = "<b>" + escape(parameter.name) + "</b>"
                val detail = parameter.description?.takeIf { it.isNotBlank() }
                    ?: parameter.type?.takeIf { it.isNotBlank() }
                if (detail.isNullOrBlank()) name else "$name — " + escape(detail)
            }
        }
        (documentation.returnDescription?.takeIf { it.isNotBlank() }
            ?: javadoc?.returnDescription?.takeIf { it.isNotBlank() }
            ?: documentation.type?.takeIf { it.isNotBlank() })?.let {
            rows += "返回" to escape(it)
        }
        appendJavadocRows(rows, javadoc)
        if (rows.isNotEmpty()) {
            append(DocumentationMarkup.SECTIONS_START)
            for ((label, value) in rows) {
                append("<tr><td valign='top'>").append(escape(label))
                append(DocumentationMarkup.SECTION_SEPARATOR)
                append(value).append("</td></tr>")
            }
            append(DocumentationMarkup.SECTIONS_END)
        }
    }

    /** 参数来自 PSI 时用名称 + 描述，缺失时按签名中的参数类型降级。 */
    private fun parameterList(documentation: SpelDocumentation): List<SpelParameter> {
        if (documentation.parameters.isNotEmpty()) {
            return documentation.parameters
        }
        val body = documentation.signature?.substringAfter('(', "")?.substringBeforeLast(')')
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        return splitParameters(body).map { SpelParameter(it) }
    }

    /** 按顶层逗号切分参数，忽略泛型内部的逗号。 */
    private fun splitParameters(body: String): List<String> {
        val result = ArrayList<String>()
        var depth = 0
        val current = StringBuilder()
        for (character in body) {
            when (character) {
                '<' -> depth++
                '>' -> if (depth > 0) depth--
                ',' -> if (depth == 0) {
                    result += current.toString().trim()
                    current.setLength(0)
                    continue
                }
            }
            current.append(character)
        }
        if (current.isNotBlank()) {
            result += current.toString().trim()
        }
        return result.filter { it.isNotEmpty() }
    }

    /** 生命周期阶段文案；不确定的阶段不显示。 */
    fun stageLabel(stage: SpelLifecycle): String? = when (stage) {
        SpelLifecycle.CLASS -> "类级可用"
        SpelLifecycle.METHOD_META -> "方法元数据阶段可用"
        SpelLifecycle.METHOD -> "方法级可用"
        SpelLifecycle.REQUEST_INIT -> "请求初始化阶段可用"
        SpelLifecycle.CONFIG_API_INIT -> "配置API初始化阶段可用"
        SpelLifecycle.CONFIG_API_METHOD_INIT -> "配置API方法初始化阶段可用"
        SpelLifecycle.CONFIG_API_INIT_COMPLETE -> "配置API初始化完成阶段可用"
        SpelLifecycle.REQUEST -> "请求阶段可用"
        SpelLifecycle.INVOKE_WRAPPER_METHOD -> "调用包装方法阶段可用"
        SpelLifecycle.RETRY -> "重试阶段可用"
        SpelLifecycle.RESPONSE_INIT -> "响应初始化阶段可用"
        SpelLifecycle.RESPONSE -> "响应阶段可用"
        SpelLifecycle.METHOD_RESULT -> "方法返回阶段可用"
        SpelLifecycle.THROWABLE -> "异常阶段可用"
        SpelLifecycle.DESTROY -> "销毁阶段可用"
        SpelLifecycle.NON -> null
        SpelLifecycle.UNKNOWN -> null
    }

    private fun rowsOf(documentation: SpelDocumentation, javadoc: SpelJavadoc? = null): List<Pair<String, String>> {
        if (documentation.unresolved) {
            return listOf("建议" to escape(documentation.unresolvedHint))
        }
        val rows = ArrayList<Pair<String, String>>()
        rows += "来源" to escape(documentation.category)
        documentation.signature?.takeIf { it.isNotBlank() }?.let { rows += "签名" to escape(it) }
        documentation.type?.takeIf { it.isNotBlank() }?.let {
            val label = if (documentation.signature.isNullOrBlank()) "类型" else "返回类型"
            rows += label to escape(it)
        }
        documentation.owner?.takeIf { it.isNotBlank() }?.let { rows += "所属类" to escape(it) }
        documentation.namespace?.takeIf { it.isNotBlank() }?.let { rows += "命名空间" to escape(it) }
        documentation.scope?.takeIf { it.isNotBlank() }?.let { rows += "作用域" to escape(it) }
        documentation.referenceForm?.takeIf { it.isNotBlank() }?.let {
            rows += "引用形式" to escape(it)
        }
        if (documentation.isRoot) {
            rows += "Root 变量" to "是"
        }
        documentation.stage?.let { stage -> stageLabel(stage)?.let { rows += "可用阶段" to escape(it) } }
        appendJavadocRows(rows, javadoc)
        return rows
    }

    /** Javadoc 块级标签行：`@since`、`@throws`、`@see`。 */
    private fun appendJavadocRows(rows: MutableList<Pair<String, String>>, javadoc: SpelJavadoc?) {
        if (javadoc == null) {
            return
        }
        javadoc.since?.takeIf { it.isNotBlank() }?.let { rows += "起始版本" to escape(it) }
        if (javadoc.exceptions.isNotEmpty()) {
            rows += "异常" to javadoc.exceptions.joinToString("<br/>") { (type, text) ->
                val name = "<b>" + escape(type) + "</b>"
                if (text.isBlank()) name else "$name — " + escape(text)
            }
        }
        if (javadoc.seeAlso.isNotEmpty()) {
            rows += "另见" to javadoc.seeAlso.joinToString("<br/>") { escape(it) }
        }
    }

    private fun escape(text: String): String = StringUtil.escapeXmlEntities(text)
}

/**
 * 成员 Javadoc 的首个段落，去除注释标记并合并为单行。
 *
 * 行内标签转为纯文本（`{@code x}` → `x`，`{@link X#m()}` → `m`），
 * 末尾句号等断句符不进入描述。补全列表与跳转声明共用，避免两处提取逻辑漂移。
 */
internal fun docCommentOf(member: PsiDocCommentOwner): String? {
    val comment = member.docComment?.text ?: return null
    val text = comment.removePrefix("/**").removeSuffix("*/")
        .lineSequence()
        .map { it.trim().removePrefix("*").trim() }
        .filter { it.isNotEmpty() && !it.startsWith("@") }
        .joinToString(" ")
        .replace(INLINE_TAG) { inlineTagText(it) }
        .replace(WHITESPACE, " ")
        .trim()
        .trimEnd { it in SENTENCE_ENDINGS }
        .trim()
    return text.takeIf { it.isNotEmpty() }
}

/** Javadoc 行内标签：`{@code x}`、`{@link X#m()}` 等。 */
private val INLINE_TAG = Regex("""\{@(code|literal|link|linkplain)\s+([^}]*)}""")

private const val SENTENCE_ENDINGS = "。．.；;！!？?"

private val WHITESPACE = Regex("""\s+""")

/** 行内标签转纯文本；链接只保留成员名，便于单行展示。 */
private fun inlineTagText(match: MatchResult): String {
    val tag = match.groupValues[1]
    val body = match.groupValues[2].trim()
    return if (tag == "link" || tag == "linkplain") {
        body.substringAfterLast('#').removeSuffix("()")
    } else {
        body
    }
}

/**
 * 单行摘要：合并换行后截断到 [limit] 个字符，超出部分以 `...` 结尾。
 */
internal fun summaryOf(text: String?, limit: Int): String? {
    val singleLine = text?.lineSequence()
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.joinToString(" ")
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    return if (singleLine.length > limit) singleLine.take(limit) + "..." else singleLine
}

/**
 * 补齐方法的参数与返回值说明：`@param` 按参数名匹配，`@return` 作为返回描述。
 *
 * 参数名与类型来自 PSI，描述来自同一份 [javadocOf] 解析结果；
 * 供补全预览与悬停文档共用，避免两套 Javadoc 提取逻辑漂移。
 */
internal fun SpelDocumentation.withMethodDetails(method: PsiMethod?): SpelDocumentation {
    if (method == null) {
        return this
    }
    val javadoc = javadocOf(method)
    return copy(
        parameters = method.parameterList.parameters.map { parameter ->
            SpelParameter(
                name = parameter.name,
                type = parameter.type.presentableText,
                description = javadoc?.parameters?.get(parameter.name)?.takeIf { it.isNotBlank() }
            )
        },
        returnDescription = javadoc?.returnDescription?.takeIf { it.isNotBlank() }
    )
}

