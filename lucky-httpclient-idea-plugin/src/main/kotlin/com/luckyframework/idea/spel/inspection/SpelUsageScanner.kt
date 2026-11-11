package com.luckyframework.idea.spel.inspection

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiLiteralExpression
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelSymbol

/**
 * 检查用的 SpEL 用法（变量、函数、命名空间）及其解析目标。
 *
 * [range] 相对字符串字面量的源码文本；[name] 是原样书写的符号名
 * （变量带 `#`，Root 变量不带）。
 */
data class SpelUsage(
    val range: TextRange,
    val name: String,
    val kind: SpelTokenType,
    val symbols: List<SpelSymbol>
) {
    /** 不含 `#` 前缀的符号名，用于快速修复与消息展示。 */
    val symbolName: String get() = name.removePrefix("#")
}

/**
 * 按引用提供器相同的词法规则收集可解析的符号用法（diagnostics）。
 *
 * 只产出变量、函数、命名空间三类；`.` 之后的成员与 `T(...)` 类型引用
 * 不参与检查，避免类型推导能力不足时产生误报。
 */
class SpelUsageScanner(
    private val scanner: SpelScanner = SpelScanner(),
    private val tokenizer: SpelTokenizer = SpelTokenizer()
) {

    fun scan(literal: PsiLiteralExpression, context: SpelContext): List<SpelUsage> {
        val result = ArrayList<SpelUsage>()
        for (fragment in scanner.scanLiteral(literal.text)) {
            collect(literal.text, fragment, context, result)
        }
        return result
    }

    private fun collect(
        literalText: String,
        fragment: SpelFragment,
        context: SpelContext,
        result: MutableList<SpelUsage>
    ) {
        val nested = fragment.children.map { it.textRange }
        val tokens = tokenizer.tokenize(literalText, fragment)
            .filter { token -> nested.none { it.contains(token.range) } }

        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            when (token.type) {
                SpelTokenType.VARIABLE ->
                    result += usage(token, token.text, context.variables(token.text.removePrefix("#")))

                SpelTokenType.FUNCTION ->
                    result += usage(token, token.text, context.functions(token.text.removePrefix("#")))

                SpelTokenType.NAMESPACE ->
                    result += usage(token, token.text, context.namespaces(token.text.removePrefix("#")))

                SpelTokenType.ROOT_VARIABLE -> {
                    val combined = combinedRootRange(tokens, index)
                    when {
                        combined != null -> {
                            val name = literalText.substring(combined.startOffset, combined.endOffset)
                            result += SpelUsage(combined, name, token.type, context.variables(name))
                            index += 2
                        }

                        !isTypeSuffix(tokens, index) ->
                            result += usage(token, token.text, context.variables(token.text))

                        else -> Unit
                    }
                }

                else -> Unit
            }
            index++
        }

        fragment.children.forEach { collect(literalText, it, context, result) }
    }

    private fun usage(token: SpelToken, name: String, symbols: List<SpelSymbol>): SpelUsage =
        SpelUsage(token.range, name, token.type, symbols)

    /** `name::type` / `$n::type` 组合形式的完整范围（三个 token）。 */
    private fun combinedRootRange(tokens: List<SpelToken>, index: Int): TextRange? {
        val separator = tokens.getOrNull(index + 1) ?: return null
        val suffix = tokens.getOrNull(index + 2) ?: return null
        if (separator.type != SpelTokenType.OPERATOR || separator.text != "::" ||
            suffix.type != SpelTokenType.ROOT_VARIABLE || suffix.text != "type"
        ) {
            return null
        }
        return TextRange(tokens[index].range.startOffset, suffix.range.endOffset)
    }

    /** `::type` 的后半部分属于前一个组合引用，不单独解析。 */
    private fun isTypeSuffix(tokens: List<SpelToken>, index: Int): Boolean {
        val previous = tokens.getOrNull(index - 1) ?: return false
        return previous.type == SpelTokenType.OPERATOR && previous.text == "::"
    }
}
