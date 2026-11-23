package com.luckyframework.idea.debug.invoke

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.util.Base64

/**
 * 表单值与 JSON 文本到协议参数的转换。
 *
 * 输出是「类型标签 + 值」的数组，由 launcher 按方法签名还原。
 * 插件进程不加载用户类型，因此这里只做字面量层面的判断；
 * List/Map/POJO 的嵌套值保持原始 JSON，由 launcher 按目标泛型转换。
 */
object DebugArgumentCodec {

    class CodecException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

    fun encodeForm(values: Map<String, String>, parameters: List<DebugParameter>): JsonArray {
        val result = JsonArray()
        for (parameter in parameters) {
            if (parameter.kind == DebugParameterKind.UNSUPPORTED) {
                throw CodecException("参数 ${parameter.name} 暂不支持：${parameter.unsupportedReason ?: "类型不支持"}")
            }
            val text = values[parameter.name]
            if (text.isNullOrBlank()) {
                if (parameter.required && parameter.kind != DebugParameterKind.STRING) {
                    throw CodecException("参数 ${parameter.name} 不能为空")
                }
                result.add(
                    if (parameter.kind == DebugParameterKind.STRING) stringNode("") else nullNode()
                )
                continue
            }
            result.add(encodeText(text, parameter))
        }
        return result
    }

    fun encodeJson(text: String, parameters: List<DebugParameter>): JsonArray {
        val element = parseJson(text, null)
        if (element.isJsonArray) {
            return encodeByPosition(element.asJsonArray, parameters)
        }
        if (element.isJsonObject) {
            return encodeByName(element.asJsonObject, parameters)
        }
        throw CodecException("JSON 必须是对象或数组")
    }

    /**
     * 表单空值的输入提示。
     *
     * 只描述该类型可接受的字面量形态，不参与编码；UI 放在 label 的
     * tooltip 与输入框提示里，避免用户面对 List/POJO 时不知道要填什么。
     */
    fun exampleOf(parameter: DebugParameter): String = when (parameter.kind) {
        DebugParameterKind.STRING -> "text"
        DebugParameterKind.BOOLEAN -> "true / false"
        DebugParameterKind.INT, DebugParameterKind.LONG -> "123"
        DebugParameterKind.DOUBLE, DebugParameterKind.DECIMAL -> "1.5"
        DebugParameterKind.BIGINT -> "12345678901234567890"
        DebugParameterKind.CHAR -> "a"
        DebugParameterKind.ENUM -> parameter.enumConstants.firstOrNull() ?: "ENUM_CONSTANT"
        DebugParameterKind.FILE -> "C:\\path\\to\\file"
        DebugParameterKind.BYTES -> "Base64 文本或本地文件"
        DebugParameterKind.LIST -> "[1, 2, 3]"
        DebugParameterKind.MAP -> "{\"key\": \"value\"}"
        DebugParameterKind.OBJECT -> "{\"field\": \"value\"}"
        DebugParameterKind.UNSUPPORTED -> parameter.unsupportedReason ?: "类型不支持"
    }

    /**
     * JSON 参数模式的模板：按参数名生成字段结构的纯 JSON 骨架。
     *
     * 模板不再写 `//` 注释：字段类型与 Javadoc 说明由
     * [DebugJsonDocumentation] 在悬停 key 时展示，避免注释挤占编辑区。
     * 用户仍可手写注释，[parseJson] 会先去掉；列表元素可解析时给一条
     * 示例元素，否则给空数组。
     */
    fun sampleJson(parameters: List<DebugParameter>): String = renderObject(parameters, "", false)

    /**
     * 表单里列表/Map/无法展开对象的初始文本。
     *
     * 列表元素是对象时生成带注释的单元素骨架，用户只填值、不用记字段名；
     * 标量列表、Map 与未知对象给空容器，避免误发示例数据。
     */
    fun formTemplateOf(parameter: DebugParameter): String = when (parameter.kind) {
        DebugParameterKind.LIST -> renderList(parameter.element, "", true)
        DebugParameterKind.MAP, DebugParameterKind.OBJECT -> "{}"
        else -> exampleOf(parameter)
    }

    private fun renderObject(
        fields: List<DebugParameter>,
        indent: String,
        withComments: Boolean
    ): String {
        if (fields.isEmpty()) {
            return "{}"
        }
        val childIndent = "$indent  "
        val body = fields.joinToString(",\n") { field ->
            buildString {
                if (withComments) {
                    append(childIndent).append("// ").append(commentOf(field)).append('\n')
                }
                append(childIndent).append(quote(field.name)).append(": ")
                append(renderValue(field, childIndent, withComments))
            }
        }
        return "{\n$body\n$indent}"
    }

