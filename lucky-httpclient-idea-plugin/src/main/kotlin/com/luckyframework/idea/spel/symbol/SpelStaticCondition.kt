package com.luckyframework.idea.spel.symbol

/**
 * `@If#condition` 的静态求值。
 *
 * 运行期 `Context` 对每个 `@If` 调用 `parseExpression(condition, boolean.class)`，
 * 结果为 `false` 时不导入 `classes`。插件只有在能脱离运行期上下文确定结果时
 * 才跳过这些类，目前支持：
 * - 字面量：`true`/`false`、整数/小数、单双引号字符串、`null`；
 * - 比较运算：`==`/`eq`、`!=`/`ne`、`<`/`lt`、`<=`/`le`、`>`/`gt`、`>=`/`ge`；
 * - 逻辑运算：`!`/`not`、`&&`/`and`、`||`/`or`；
 * - 括号与一元负号。
 *
 * 引用变量、配置占位符、函数调用、算术运算、多片段模板等一律返回 `null`，
 * 调用方保持「按已导入处理」的保守策略，避免误报未解析。
 */
internal object SpelStaticCondition {

    fun evaluate(condition: String): Boolean? {
        val content = contentOf(condition) ?: return null
        val tokens = tokenize(content) ?: return null
        if (tokens.isEmpty()) {
            return null
        }
        val parser = Parser(tokens)
        val value = parser.parseOr() ?: return null
        if (!parser.atEnd) {
            return null
        }
        return (value as? Value.Bool)?.value
    }

    /** 表达式正文；多片段模板与含占位符的文本无法静态确定。 */
    private fun contentOf(condition: String): String? {
        val text = condition.trim()
        if (text.contains("\${")) {
            return null
        }
        if (!text.startsWith("#{")) {
            return if (text.contains("#{")) null else text
        }
        if (!text.endsWith("}")) {
            return null
        }
        val content = text.substring(2, text.length - 1)
        return if (content.contains("#{")) null else content
    }

    private sealed interface Value {
        data class Bool(val value: Boolean) : Value
        data class Num(val value: Double) : Value
        data class Str(val value: String) : Value
        data object Null : Value
    }

    private sealed interface Token {
        data class Word(val text: String) : Token
        data class Num(val value: Double) : Token
        data class Str(val value: String) : Token
        data class Sym(val text: String) : Token
    }

    private fun tokenize(text: String): List<Token>? {
        val tokens = ArrayList<Token>()
        var index = 0
        while (index < text.length) {
            val c = text[index]
            when {
                c.isWhitespace() -> index++

                c == '(' || c == ')' -> {
                    tokens += Token.Sym(c.toString())
                    index++
                }

                c == '!' -> {
                    if (text.getOrNull(index + 1) == '=') {
                        tokens += Token.Sym("!=")
                        index += 2
                    } else {
                        tokens += Token.Sym("!")
                        index++
                    }
                }

                c == '=' -> {
                    if (text.getOrNull(index + 1) != '=') {
                        return null
                    }
                    tokens += Token.Sym("==")
                    index += 2
                }

                c == '<' || c == '>' -> {
                    if (text.getOrNull(index + 1) == '=') {
                        tokens += Token.Sym("$c=")
                        index += 2
                    } else {
                        tokens += Token.Sym(c.toString())
                        index++
                    }
                }

                c == '&' -> {
                    if (text.getOrNull(index + 1) != '&') {
                        return null
                    }
                    tokens += Token.Sym("&&")
                    index += 2
                }

                c == '|' -> {
                    if (text.getOrNull(index + 1) != '|') {
                        return null
                    }
                    tokens += Token.Sym("||")
                    index += 2
                }

                c == '-' -> {
                    tokens += Token.Sym("-")
                    index++
                }

                c == '\'' || c == '"' -> {
                    val quote = c
                    var cursor = index + 1
                    val builder = StringBuilder()
                    var closed = false
                    while (cursor < text.length) {
                        val ch = text[cursor]
                        when {
                            // SpEL 用两个连续引号表示引号本身
                            ch == quote && text.getOrNull(cursor + 1) == quote -> {
                                builder.append(quote)
                                cursor += 2
                            }

                            ch == quote -> {
                                cursor++
                                closed = true
                                break
                            }

                            ch == '\\' && cursor + 1 < text.length -> {
                                builder.append(text[cursor + 1])
                                cursor += 2
                            }

                            else -> {
                                builder.append(ch)
                                cursor++
                            }
                        }
                    }
                    if (!closed) {
                        return null
                    }
                    tokens += Token.Str(builder.toString())
                    index = cursor
                }

                c.isDigit() -> {
                    val start = index
                    while (index < text.length && text[index].isDigit()) {
                        index++
                    }
                    if (text.getOrNull(index) == '.' && text.getOrNull(index + 1)?.isDigit() == true) {
                        index++
                        while (index < text.length && text[index].isDigit()) {
                            index++
                        }
                    }
                    val number = text.substring(start, index).toDoubleOrNull() ?: return null
                    tokens += Token.Num(number)
                }

                c.isLetter() || c == '_' || c == '$' -> {
                    val start = index
                    while (index < text.length && isWordPart(text[index])) {
                        index++
                    }
                    val word = text.substring(start, index)
                    if (word !in KEYWORDS) {
                        return null
                    }
                    tokens += Token.Word(word)
                }

                else -> return null
            }
        }
        return tokens
    }

