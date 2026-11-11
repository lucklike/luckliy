package com.luckyframework.idea.spel.lexer

/**
 * 把 Java 字符串字面量的源码文本（含引号）转换为运行时值，
 * 并记录「值偏移 → 源码偏移」的映射，供词法分析结果回写文件偏移。
 *
 * 只处理边界识别需要的转义：常见单字符转义、八进制与 `\\uXXXX`。
 */
object JavaStringEscapeMapper {

    data class Mapped(val value: String, private val valueToSource: IntArray) {

        /**
         * 把值中的偏移换算成源码文本中的偏移。
         * 越界时钳制到有效范围，保证映射永远可用。
         */
        fun sourceOffset(valueOffset: Int): Int =
            valueToSource[valueOffset.coerceIn(0, valueToSource.size - 1)]
    }

    fun map(literalText: String): Mapped {
        val hasQuotes = literalText.length >= 2 &&
                literalText.first() == '"' &&
                literalText.last() == '"'
        val contentStart = if (hasQuotes) 1 else 0
        val contentEnd = if (hasQuotes) literalText.length - 1 else literalText.length

        val value = StringBuilder()
        val mapping = IntArray(contentEnd - contentStart + 1)
        var sourceIndex = contentStart
        var valueIndex = 0

        while (sourceIndex < contentEnd) {
            val current = literalText[sourceIndex]
            if (current == '\\' && sourceIndex + 1 < contentEnd) {
                val next = literalText[sourceIndex + 1]
                when {
                    next == 'u' && sourceIndex + 6 <= contentEnd -> {
                        val hex = literalText.substring(sourceIndex + 2, sourceIndex + 6)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            value.append(code.toChar())
                            mapping[valueIndex++] = sourceIndex
                            sourceIndex += 6
                        } else {
                            value.append(next)
                            mapping[valueIndex++] = sourceIndex
                            sourceIndex += 2
                        }
                    }

                    next in '0'..'7' -> {
                        var end = sourceIndex + 1
                        while (end < contentEnd && end < sourceIndex + 4 && literalText[end] in '0'..'7') {
                            end++
                        }
                        val code = literalText.substring(sourceIndex + 1, end).toIntOrNull(8)
                        value.append(code?.toChar() ?: next)
                        mapping[valueIndex++] = sourceIndex
                        sourceIndex = end
                    }

                    else -> {
                        value.append(unescapeChar(next))
                        mapping[valueIndex++] = sourceIndex
                        sourceIndex += 2
                    }
                }
            } else {
                value.append(current)
                mapping[valueIndex++] = sourceIndex
                sourceIndex++
            }
        }

        mapping[valueIndex] = contentEnd
        return Mapped(value.toString(), mapping.copyOf(valueIndex + 1))
    }

    private fun unescapeChar(escaped: Char): Char = when (escaped) {
        'b' -> '\b'
        't' -> '\t'
        'n' -> '\n'
        'f' -> '\u000C'
        'r' -> '\r'
        else -> escaped
    }
}
