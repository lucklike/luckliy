package com.luckyframework.idea.spel.documentation

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDocCommentOwner
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
    val unresolved: Boolean = false,
    val unresolvedTitle: String = "未解析的 SpEL 符号",
    val unresolvedHint: String = "请检查变量名或当前上下文是否可见"
) {

    companion object {

        /**
         * 从符号层生成文档，[category] 由调用方按来源命名。
         *
         * [method] 是符号对应的真实方法（内置函数按 owner 解析），
         * 用于补齐参数说明与返回值说明。
         */
        fun of(symbol: SpelSymbol, category: String, method: PsiMethod? = null): SpelDocumentation = SpelDocumentation(
            name = symbol.name,
            category = category,
            type = symbol.type,
            signature = symbol.signature,
            owner = symbol.owner,
            scope = scopeOf(symbol),
            namespace = symbol.namespace?.takeUnless { it.startsWith("__") },
            description = symbol.description,
            stage = symbol.lifecycle.takeIf {
                symbol.origin == SpelSymbolOrigin.BUILTIN && symbol.kind != SpelSymbolKind.FUNCTION
            },
            isRoot = symbol.isRoot
        ).withMethodDetails(method)

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

        private fun scopeOf(symbol: SpelSymbol): String = when (symbol.origin) {
            SpelSymbolOrigin.PARAMETER -> "当前方法"
            SpelSymbolOrigin.METHOD_IMPORT -> "当前方法（方法级 @SpELImport）"
            SpelSymbolOrigin.CLASS_IMPORT -> "当前类及其子类（类级 @SpELImport）"
            SpelSymbolOrigin.CLASS_STATIC -> "当前类及其父类、父接口的静态方法"
            SpelSymbolOrigin.BUILTIN -> "所有 SpEL 表达式"
            SpelSymbolOrigin.GLOBAL -> "项目全局"
            SpelSymbolOrigin.UNKNOWN -> "当前表达式"
        }
    }
}

/**
 * 把 [SpelDocumentation] 渲染为平台文档渲染器识别的 HTML。
 */
object SpelDocumentationRenderer {

    fun render(documentation: SpelDocumentation): String = buildString {
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
        append(
            escape(
                when {
                    documentation.unresolved -> documentation.unresolvedTitle
                    else -> documentation.description ?: documentation.category
                }
            )
        )
        append(DocumentationMarkup.CONTENT_END)

        val rows = rowsOf(documentation)
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
     */
    fun renderCompletion(documentation: SpelDocumentation): String = buildString {
        append(DocumentationMarkup.DEFINITION_START)
        documentation.owner?.takeIf { it.isNotBlank() }?.let {
            append("<small>").append(escape(it)).append("</small><br/>")
        }
        documentation.type?.takeIf { it.isNotBlank() }?.let {
            append(escape(it)).append(' ')
        }
        append("<b>").append(escape(documentation.signature ?: documentation.name)).append("</b>")
        append(DocumentationMarkup.DEFINITION_END)

        documentation.description?.takeIf { it.isNotBlank() }?.let {
            append(DocumentationMarkup.CONTENT_START)
            append(escape(it))
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
            ?: documentation.type?.takeIf { it.isNotBlank() })?.let {
            rows += "返回" to escape(it)
        }
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
        SpelLifecycle.REQUEST_INIT -> "请求阶段可用"
        SpelLifecycle.RESPONSE_INIT, SpelLifecycle.RESPONSE -> "响应阶段可用"
        SpelLifecycle.RETRY -> "重试阶段可用"
        SpelLifecycle.THROWABLE -> "异常阶段可用"
        SpelLifecycle.METHOD_RESULT -> "方法返回阶段可用"
        SpelLifecycle.METHOD -> "方法级可用"
        SpelLifecycle.UNKNOWN -> null
    }

    private fun rowsOf(documentation: SpelDocumentation): List<Pair<String, String>> {
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
        if (documentation.isRoot) {
            rows += "Root 变量" to "是"
        }
        documentation.stage?.let { stage -> stageLabel(stage)?.let { rows += "可用阶段" to escape(it) } }
        return rows
    }

    private fun escape(text: String): String = StringUtil.escapeXmlEntities(text)
}

/**
 * 成员 Javadoc 的首个段落，去除注释标记并合并为单行。
 *
 * 补全列表与跳转声明共用，避免两处提取逻辑漂移。
 */
internal fun docCommentOf(member: PsiDocCommentOwner): String? {
    val comment = member.docComment?.text ?: return null
    val text = comment.removePrefix("/**").removeSuffix("*/")
        .lineSequence()
        .map { it.trim().removePrefix("*").trim() }
        .filter { it.isNotEmpty() && !it.startsWith("@") }
        .joinToString(" ")
        .trim()
    return text.takeIf { it.isNotEmpty() }
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
 * 参数名与类型来自 PSI，描述来自 Javadoc；供补全预览与悬停文档共用。
 */
internal fun SpelDocumentation.withMethodDetails(method: PsiMethod?): SpelDocumentation {
    if (method == null) {
        return this
    }
    val tags = javadocTags(method)
    return copy(
        parameters = method.parameterList.parameters.map { parameter ->
            SpelParameter(
                name = parameter.name,
                type = parameter.type.presentableText,
                description = tags[parameter.name]?.takeIf { it.isNotBlank() }
            )
        },
        returnDescription = tags[RETURN_TAG]?.takeIf { it.isNotBlank() }
    )
}

/** `@return` 在标签表里的键，与参数名不会冲突。 */
private const val RETURN_TAG = "\u0000return"

/**
 * Javadoc 标签文本：`@param name` 以参数名为键，`@return` 以 [RETURN_TAG] 为键。
 * 标签后换行的描述会合并为一行。
 */
private fun javadocTags(member: PsiDocCommentOwner): Map<String, String> {
    val lines = member.docComment?.text
        ?.removePrefix("/**")
        ?.removeSuffix("*/")
        ?.lineSequence()
        ?.map { it.trim().removePrefix("*").trim() }
        ?.filter { it.isNotEmpty() }
        ?.toList()
        ?: return emptyMap()
    val result = LinkedHashMap<String, String>()
    var key: String? = null
    val text = StringBuilder()
    fun flush() {
        val current = key ?: return
        result[current] = text.toString().trim()
        key = null
        text.setLength(0)
    }
    for (line in lines) {
        if (!line.startsWith("@")) {
            if (key != null) {
                if (text.isNotEmpty()) text.append(' ')
                text.append(line)
            }
            continue
        }
        flush()
        val tag = line.substring(1).substringBefore(' ').trim()
        val body = line.substringAfter(' ', "").trim()
        when (tag) {
            "param" -> {
                key = body.substringBefore(' ').trim().takeIf { it.isNotEmpty() }
                text.append(body.substringAfter(' ', "").trim())
            }

            "return" -> {
                key = RETURN_TAG
                text.append(body)
            }

            else -> key = null
        }
    }
    flush()
    return result
}