    private fun renderValue(
        parameter: DebugParameter,
        indent: String,
        withComments: Boolean
    ): String = when (parameter.kind) {
        DebugParameterKind.STRING,
        DebugParameterKind.FILE,
        DebugParameterKind.BYTES -> "\"\""
        DebugParameterKind.CHAR -> "\"a\""
        DebugParameterKind.BOOLEAN -> "false"
        DebugParameterKind.INT,
        DebugParameterKind.LONG,
        DebugParameterKind.DOUBLE,
        DebugParameterKind.DECIMAL,
        DebugParameterKind.BIGINT -> "0"
        DebugParameterKind.ENUM -> quote(parameter.enumConstants.firstOrNull().orEmpty())
        DebugParameterKind.LIST -> renderList(parameter.element, indent, withComments)
        DebugParameterKind.MAP -> "{}"
        DebugParameterKind.OBJECT ->
            if (parameter.fields.isEmpty()) "{}" else renderObject(parameter.fields, indent, withComments)
        DebugParameterKind.UNSUPPORTED -> "null"
    }

    private fun renderList(element: DebugParameter?, indent: String, withComments: Boolean): String {
        if (element == null) {
            return "[]"
        }
        if (element.kind == DebugParameterKind.OBJECT && element.fields.isNotEmpty()) {
            val childIndent = "$indent  "
            val item = renderObject(element.fields, childIndent, withComments)
            return "[\n$childIndent$item\n$indent]"
        }
        return "[]"
    }

    /** 注释一行：类型 + 文档说明；没有文档时保留类型，骨架依旧可读。 */
    private fun commentOf(parameter: DebugParameter): String {
        val type = parameter.typeName.substringAfterLast('.')
        val description = parameter.description
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        val text = if (description == null) type else "$type · $description"
        return text.take(COMMENT_MAX_LENGTH).replace("*/", "* /")
    }

    private fun quote(value: String): String = SAMPLE_GSON.toJson(value)

    /**
     * 对象字段值 → 普通 JSON 元素。
     *
     * 表单把对象字段拆成了独立输入框，提交前需要重新组装 JSON 对象；
     * 这里只做字面量校验与类型转换，协议包装仍由 [encodeForm] 完成。
     */
    fun fieldElement(parameter: DebugParameter, text: String): JsonElement = when (parameter.kind) {
        DebugParameterKind.STRING -> JsonPrimitive(text)
        DebugParameterKind.BOOLEAN -> booleanNode(text)["value"]
        DebugParameterKind.INT -> intNode(text)["value"]
        // long 在协议里是字符串，组装 JSON 时必须还原成数字
        DebugParameterKind.LONG -> JsonPrimitive(longNode(text)["value"].asLong)
        DebugParameterKind.DOUBLE -> doubleElement(text)
        DebugParameterKind.CHAR -> charNode(text)["value"]
        DebugParameterKind.DECIMAL -> JsonPrimitive(decimalNode(text)["value"].asString.toBigDecimal())
        DebugParameterKind.BIGINT -> JsonPrimitive(bigintNode(text)["value"].asString.toBigInteger())
        DebugParameterKind.ENUM -> enumNode(text, parameter)["value"]
        DebugParameterKind.FILE -> fileNode(text)["value"]
        DebugParameterKind.BYTES -> base64Node(text)["value"]
        DebugParameterKind.LIST -> parseContainer(text, parameter, array = true)
        DebugParameterKind.MAP -> parseContainer(text, parameter, array = false)
        DebugParameterKind.OBJECT -> parseContainer(text, parameter, array = false)
        DebugParameterKind.UNSUPPORTED -> throw CodecException(
            "参数 ${parameter.name} 暂不支持：${parameter.unsupportedReason ?: "类型不支持"}"
        )
    }

    private fun doubleElement(text: String): JsonElement {
        val raw = doubleNode(text)["value"].asString
        if (raw == "NaN" || raw.contains("Infinity")) {
            throw CodecException("JSON 不支持 NaN/Infinity：$text")
        }
        return JsonPrimitive(raw.toDouble())
    }

    private fun parseContainer(text: String, parameter: DebugParameter, array: Boolean): JsonElement {
        val element = parseJson(text, parameter)
        val valid = if (array) element.isJsonArray else element.isJsonObject
        if (!valid) {
            val expected = if (array) "JSON 数组" else "JSON 对象"
            throw CodecException("参数 ${parameter.name} 必须是$expected")
        }
        return element
    }

