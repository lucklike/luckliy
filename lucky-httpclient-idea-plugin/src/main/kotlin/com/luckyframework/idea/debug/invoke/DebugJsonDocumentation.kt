package com.luckyframework.idea.debug.invoke

import com.intellij.json.psi.JsonProperty
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * JSON 参数编辑器的字段文档索引。
 *
 * 模板保持纯 JSON，不再写入 `//` 注释：字段类型与文档说明改由平台 Quick Doc
 * 在悬停 key 时展示。索引按 JSON 属性路径挂在虚拟文件上，路径规则与
 * [DebugArgumentCodec.sampleJson] 生成的骨架一致（顶层参数名 + 嵌套字段名，
 * 列表元素按「列表名.字段名」索引，数组下标不参与路径）。
 */
object DebugJsonDocumentation {

    private val FIELD_DOCS: Key<Map<String, DebugParameter>> =
        Key.create("lucky.debug.json.field.docs")

    /** 把参数树索引挂到 JSON 虚拟文件上；没有字段结构时不挂，普通 JSON 文件不受影响。 */
    fun attach(file: PsiFile, parameters: List<DebugParameter>) {
        val docs = index(parameters)
        if (docs.isNotEmpty()) {
            file.putUserData(FIELD_DOCS, docs)
        }
    }

    /** 按 JSON 属性路径索引字段文档；路径与模板里的 key 一一对应。 */
    internal fun index(parameters: List<DebugParameter>): Map<String, DebugParameter> {
        val docs = LinkedHashMap<String, DebugParameter>()
        for (parameter in parameters) {
            addParameter(parameter, parameter.name, docs)
        }
        return docs
    }

    private fun addParameter(
        parameter: DebugParameter,
        path: String,
        docs: MutableMap<String, DebugParameter>
    ) {
        docs[path] = parameter
        when (parameter.kind) {
            DebugParameterKind.OBJECT ->
                parameter.fields.forEach { addParameter(it, "$path.${it.name}", docs) }
            // 列表模板给可解析元素生成单元素骨架，元素字段沿用「列表名.字段名」路径
            DebugParameterKind.LIST -> parameter.element
                ?.takeIf { it.kind == DebugParameterKind.OBJECT }
                ?.fields
                ?.forEach { addParameter(it, "$path.${it.name}", docs) }
            else -> Unit
        }
    }

    /** 光标所在 key 的字段文档；值区域与普通 JSON 文件返回 null。 */
    fun parameterAt(file: PsiFile, offset: Int): DebugParameter? {
        val element = file.findElementAt(offset) ?: return null
        return parameterAt(element)
    }

    /**
     * 元素所属属性的字段文档。
     *
     * 只响应 key（含引号）：悬停在值上时平台应展示值本身的信息，
     * 弹出字段说明反而干扰输入。
     */
    fun parameterAt(element: PsiElement): DebugParameter? {
        val property = PsiTreeUtil.getParentOfType(element, JsonProperty::class.java, false) ?: return null
        val nameElement = property.nameElement ?: return null
        if (!nameElement.textRange.containsOffset(element.textRange.startOffset)) {
            return null
        }
        val docs = property.containingFile.getUserData(FIELD_DOCS) ?: return null
        return docs[pathOf(property)]
    }

    /** 属性的 JSON 路径：从最内层 key 逐级拼到顶层，数组元素不贡献路径段。 */
    internal fun pathOf(property: JsonProperty): String {
        val names = ArrayList<String>()
        var current: JsonProperty? = property
        while (current != null) {
            names.add(0, current.name)
            current = PsiTreeUtil.getParentOfType(current, JsonProperty::class.java, true)
        }
        return names.joinToString(".")
    }

    /** Quick Doc 正文：字段名 + 简单类型 + 全限定名 + 文档说明。 */
    fun htmlOf(parameter: DebugParameter): String {
        val name = StringUtil.escapeXmlEntities(parameter.name)
        val type = StringUtil.escapeXmlEntities(parameter.typeName.substringAfterLast('.'))
        val qualified = StringUtil.escapeXmlEntities(parameter.typeName)
        val description = descriptionOf(parameter)
        return buildString {
            append("<html><body>")
            append("<b>").append(name).append("</b> : ").append(type)
            if (parameter.kind == DebugParameterKind.ENUM && parameter.enumConstants.isNotEmpty()) {
                append(" (")
                append(StringUtil.escapeXmlEntities(parameter.enumConstants.joinToString(", ")))
                append(')')
            }
            append("<br/><font color='gray'>").append(qualified).append("</font>")
            if (description != null) {
                append("<p>").append(StringUtil.escapeXmlEntities(description)).append("</p>")
            }
            append("</body></html>")
        }
    }

    /** 单行摘要：悬停预览与文档标题共用，避免两处格式漂移。 */
    fun hintOf(parameter: DebugParameter): String {
        val type = parameter.typeName.substringAfterLast('.')
        val description = descriptionOf(parameter)
        return if (description == null) "$type ${parameter.name}" else "$type ${parameter.name} · $description"
    }

    /** Javadoc 文本折叠成一行；空文档按「没有说明」处理。 */
    private fun descriptionOf(parameter: DebugParameter): String? = parameter.description
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
}
