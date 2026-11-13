package com.luckyframework.idea.spel.completion

import com.luckyframework.idea.spel.lexer.SpelElementScope
import com.luckyframework.idea.spel.lexer.SpelElementScopes
import com.luckyframework.idea.spel.lexer.SpelExpressionText

/**
 * 光标所在位置的补全类别。
 */
enum class SpelCompletionMode {
    /** `#` 之后的变量、函数与命名空间 */
    VARIABLE,

    /** 表达式起始位置或运算符之后的 Root 变量 */
    ROOT,

    /** `.` 之后的成员 */
    MEMBER,

    /** 选择/投影方括号内的裸标识符：按当前元素类型的成员补全 */
    ELEMENT,

    /** `T(`、`new`、`instanceof` 之后的类型（`instanceof` 自动补 `T(...)`） */
    TYPE,

    /** `@` 之后的 Spring Bean */
    BEAN
}

/**
 * 一个补全位置。
 *
 * [prefixOffset] 是候选替换范围的起点（`#` 之后时包含 `#`，并跳过中间的空白）；
 * [wordOffset] 是当前输入单词的起点，`$` 简写候选需要保留前面的 `#`。
 * [qualifier] 是成员补全时 `.` 左侧的接收者表达式；
 * [typePrefix] 是类型补全时已经输入并确认的包限定前缀（如 `java.util.`）；
 * [wrapInTypeReference] 表示候选要自动包一层 `T(...)`：`instanceof` 的
 * 右操作数必须是类型引用，裸类名不是合法 SpEL；
 * [elementScopes] 是光标所在的选择/投影方括号（由外到内），用于把 `#this`
 * 与裸标识符解析到当前元素类型。
 */
data class SpelCompletionSite(
    val mode: SpelCompletionMode,
    val word: String,
    val wordOffset: Int,
    val prefixOffset: Int,
    val qualifier: String? = null,
    val typePrefix: String = "",
    val wrapInTypeReference: Boolean = false,
    val elementScopes: List<SpelElementScope> = emptyList()
)

/**
 * 判断光标是否位于一个未闭合的 `#{...}` 片段内，并区分补全类别。
 *
 * 只处理字面量内部文本，不依赖 PSI；字符串字面量内部不补全。
 */
class SpelCompletionAnalyzer {

    fun analyze(literalText: String, offsetInLiteral: Int): SpelCompletionSite? {
        if (offsetInLiteral <= 0 || offsetInLiteral > literalText.length) {
            return null
        }
        val expressionStart = unclosedExpressionStart(literalText, offsetInLiteral) ?: return null
        if (insideString(literalText, expressionStart, offsetInLiteral)) {
            return null
        }

        var wordOffset = offsetInLiteral
        while (wordOffset > expressionStart && isWordPart(literalText[wordOffset - 1])) {
            wordOffset--
        }
        val word = literalText.substring(wordOffset, offsetInLiteral)
        val rawHead = literalText.substring(expressionStart, wordOffset)
        val head = rawHead.trimEnd()
        // 光标所在的选择/投影方括号：`members.?[<caret>]` 内的裸标识符按元素类型补全
        val elementScopes = SpelElementScopes.enclosing(literalText, expressionStart, offsetInLiteral)

        // 类型位置：`T(`、`new`、`instanceof` 之后；支持已输入的包限定前缀
        typePositionAt(rawHead)?.let { position ->
            return SpelCompletionSite(
                SpelCompletionMode.TYPE,
                word,
                wordOffset,
                wordOffset,
                typePrefix = position.prefix,
                wrapInTypeReference = position.wrapInTypeReference
            )
        }
        if (head.endsWith("@")) {
            return SpelCompletionSite(SpelCompletionMode.BEAN, word, wordOffset, wordOffset)
        }
        if (head.endsWith(".")) {
            // `?.` 的接收者在 `?` 之前
            val receiver = head.dropLast(1).removeSuffix("?").trimEnd()
            val qualifier = receiver.substring(SpelExpressionText.receiverStart(receiver)).trim()
            if (qualifier.isEmpty()) {
                return null
            }
            return SpelCompletionSite(
                SpelCompletionMode.MEMBER,
                word,
                wordOffset,
                wordOffset,
                qualifier,
                elementScopes = elementScopes
            )
        }
        if (head.endsWith("#")) {
            val hashOffset = expressionStart + head.length - 1
            return SpelCompletionSite(
                SpelCompletionMode.VARIABLE,
                word,
                wordOffset,
                hashOffset,
                elementScopes = elementScopes
            )
        }
        if (head.isEmpty() ||
            ROOT_BOUNDARY.containsMatchIn(head.takeLast(2)) ||
            WORD_OPERATOR.containsMatchIn(head)
        ) {
            // 方括号内的裸标识符是元素成员，不回退到 Root 变量
            val mode = if (elementScopes.isEmpty()) SpelCompletionMode.ROOT else SpelCompletionMode.ELEMENT
            return SpelCompletionSite(mode, word, wordOffset, wordOffset, elementScopes = elementScopes)
        }
        return null
    }

