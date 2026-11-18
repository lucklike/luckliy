package com.luckyframework.idea.spel.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.luckyframework.idea.spel.lexer.SpelElementScope
import com.luckyframework.idea.spel.lexer.SpelElementScopes
import com.luckyframework.idea.spel.lexer.SpelExpressionText
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType

/**
 * 从任意承载 SpEL 片段的元素（Java 字符串字面量、YAML 标量）收集引用。
 *
 * [text] 是元素文本（含引号），token 范围与 [SpelReference] 的范围
 * 均相对该文本。
 */
internal object SpelReferenceCollector {

    fun collect(
        element: PsiElement,
        text: String,
        navigation: SpelNavigation,
        scanner: SpelScanner = SpelScanner(),
        tokenizer: SpelTokenizer = SpelTokenizer()
    ): List<PsiReference> {
        val references = ArrayList<PsiReference>()
        for (fragment in scanner.scanLiteral(text)) {
            collectFragment(element, text, fragment, navigation, tokenizer, references)
        }
        return references
    }

    private fun collectFragment(
        element: PsiElement,
        text: String,
        fragment: SpelFragment,
        navigation: SpelNavigation,
        tokenizer: SpelTokenizer,
        references: MutableList<PsiReference>
    ) {
        val nested = fragment.children.map { it.textRange }
        val tokens = tokenizer.tokenize(text, fragment)
            .filter { token -> nested.none { it.contains(token.range) } }
        val expressionStart = fragment.expressionRange.startOffset

        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            val scopes = SpelElementScopes.enclosing(text, expressionStart, token.range.startOffset)
            when (token.type) {
                SpelTokenType.VARIABLE -> if (!SPEL_BUILTINS.contains(token.text)) {
                    add(references, element, token.range) {
                        navigation.variables(token.text.removePrefix("#"))
                    }
                }

                SpelTokenType.FUNCTION ->
                    if (isMemberAccess(tokens.getOrNull(index - 1))) {
                        addMemberReference(
                            element, text, tokens, index,
                            expressionStart, navigation, references, scopes
                        )
                    } else {
                        add(references, element, token.range) {
                            when {
                                // 元素上下文中的裸调用优先是元素方法（`length()`），
                                // 未命中时回退全局函数：`str_eq(#this.address, '3')`
                                // 运行期会从函数表解析，两者都必须能定位
                                scopes.isNotEmpty() && !token.text.startsWith("#") ->
                                    navigation.elementMembers(
                                        scopes, token.text, argumentsAt(text, tokens, index)
                                    ).ifEmpty {
                                        navigation.functions(token.text, null, includeBuiltin = true)
                                    }

                                // 内置函数只能以裸 `name(...)` 调用，`#name(...)` 不产生引用
                                else -> navigation.functions(
                                    token.text.removePrefix("#"),
                                    null,
                                    includeBuiltin = !token.text.startsWith("#")
                                )
                            }
                        }
                    }

                // `#name.` 既可能是命名空间（`#crypto.md5()`），也可能是普通变量
                // （`#user.name`）；`#this`/`#root` 是内置对象，两者都不在符号表，不产生引用
                SpelTokenType.NAMESPACE -> {
                    val name = token.text.removePrefix("#")
                    when {
                        navigation.hasNamespace(name) -> add(references, element, token.range) {
                            navigation.namespaces(name)
                        }

                        navigation.hasVariable(name) -> add(references, element, token.range) {
                            navigation.variables(name)
                        }
                    }
                }

                SpelTokenType.BEAN -> add(references, element, token.range) {
                    navigation.beans(token.text.removePrefix("@"))
                }

                SpelTokenType.TYPE_REFERENCE -> {
                    val typeReference = typeReferenceAt(tokens, text, index)
                    if (typeReference != null) {
                        add(references, element, typeReference.range) { navigation.types(typeReference.name) }
                        index = typeReference.lastIndex
                    }
                }

                SpelTokenType.ROOT_VARIABLE ->
                    index = addRootReference(element, text, tokens, index, navigation, references, scopes)

                SpelTokenType.IDENTIFIER ->
                    if (isMemberAccess(tokens.getOrNull(index - 1))) {
                        addMemberReference(
                            element, text, tokens, index,
                            expressionStart, navigation, references, scopes
                        )
                    }

                else -> Unit
            }
            index++
        }

