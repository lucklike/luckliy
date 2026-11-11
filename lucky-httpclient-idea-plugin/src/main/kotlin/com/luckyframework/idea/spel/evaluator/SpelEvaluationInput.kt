package com.luckyframework.idea.spel.evaluator

import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelTokenType

/**
 * 求值的变量输入（spec 15.3）。
 *
 * 从表达式中收集 `#name` 形式的变量，并解析用户在对话框中
 * 填写的 `name=value` 行；`#root`/`#this` 是 SpEL 保留变量，不参与。
 */
object SpelEvaluationInput {

    private val RESERVED = setOf("root", "this")

    /** 表达式引用的普通变量名（不含 `#`），按出现顺序去重。 */
    fun referencedVariables(expression: String): List<String> {
        val fragment = SpelFragment(
            textRange = TextRange(0, expression.length),
            expressionRange = TextRange(0, expression.length),
            content = expression,
            closed = true
        )
        return SpelTokenizer().tokenize(expression, fragment)
            .filter { it.type == SpelTokenType.VARIABLE }
            .map { it.text.removePrefix("#") }
            .filter { it.isNotBlank() && it !in RESERVED }
            .distinct()
    }

    /** 解析 `name=value` 文本；支持引号字符串、布尔、整数、小数与 null。`//` 开头为注释。 */
    fun parseValues(text: String): Map<String, Any?> {
        val result = LinkedHashMap<String, Any?>()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("//")) {
                continue
            }
            val index = trimmed.indexOf('=')
            if (index <= 0) {
                continue
            }
            val name = trimmed.substring(0, index).trim().removePrefix("#")
            if (name.isEmpty()) {
                continue
            }
            result[name] = parseValue(trimmed.substring(index + 1).trim())
        }
        return result
    }

    private fun parseValue(raw: String): Any? {
        val text = raw.removeSurrounding("\"").removeSurrounding("'")
        return when {
            text.equals("true", ignoreCase = true) -> true
            text.equals("false", ignoreCase = true) -> false
            text.equals("null", ignoreCase = true) -> null
            text.toLongOrNull() != null -> text.toLong()
            text.toDoubleOrNull() != null -> text.toDouble()
            else -> text
        }
    }
}

/** 求值结果与值的展示格式（spec 15.3）。 */
object SpelEvaluationFormatter {

    fun format(result: EvaluatorResult): String = when (result) {
        is EvaluatorResult.Success -> "结果：${result.value}（${result.type}）"
        is EvaluatorResult.MissingVariable -> "变量 ${result.name} 未提供"
        is EvaluatorResult.Forbidden -> "操作被禁止：${result.reason}"
        is EvaluatorResult.Timeout -> "求值超时（${result.seconds} 秒）"
        is EvaluatorResult.Failure -> "求值失败：${result.message}"
        is EvaluatorResult.Unavailable -> "无法求值：${result.message}"
    }

    /** 字符串加引号，其余用标准 `toString`。 */
    fun value(value: Any?): String = when (value) {
        null -> "null"
        is String -> "\"$value\""
        is CharSequence -> value.toString()
        is Char -> "'$value'"
        is Array<*> -> value.joinToString(prefix = "[", postfix = "]") { element -> value(element) }
        is IntArray -> value.joinToString(prefix = "[", postfix = "]")
        is LongArray -> value.joinToString(prefix = "[", postfix = "]")
        is DoubleArray -> value.joinToString(prefix = "[", postfix = "]")
        is BooleanArray -> value.joinToString(prefix = "[", postfix = "]")
        else -> value.toString()
    }
}
