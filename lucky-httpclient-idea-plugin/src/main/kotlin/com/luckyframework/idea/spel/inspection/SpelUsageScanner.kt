package com.luckyframework.idea.spel.inspection

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiLiteralExpression
import com.luckyframework.idea.spel.lexer.SpelElementScopes
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelSymbol

/**
 * 检查用的 SpEL 用法（变量、函数、命名空间）及其解析目标。
 *
 * [range] 相对字符串字面量的源码文本；[name] 是原样书写的符号名
 * （变量带 `#`，Root 变量不带）。[alternativeSymbols] 是同名但书写形式
 * 不同的符号（如 `#param` 对应的 Root 变量、`#fn(...)` 对应的内置函数），
 * 用于给出「换一种写法即可解析」的诊断。
 */
data class SpelUsage(
    val range: TextRange,
    val name: String,
    val kind: SpelTokenType,
    val symbols: List<SpelSymbol>,
    val alternativeSymbols: List<SpelSymbol> = emptyList()
) {
    /** 不含 `#` 前缀的符号名，用于快速修复与消息展示。 */
    val symbolName: String get() = name.removePrefix("#")
}

/**
 * 按引用提供器相同的词法规则收集可解析的符号用法（diagnostics）。
 *
 * 只产出变量、函数、命名空间三类；`.` 之后的成员由
 * [SpelMemberAccessInspection] 按接收者类型单独检查，`T(...)` 类型引用不参与
 * 未解析检查，避免类型推导能力不足时产生误报。
 *
 * 选择/投影方括号内的 `#this`、裸标识符与裸函数可能是元素成员（`age`、
 * `length()`），也可能是全局函数（`str_eq(...)`）：裸标识符不参与变量表解析，
 * 裸函数先查函数表，未命中再交给成员检查；`$name$` 响应变量仍按 Root
 * 变量检查。
 */
class SpelUsageScanner(
    private val scanner: SpelScanner = SpelScanner(),
    private val tokenizer: SpelTokenizer = SpelTokenizer()
) {

    fun scan(literal: PsiLiteralExpression, context: SpelContext): List<SpelUsage> {
        if (!literal.text.contains("#{")) {
            return functionNameUsages(literal, context)
        }
        val result = ArrayList<SpelUsage>()
        for (fragment in scanner.scanLiteral(literal.text)) {
            collect(literal.text, fragment, context, result)
        }
        return result
    }

    /**
     * 函数名属性（`kind = FUNCTION`）：整段文本就是函数名，按变量表的平铺名
     * 解析（与 [com.luckyframework.idea.spel.reference.SpelReferenceProvider] 一致）。
     *
     * 空内容表示尚未填写，不产生用法；普通字符串属性不是函数名，也不产生用法。
     */
    private fun functionNameUsages(
        literal: PsiLiteralExpression,
        context: SpelContext
    ): List<SpelUsage> {
        val resolver = SpelAnnotationResolver()
        if (!resolver.isFunctionNameLiteral(literal)) {
            return emptyList()
        }
        val name = resolver.functionNameOf(literal) ?: return emptyList()
        if (name.isEmpty()) {
            return emptyList()
        }
        return listOf(
            SpelUsage(
                TextRange(1, literal.textLength - 1),
                name,
                SpelTokenType.FUNCTION,
                context.functionNames(name)
            )
        )
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
        val expressionStart = fragment.expressionRange.startOffset

        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            val elementContext = SpelElementScopes
                .enclosing(literalText, expressionStart, token.range.startOffset)
                .isNotEmpty()
            when (token.type) {
                // `#this`/`#root` 是 SpEL 内置的当前对象，不在变量表里
                SpelTokenType.VARIABLE -> if (!SPEL_BUILTINS.contains(token.text)) {
                    val name = token.text.removePrefix("#")
                    result += SpelUsage(
                        token.range, token.text, token.type,
                        context.variables(name), context.rootVariables(name)
                    )
                }

                // 元素上下文中的裸调用既可能是元素方法（`length()`），也可能是全局函数
                // （`users.?[str_eq(#this.name, 'a')]`）：函数表命中时按函数处理，
                // 未命中时留给成员检查按元素方法校验。
                // 内置函数只能以裸 `name(...)` 调用，`#name(...)` 只解析非内置函数
                SpelTokenType.FUNCTION -> {
                    val name = token.text.removePrefix("#")
                    val hashed = token.text.startsWith("#")
                    val symbols = context.functions(name, null, includeBuiltin = !hashed)
                    val alternative = if (hashed && symbols.isEmpty()) {
                        context.functions(name, null)
                    } else {
                        emptyList()
                    }
                    if (!elementContext || hashed || symbols.isNotEmpty()) {
                        result += SpelUsage(token.range, token.text, token.type, symbols, alternative)
                    }
                }

                // `#name.` 既可能是命名空间（`#crypto.md5()`），也可能是普通变量
                // （`#user.name`）：命名空间查不到时回退变量表；`#this`/`#root`
                // 是 SpEL 内置对象，不在符号表里，不参与未解析检查
                SpelTokenType.NAMESPACE -> if (!SPEL_BUILTINS.contains(token.text)) {
                    val name = token.text.removePrefix("#")
                    val namespaces = context.namespaces(name)
                    if (namespaces.isNotEmpty()) {
                        result += SpelUsage(token.range, token.text, token.type, namespaces)
                    } else {
                        // Root 变量写成 `#name` 时给出书写形式提示
                        result += SpelUsage(
                            token.range, token.text, token.type,
                            context.variables(name), context.rootVariables(name)
                        )
                    }
                }

                SpelTokenType.ROOT_VARIABLE -> {
                    val combined = combinedRootRange(tokens, index)
                    when {
                        // 元素上下文中的裸标识符是元素属性，由成员检查负责
                        elementContext && isElementMemberName(tokens, index) -> Unit

                        combined != null -> {
                            val name = literalText.substring(combined.startOffset, combined.endOffset)
                            result += SpelUsage(
                                combined, name, token.type,
                                context.rootVariables(name), context.variables(name)
                            )
                            index += 2
                        }

                        !isTypeSuffix(tokens, index) ->
                            result += SpelUsage(
                                token.range, token.text, token.type,
                                context.rootVariables(token.text), context.variables(token.text)
                            )

                        else -> Unit
                    }
                }

                else -> Unit
            }
            index++
        }

        fragment.children.forEach { collect(literalText, it, context, result) }
    }

    /** 元素成员形式的裸标识符：不含 `$` 简写，也不是 `name::type` 组合的左侧。 */
    private fun isElementMemberName(tokens: List<SpelToken>, index: Int): Boolean {
        val token = tokens[index]
        if (token.text.startsWith("$")) {
            return false
        }
        val next = tokens.getOrNull(index + 1)
        return next == null || !(next.type == SpelTokenType.OPERATOR && next.text == "::")
    }

    private companion object {
        /** SpEL 运行期内置对象，不出现在变量表/命名空间表中。 */
        val SPEL_BUILTINS = setOf("#this", "#root")
    }

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