        fragment.children.forEach {
            collectFragment(element, text, it, navigation, tokenizer, references)
        }
    }

    /**
     * Root 变量：`name`、`$name`、`$n`、`$$n`，以及 `name::type` 组合。
     *
     * 返回下一个待处理 token 下标（`name::type` 会一次消费三个 token）。
     */
    private fun addRootReference(
        element: PsiElement,
        text: String,
        tokens: List<SpelToken>,
        index: Int,
        navigation: SpelNavigation,
        references: MutableList<PsiReference>,
        scopes: List<SpelElementScope>
    ): Int {
        val token = tokens[index]
        val previous = tokens.getOrNull(index - 1)
        // `::type` 的后半部分属于前一个 `name::type` 引用
        if (previous != null && previous.type == SpelTokenType.OPERATOR && previous.text == "::") {
            return index
        }

        // 选择/投影方括号内的裸标识符是当前元素的成员
        if (scopes.isNotEmpty() && isElementMemberName(tokens, index)) {
            add(references, element, token.range) {
                navigation.elementMembers(scopes, token.text, argumentsAt(text, tokens, index))
            }
            return index
        }

        val separator = tokens.getOrNull(index + 1)
        val typeToken = tokens.getOrNull(index + 2)
        if (separator != null && separator.type == SpelTokenType.OPERATOR && separator.text == "::" &&
            typeToken != null && typeToken.type == SpelTokenType.ROOT_VARIABLE && typeToken.text == "type"
        ) {
            val range = TextRange(token.range.startOffset, typeToken.range.endOffset)
            add(references, element, range) {
                navigation.rootVariables(text.substring(range.startOffset, range.endOffset))
            }
            return index + 2
        }

        add(references, element, token.range) { navigation.rootVariables(token.text) }
        return index
    }

    /**
     * `index` 处的类型引用：`T(...)` 取括号内的类型名（`instanceof` 右侧的
     * 合法写法），`new` 后的裸类型名合并点链作为整体。
     *
     * [SpelTypeReference.hitRange] 是悬停命中的范围：`T(...)` 覆盖整个 token
     * （悬停在 `T` 或括号上也能看到类型文档），裸类型名与引用范围一致。
     */
    internal fun typeReferenceAt(tokens: List<SpelToken>, text: String, index: Int): SpelTypeReference? {
        val token = tokens.getOrNull(index) ?: return null
        if (token.type != SpelTokenType.TYPE_REFERENCE) {
            return null
        }
        val open = token.text.indexOf('(')
        val close = token.text.lastIndexOf(')')
        if (open >= 0 && close > open) {
            val start = token.range.startOffset + open + 1
            val end = token.range.startOffset + close
            val typeName = text.substring(start, end).trim()
            if (typeName.isEmpty()) {
                return null
            }
            return SpelTypeReference(typeName, TextRange(start, end), token.range, index)
        }
        var lastIndex = index
        var end = token.range.endOffset
        while (tokens.getOrNull(lastIndex + 1)?.type == SpelTokenType.DOT &&
            tokens.getOrNull(lastIndex + 2)?.type == SpelTokenType.TYPE_REFERENCE
        ) {
            lastIndex += 2
            end = tokens[lastIndex].range.endOffset
        }
        val start = token.range.startOffset
        val typeName = text.substring(start, end).replace(TYPE_NAME_WHITESPACE, "")
        if (typeName.isEmpty()) {
            return null
        }
        return SpelTypeReference(typeName, TextRange(start, end), TextRange(start, end), lastIndex)
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

    /** `.` 或 `?.` 之后的成员：命名空间函数或按接收者类型解析的字段与方法。 */
    private fun addMemberReference(
        element: PsiElement,
        text: String,
        tokens: List<SpelToken>,
        index: Int,
        expressionStart: Int,
        navigation: SpelNavigation,
        references: MutableList<PsiReference>,
        scopes: List<SpelElementScope>
    ) {
        val access = tokens[index - 1]
        val qualifier = qualifierBefore(text, access.range.startOffset, expressionStart) ?: return
        val token = tokens[index]
        val arguments = argumentsAt(text, tokens, index)
        add(references, element, token.range) {
            navigation.members(qualifier, token.text, arguments, scopes)
        }
    }

    /**
     * 方法/函数名之后的实参文本；不是调用形式时返回 null。
     *
     * 实参用于在多个重载中选中实际调用的那个，使悬停与跳转更精确。
     */
    private fun argumentsAt(text: String, tokens: List<SpelToken>, index: Int): List<String>? {
        if (tokens.getOrNull(index + 1)?.type != SpelTokenType.LPAREN) {
            return null
        }
        val close = matchingParen(tokens, index + 1) ?: return null
        val body = text.substring(tokens[index + 1].range.endOffset, tokens[close].range.startOffset)
        return SpelExpressionText.splitArguments(body)
    }

    /** 与 [open] 位置左括号匹配的右括号 token 下标。 */
    private fun matchingParen(tokens: List<SpelToken>, open: Int): Int? {
        var depth = 0
        for (index in open until tokens.size) {
            when (tokens[index].type) {
                SpelTokenType.LPAREN -> depth++
                SpelTokenType.RPAREN -> {
                    depth--
                    if (depth == 0) {
                        return index
                    }
                }

                else -> Unit
            }
        }
        return null
    }

    /**
     * 从 `.`（或 `?.` 的 `?`）向前回溯接收者表达式文本。
     *
     * 回溯规则与补全、调用检查共用 [SpelExpressionText.receiverStart]，
     * 支持函数结果、方法链、下标、字符串、`T(...)`、`@bean` 与 `new` 接收者。
     */
    private fun qualifierBefore(text: String, accessOffset: Int, expressionStart: Int): String? {
        if (accessOffset <= expressionStart) {
            return null
        }
        val prefix = text.substring(expressionStart, accessOffset)
        val start = expressionStart + SpelExpressionText.receiverStart(prefix)
        return text.substring(start, accessOffset).trim().takeIf { it.isNotEmpty() }
    }

    /** 成员访问前缀：`.` 或安全导航 `?.`（tokenizer 将 `?.` 识别为单个操作符）。 */
    private fun isMemberAccess(token: SpelToken?): Boolean =
        token != null && (token.type == SpelTokenType.DOT ||
            (token.type == SpelTokenType.OPERATOR && token.text == "?."))

    private fun add(
        references: MutableList<PsiReference>,
        element: PsiElement,
        range: TextRange,
        targetProvider: () -> List<SpelDeclaration>
    ) {
        if (range.length <= 0 || range.endOffset > element.textLength) {
            return
        }
        references += SpelReference(element, range, targetProvider = targetProvider)
    }

    /** 类型名内部的多余空白（`new java . util .List`）会在查类前移除。 */
    private val TYPE_NAME_WHITESPACE = Regex("\\s+")

    /** SpEL 运行期内置对象，不出现在变量表/命名空间表中。 */
    private val SPEL_BUILTINS = setOf("#this", "#root")
}

/**
 * 一个类型引用：引用范围与悬停命中范围分开，点链类型名合并为单个引用。
 */
internal data class SpelTypeReference(
    val name: String,
    val range: TextRange,
    val hitRange: TextRange,
    val lastIndex: Int
)