    /**
     * 类型位置：`T(`、`new`、`instanceof` 之后，支持已输入的包限定前缀。
     *
     * 返回 null 表示不在类型位置；[TypePosition.prefix] 为空串表示还没有输入
     * 前缀，当前单词仍由调用方的 `word` 提供。`instanceof` 的右操作数必须是
     * `T(...)`，因此 [TypePosition.wrapInTypeReference] 为 true，候选插入时
     * 自动补上类型引用括号。
     *
     * `head` 不包含光标前的当前单词，因此这里看到的总是完整的限定前缀：
     * `new java.util.Arr` 拿到 `java.util.`，`T(java.` 同样按前缀处理，
     * 交给类型候选按限定名过滤。
     */
    private fun typePositionAt(head: String): TypePosition? {
        if (head.isEmpty()) {
            return null
        }
        val trimmed = head.trimEnd()
        if (trimmed.isEmpty()) {
            return null
        }
        // 关键字后直接是空白（`new <caret>`/`instanceof <caret>`）或 `T(<caret>`
        if (TYPE_TRIGGER.containsMatchIn(trimmed)) {
            return TypePosition("", false)
        }
        if (INSTANCEOF_TRIGGER.containsMatchIn(trimmed)) {
            return TypePosition("", true)
        }
        var start = trimmed.length
        while (start > 0) {
            val char = trimmed[start - 1]
            if (Character.isJavaIdentifierPart(char) || char == '.') start-- else break
        }
        val before = trimmed.substring(0, start).trimEnd()
        if (!TYPE_TRIGGER.containsMatchIn(before) && !INSTANCEOF_TRIGGER.containsMatchIn(before)) {
            return null
        }
        val prefix = trimmed.substring(start)
        if (prefix.isNotEmpty() && !TYPE_PATH_PREFIX.matches(prefix)) {
            return null
        }
        return TypePosition(prefix, INSTANCEOF_TRIGGER.containsMatchIn(before))
    }

    /** 类型位置与是否需要包一层 `T(...)`。 */
    private data class TypePosition(val prefix: String, val wrapInTypeReference: Boolean)

    /** 最后一个包含光标的未闭合 `#{` 的表达式起点；没有时返回 null。 */
    private fun unclosedExpressionStart(text: String, offset: Int): Int? {
        var from = 0
        var result: Int? = null
        while (true) {
            val index = text.indexOf("#{", from)
            if (index < 0 || index + 2 > offset) {
                break
            }
            if (isOpenAt(text, index, offset)) {
                result = index + 2
            }
            from = index + 2
        }
        return result
    }

    /** 从 `#{` 开始扫描到光标；花括号未配平时视为仍在片段内。 */
    private fun isOpenAt(text: String, fragmentStart: Int, offset: Int): Boolean {
        var depth = 0
        var quote: Char? = null
        var index = fragmentStart
        while (index < offset) {
            val current = text[index]
            if (quote != null) {
                when {
                    current == '\\' -> {
                        index += 2
                        continue
                    }

                    current == quote -> quote = null
                }
                index++
                continue
            }
            when (current) {
                '\'', '"' -> quote = current
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth <= 0) {
                        return false
                    }
                }
            }
            index++
        }
        return depth > 0
    }

    /** 光标是否位于片段内的字符串字面量中。 */
    private fun insideString(text: String, from: Int, to: Int): Boolean {
        var quote: Char? = null
        var index = from
        while (index < to) {
            val current = text[index]
            if (quote != null) {
                when {
                    current == '\\' -> {
                        index += 2
                        continue
                    }

                    current == quote -> quote = null
                }
            } else if (current == '\'' || current == '"') {
                quote = current
            }
            index++
        }
        return quote != null
    }

    private fun isWordPart(char: Char): Boolean =
        Character.isJavaIdentifierPart(char) || char == '$'

    private companion object {
        /** 类型位置的起始关键字：`T(`、`new`。 */
        val TYPE_TRIGGER = Regex(
            """(?:(?<![A-Za-z0-9_$.#@])T\s*\(|(?<![A-Za-z0-9_$.#@])new)\s*$"""
        )

        /** `instanceof` 之后：补全结果需要自动包一层 `T(...)`。 */
        val INSTANCEOF_TRIGGER = Regex("""(?<![A-Za-z0-9_$.#@])instanceof\s*$""")

        /** 类型位置已输入的包限定前缀：以 `.` 结尾的标识符段。 */
        val TYPE_PATH_PREFIX = Regex("""(?:[A-Za-z_$][A-Za-z0-9_$]*\.)+""")

        /** 表达式边界：运算符、括号与内联集合的 `{` 之后都是 Root 位置。 */
        val ROOT_BOUNDARY = Regex("""[(\[{,?:+\-*/%!<>=&|^~]""")

        /** 文本运算符（`and`/`eq` 等）之后同样是 Root 位置。 */
        val WORD_OPERATOR = Regex(
            """(?<![A-Za-z0-9_$.#])(?:and|or|not|eq|ne|lt|le|gt|ge|div|mod)$"""
        )
    }
}
