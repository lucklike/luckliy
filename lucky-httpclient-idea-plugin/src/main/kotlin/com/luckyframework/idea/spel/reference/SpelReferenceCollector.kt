package com.luckyframework.idea.spel.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
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

        var index = 0
        while (index < tokens.size) {
            val token = tokens[index]
            when (token.type) {
                SpelTokenType.VARIABLE -> add(references, element, token.range) {
                    navigation.variables(token.text.removePrefix("#"))
                }

                SpelTokenType.FUNCTION -> add(references, element, token.range) {
                    navigation.functions(token.text.removePrefix("#"), null)
                }

                SpelTokenType.NAMESPACE -> add(references, element, token.range) {
                    navigation.namespaces(token.text.removePrefix("#"))
                }

                SpelTokenType.BEAN -> add(references, element, token.range) {
                    navigation.beans(token.text.removePrefix("@"))
                }

                SpelTokenType.TYPE_REFERENCE ->
                    addTypeReference(element, text, token, navigation, references)

                SpelTokenType.ROOT_VARIABLE ->
                    index = addRootReference(element, text, tokens, index, navigation, references)

                SpelTokenType.IDENTIFIER ->
                    if (index > 0 && tokens[index - 1].type == SpelTokenType.DOT) {
                        addMemberReference(element, text, tokens, index, navigation, references)
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
        references: MutableList<PsiReference>
    ): Int {
        val token = tokens[index]
        val previous = tokens.getOrNull(index - 1)
        // `::type` 的后半部分属于前一个 `name::type` 引用
        if (previous != null && previous.type == SpelTokenType.OPERATOR && previous.text == "::") {
            return index
        }

        val separator = tokens.getOrNull(index + 1)
        val typeToken = tokens.getOrNull(index + 2)
        if (separator != null && separator.type == SpelTokenType.OPERATOR && separator.text == "::" &&
            typeToken != null && typeToken.type == SpelTokenType.ROOT_VARIABLE && typeToken.text == "type"
        ) {
            val range = TextRange(token.range.startOffset, typeToken.range.endOffset)
            add(references, element, range) {
                navigation.variables(text.substring(range.startOffset, range.endOffset))
            }
            return index + 2
        }

        add(references, element, token.range) { navigation.variables(token.text) }
        return index
    }

    /** `T(...)` 的引用范围只覆盖括号内的类型名。 */
    private fun addTypeReference(
        element: PsiElement,
        text: String,
        token: SpelToken,
        navigation: SpelNavigation,
        references: MutableList<PsiReference>
    ) {
        val open = token.text.indexOf('(')
        val close = token.text.lastIndexOf(')')
        if (open < 0 || close <= open) {
            return
        }
        val start = token.range.startOffset + open + 1
        val end = token.range.startOffset + close
        val typeName = text.substring(start, end).trim()
        if (typeName.isEmpty()) {
            return
        }
        add(references, element, TextRange(start, end)) { navigation.types(typeName) }
    }

    /** `.` 之后的成员：命名空间函数或按接收者类型解析的字段与方法。 */
    private fun addMemberReference(
        element: PsiElement,
        text: String,
        tokens: List<SpelToken>,
        index: Int,
        navigation: SpelNavigation,
        references: MutableList<PsiReference>
    ) {
        val qualifier = qualifierBefore(text, tokens, index - 1) ?: return
        val token = tokens[index]
        add(references, element, token.range) { navigation.members(qualifier, token.text) }
    }

    /** 从 `.` 向前收集接收者表达式文本；复杂表达式（含括号调用）暂不处理。 */
    private fun qualifierBefore(text: String, tokens: List<SpelToken>, dotIndex: Int): String? {
        var start = -1
        var cursor = dotIndex - 1
        while (cursor >= 0) {
            when (tokens[cursor].type) {
                SpelTokenType.IDENTIFIER,
                SpelTokenType.ROOT_VARIABLE,
                SpelTokenType.VARIABLE,
                SpelTokenType.FUNCTION,
                SpelTokenType.NAMESPACE -> {
                    start = tokens[cursor].range.startOffset
                    cursor--
                }

                SpelTokenType.DOT -> cursor--

                else -> break
            }
        }
        if (start < 0) {
            return null
        }
        val dot = tokens[dotIndex].range.startOffset
        return text.substring(start, dot).trim().removeSuffix(".")
    }

    private fun add(
        references: MutableList<PsiReference>,
        element: PsiElement,
        range: TextRange,
        targetProvider: () -> List<SpelDeclaration>
    ) {
        if (range.length <= 0 || range.endOffset > element.textLength) {
            return
        }
        references += SpelReference(element, range, targetProvider)
    }
}