    private fun encodeByPosition(values: JsonArray, parameters: List<DebugParameter>): JsonArray {
        if (values.size() != parameters.size) {
            throw CodecException("JSON 数组长度与参数数量不一致：期望 ${parameters.size}，实际 ${values.size()}")
        }
        val result = JsonArray()
        parameters.forEachIndexed { index, parameter ->
            result.add(encodeElement(values[index], parameter))
        }
        return result
    }

    private fun encodeByName(values: JsonObject, parameters: List<DebugParameter>): JsonArray {
        val result = JsonArray()
        for (parameter in parameters) {
            val value = values.get(parameter.name)
            if (value == null || value.isJsonNull) {
                if (parameter.required && parameter.kind != DebugParameterKind.STRING) {
                    throw CodecException("缺少参数 ${parameter.name}")
                }
                result.add(
                    if (parameter.kind == DebugParameterKind.STRING) stringNode("") else nullNode()
                )
                continue
            }
            result.add(encodeElement(value, parameter))
        }
        return result
    }

    private fun encodeText(text: String, parameter: DebugParameter): JsonObject =
        when (parameter.kind) {
            DebugParameterKind.STRING -> stringNode(text)
            DebugParameterKind.BOOLEAN -> booleanNode(text)
            DebugParameterKind.INT -> intNode(text)
            DebugParameterKind.LONG -> longNode(text)
            DebugParameterKind.DOUBLE -> doubleNode(text)
            DebugParameterKind.CHAR -> charNode(text)
            DebugParameterKind.DECIMAL -> decimalNode(text)
            DebugParameterKind.BIGINT -> bigintNode(text)
            DebugParameterKind.ENUM -> enumNode(text, parameter)
            DebugParameterKind.FILE -> fileNode(text)
            DebugParameterKind.BYTES -> base64Node(text)
            DebugParameterKind.LIST -> listNode(parseJson(text, parameter))
            DebugParameterKind.MAP -> mapNode(parseJson(text, parameter))
            DebugParameterKind.OBJECT -> objectJsonNode(parseJson(text, parameter))
            DebugParameterKind.UNSUPPORTED -> throw CodecException(
                "参数 ${parameter.name} 暂不支持：${parameter.unsupportedReason ?: "类型不支持"}"
            )
        }

    private fun encodeElement(element: JsonElement, parameter: DebugParameter): JsonObject {
        if (element.isJsonNull) {
            return nullNode()
        }
        return when (parameter.kind) {
            DebugParameterKind.STRING -> stringNode(element.asString)
            DebugParameterKind.BOOLEAN -> booleanNode(element.asString)
            DebugParameterKind.INT -> intNode(element.asString)
            DebugParameterKind.LONG -> longNode(element.asString)
            DebugParameterKind.DOUBLE -> doubleNode(element.asString)
            DebugParameterKind.CHAR -> charNode(element.asString)
            DebugParameterKind.DECIMAL -> decimalNode(element.asString)
            DebugParameterKind.BIGINT -> bigintNode(element.asString)
            DebugParameterKind.ENUM -> enumNode(element.asString, parameter)
            DebugParameterKind.FILE -> fileNode(element.asString)
            DebugParameterKind.BYTES -> base64Node(element.asString)
            DebugParameterKind.LIST -> listNode(element)
            DebugParameterKind.MAP -> mapNode(element)
            DebugParameterKind.OBJECT -> objectJsonNode(element)
            DebugParameterKind.UNSUPPORTED -> throw CodecException(
                "参数 ${parameter.name} 暂不支持：${parameter.unsupportedReason ?: "类型不支持"}"
            )
        }
    }

    private fun parseJson(text: String, parameter: DebugParameter?): JsonElement =
        runCatching { JsonParser.parseString(stripComments(text)) }
            .getOrElse {
                val prefix = parameter?.let { "参数 ${it.name} " } ?: ""
                throw CodecException("${prefix}不是合法 JSON：${it.message}")
            }

    /**
     * 去掉 JSON 文本里的行注释与块注释。
     *
     * 骨架模板带注释，直接交给 Gson 会报错；字符串里的斜杠不处理。
     * 注释只用于编辑阅读，提交给 launcher 前一律剔除。
     */
    fun stripComments(text: String): String {
        val out = StringBuilder(text.length)
        var index = 0
        var inString = false
        var escaped = false
        while (index < text.length) {
            val c = text[index]
            if (inString) {
                out.append(c)
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
                index++
                continue
            }
            when {
                c == '"' -> {
                    inString = true
                    out.append(c)
                    index++
                }
                c == '/' && index + 1 < text.length && text[index + 1] == '/' -> {
                    while (index < text.length && text[index] != '\n') {
                        index++
                    }
                }
                c == '/' && index + 1 < text.length && text[index + 1] == '*' -> {
                    index += 2
                    while (index + 1 < text.length && !(text[index] == '*' && text[index + 1] == '/')) {
                        index++
                    }
                    index = (index + 2).coerceAtMost(text.length)
                }
                else -> {
                    out.append(c)
                    index++
                }
            }
        }
        return out.toString()
    }

