package com.luckyframework.idea.spel.lexer

import com.intellij.openapi.util.TextRange

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

    /**
     * 词法分析使用的文本。
     *
     * Java/YAML 双引号字面量使用还原转义后的值文本，token 范围通过
     * [sourceOffset] 映射回源码偏移；裸表达式原文即值文本，偏移不变。
     * 直接在源码文本上做词法会把转义引号（反斜杠 + 引号）当成普通反斜杠，
     * `#{T(String).format("#{x}")}` 的源码写法会被误报为语法错误。
     */
    class Source internal constructor(
        val text: String,
        val expressionRange: TextRange,
        private val mapped: Mapped?
    ) {

        /** 把值文本中的偏移换算成源码偏移。 */
        fun sourceOffset(valueOffset: Int): Int =
            mapped?.sourceOffset(valueOffset) ?: valueOffset
    }

    /**
     * 选择词法分析文本。
     *
     * [literalText] 含首尾双引号时返回还原转义后的值文本，
     * [expressionRange] 此时应传入值坐标的 [valueRange]；
     * 裸表达式没有转义语义，原文即值文本，偏移不变。
     */
    fun sourceOf(
        literalText: String,
        expressionRange: TextRange,
        valueRange: TextRange
    ): Source {
        val hasQuotes = literalText.length >= 2 &&
            literalText.first() == '"' &&
            literalText.last() == '"'
        if (!hasQuotes) {
            return Source(literalText, expressionRange, null)
        }
        val mapped = map(literalText)
        return Source(mapped.value, valueRange, mapped)
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