    private fun isWordPart(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '$'

    private val KEYWORDS = setOf(
        "true", "false", "null",
        "and", "or", "not",
        "eq", "ne", "lt", "le", "gt", "ge"
    )

    private val COMPARE_WORDS = setOf("eq", "ne", "lt", "le", "gt", "ge")

    private val COMPARE_SYMBOLS = setOf("==", "!=", "<", "<=", ">", ">=")

    private class Parser(private val tokens: List<Token>) {

        private var index = 0

        val atEnd: Boolean get() = index >= tokens.size

        fun parseOr(): Value? {
            var value = parseAnd() ?: return null
            while (peekSymbol("||") || peekWord("or")) {
                index++
                val right = parseAnd() ?: return null
                val leftBool = (value as? Value.Bool)?.value ?: return null
                val rightBool = (right as? Value.Bool)?.value ?: return null
                value = Value.Bool(leftBool || rightBool)
            }
            return value
        }

        private fun parseAnd(): Value? {
            var value = parseComparison() ?: return null
            while (peekSymbol("&&") || peekWord("and")) {
                index++
                val right = parseComparison() ?: return null
                val leftBool = (value as? Value.Bool)?.value ?: return null
                val rightBool = (right as? Value.Bool)?.value ?: return null
                value = Value.Bool(leftBool && rightBool)
            }
            return value
        }

        private fun parseComparison(): Value? {
            val left = parseUnary() ?: return null
            val op = comparisonOp() ?: return left
            index++
            val right = parseUnary() ?: return null
            return compare(left, op, right)
        }

        private fun parseUnary(): Value? {
            if (peekSymbol("!") || peekWord("not")) {
                index++
                val operand = parseUnary() ?: return null
                return (operand as? Value.Bool)?.let { Value.Bool(!it.value) }
            }
            if (peekSymbol("-")) {
                index++
                val operand = parsePrimary() ?: return null
                return (operand as? Value.Num)?.let { Value.Num(-it.value) }
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Value? {
            val token = tokens.getOrNull(index) ?: return null
            return when (token) {
                is Token.Sym -> if (token.text == "(") {
                    index++
                    val value = parseOr() ?: return null
                    if (!peekSymbol(")")) {
                        return null
                    }
                    index++
                    value
                } else {
                    null
                }

                is Token.Num -> {
                    index++
                    Value.Num(token.value)
                }

                is Token.Str -> {
                    index++
                    Value.Str(token.value)
                }

                is Token.Word -> when (token.text) {
                    "true" -> {
                        index++
                        Value.Bool(true)
                    }

                    "false" -> {
                        index++
                        Value.Bool(false)
                    }

                    "null" -> {
                        index++
                        Value.Null
                    }

                    else -> null
                }
            }
        }

        private fun comparisonOp(): String? = when (val token = tokens.getOrNull(index)) {
            is Token.Sym -> token.text.takeIf { it in COMPARE_SYMBOLS }
            is Token.Word -> token.text.takeIf { it in COMPARE_WORDS }
            else -> null
        }

        private fun compare(left: Value, op: String, right: Value): Value? {
            val result = when {
                left is Value.Num && right is Value.Num -> compareNumbers(left.value, op, right.value)
                left is Value.Str && right is Value.Str -> compareStrings(left.value, op, right.value)
                left is Value.Bool && right is Value.Bool -> compareBooleans(left.value, op, right.value)
                left is Value.Null && right is Value.Null -> compareNulls(op)
                // 类型不同：SpEL 的相等比较为 false，大小比较无法静态确定
                op in EQUALITY_OPS -> op in NEGATIVE_EQUALITY_OPS
                else -> null
            }
            return result?.let { Value.Bool(it) }
        }

        private fun compareNumbers(left: Double, op: String, right: Double): Boolean? = when (op) {
            "==", "eq" -> left == right
            "!=", "ne" -> left != right
            "<", "lt" -> left < right
            "<=", "le" -> left <= right
            ">", "gt" -> left > right
            ">=", "ge" -> left >= right
            else -> null
        }

        private fun compareStrings(left: String, op: String, right: String): Boolean? {
            val compared = left.compareTo(right)
            return when (op) {
                "==", "eq" -> compared == 0
                "!=", "ne" -> compared != 0
                "<", "lt" -> compared < 0
                "<=", "le" -> compared <= 0
                ">", "gt" -> compared > 0
                ">=", "ge" -> compared >= 0
                else -> null
            }
        }

        private fun compareBooleans(left: Boolean, op: String, right: Boolean): Boolean? = when (op) {
            "==", "eq" -> left == right
            "!=", "ne" -> left != right
            else -> null
        }

        private fun compareNulls(op: String): Boolean? = when (op) {
            "==", "eq" -> true
            "!=", "ne" -> false
            else -> null
        }

        private fun peekSymbol(text: String): Boolean =
            (tokens.getOrNull(index) as? Token.Sym)?.text == text

        private fun peekWord(text: String): Boolean =
            (tokens.getOrNull(index) as? Token.Word)?.text == text
    }

    private val EQUALITY_OPS = setOf("==", "eq", "!=", "ne")

    private val NEGATIVE_EQUALITY_OPS = setOf("!=", "ne")
}