    private fun stringNode(value: String) = JsonObject().apply {
        addProperty("type", "string")
        addProperty("value", value)
    }

    private fun booleanNode(value: String): JsonObject {
        val normalized = value.trim().lowercase()
        if (normalized != "true" && normalized != "false") {
            throw CodecException("布尔值只能是 true/false：$value")
        }
        return JsonObject().apply {
            addProperty("type", "boolean")
            addProperty("value", normalized == "true")
        }
    }

    private fun intNode(value: String): JsonObject {
        val parsed = value.trim().toLongOrNull()
            ?: throw CodecException("不是合法整数：$value")
        return JsonObject().apply {
            addProperty("type", "int")
            addProperty("value", parsed)
        }
    }

    private fun longNode(value: String): JsonObject {
        value.trim().toLongOrNull() ?: throw CodecException("不是合法 long：$value")
        return JsonObject().apply {
            addProperty("type", "long")
            addProperty("value", value.trim())
        }
    }

    private fun doubleNode(value: String): JsonObject {
        val trimmed = value.trim()
        val special = trimmed == "NaN" || trimmed == "Infinity" || trimmed == "-Infinity"
        if (!special && trimmed.toDoubleOrNull() == null) {
            throw CodecException("不是合法浮点数：$value")
        }
        return JsonObject().apply {
            addProperty("type", "double")
            addProperty("value", trimmed)
        }
    }

    private fun charNode(value: String): JsonObject {
        if (value.length != 1) {
            throw CodecException("字符参数只接受单个字符：$value")
        }
        return JsonObject().apply {
            addProperty("type", "char")
            addProperty("value", value)
        }
    }

    private fun decimalNode(value: String): JsonObject {
        value.trim().toBigDecimalOrNull() ?: throw CodecException("不是合法小数：$value")
        return JsonObject().apply {
            addProperty("type", "decimal")
            addProperty("value", value.trim())
        }
    }

    private fun bigintNode(value: String): JsonObject {
        value.trim().toBigIntegerOrNull() ?: throw CodecException("不是合法大整数：$value")
        return JsonObject().apply {
            addProperty("type", "bigint")
            addProperty("value", value.trim())
        }
    }

    private fun enumNode(value: String, parameter: DebugParameter): JsonObject {
        val name = value.trim()
        if (parameter.enumConstants.isNotEmpty() && name !in parameter.enumConstants) {
            throw CodecException("枚举值不存在：${parameter.typeName}.$name")
        }
        return JsonObject().apply {
            addProperty("type", "enum")
            addProperty("value", name)
        }
    }

    private fun fileNode(value: String) = JsonObject().apply {
        addProperty("type", "file")
        addProperty("value", value.trim())
    }

    private fun base64Node(value: String): JsonObject {
        val trimmed = value.trim()
        runCatching { Base64.getDecoder().decode(trimmed) }
            .getOrElse { throw CodecException("不是合法 Base64：$value") }
        return JsonObject().apply {
            addProperty("type", "base64")
            addProperty("value", trimmed)
        }
    }

    private fun listNode(element: JsonElement): JsonObject {
        if (!element.isJsonArray) {
            throw CodecException("列表参数必须是 JSON 数组")
        }
        return JsonObject().apply {
            addProperty("type", "list")
            add("value", element)
        }
    }

    private fun mapNode(element: JsonElement): JsonObject {
        if (!element.isJsonObject) {
            throw CodecException("Map 参数必须是 JSON 对象")
        }
        return JsonObject().apply {
            addProperty("type", "map")
            add("value", element)
        }
    }

    private fun objectJsonNode(element: JsonElement): JsonObject = JsonObject().apply {
        addProperty("type", "object")
        addProperty("json", element.toString())
    }

    private fun nullNode() = JsonObject().apply {
        addProperty("type", "null")
    }

    private val SAMPLE_GSON = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create()

    /** 骨架注释的最大长度，避免一行注释撑满编辑器。 */
    private const val COMMENT_MAX_LENGTH = 120
}
