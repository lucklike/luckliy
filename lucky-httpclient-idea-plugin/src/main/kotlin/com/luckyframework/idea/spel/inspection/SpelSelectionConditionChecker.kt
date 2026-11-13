package com.luckyframework.idea.spel.inspection

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiTypes
import com.luckyframework.idea.spel.completion.SpelElementContext
import com.luckyframework.idea.spel.completion.SpelTypeResolver
import com.luckyframework.idea.spel.lexer.SpelElementScopes
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelSyntaxParser
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.symbol.SpelContext

/**
 * 一个过滤条件问题；[range] 相对所在字符串字面量的源码文本。
 */
data class SpelConditionProblem(
    val range: TextRange,
    val message: String
)

/**
 * 选择（过滤）表达式 `?[...]`/`^[...]`/`$[...]` 的条件类型检查。
 *
 * Spring 求值过滤条件时要求结果是 `boolean`/`Boolean`（其余类型抛
 * `SpelEvaluationException`）。条件表达式能静态推导出确定类型且不是布尔时报告问题。
 *
 * 保守放行的场景：条件类型无法推导（`age > 1` 这类二元表达式、未知符号）、
 * 泛型类型变量（运行期取决于实参）、`![...]` 投影（结果类型由投影表达式
 * 决定，不参与条件判断）。
 */
class SpelSelectionConditionChecker(
    private val scanner: SpelScanner = SpelScanner(),
    private val tokenizer: SpelTokenizer = SpelTokenizer(),
    private val syntax: SpelSyntaxParser = SpelSyntaxParser()
) {

    fun check(literalText: String, context: SpelContext, project: Project): List<SpelConditionProblem> {
        val problems = ArrayList<SpelConditionProblem>()
        for (fragment in scanner.scanLiteral(literalText)) {
            collect(literalText, fragment, context, project, problems)
        }
        return problems
    }

    private fun collect(
        literalText: String,
        fragment: SpelFragment,
        context: SpelContext,
        project: Project,
        problems: MutableList<SpelConditionProblem>
    ) {
        // 结构或语法不完整的片段交给语法高亮，避免在错误文本上叠加类型告警
        if (fragment.problems.isEmpty() && syntax.problemsOf(literalText, fragment).isEmpty()) {
            checkFragment(literalText, fragment, context, project, problems)
        }
        fragment.children.forEach { collect(literalText, it, context, project, problems) }
    }

    private fun checkFragment(
        literalText: String,
        fragment: SpelFragment,
        context: SpelContext,
        project: Project,
        problems: MutableList<SpelConditionProblem>
    ) {
        val nested = fragment.children.map { it.textRange }
        val tokens = tokenizer.tokenize(literalText, fragment)
            .filter { token -> nested.none { it.contains(token.range) } }
        if (tokens.isEmpty()) {
            return
        }
        val typeResolver = SpelTypeResolver(project, context)
        for (index in tokens.indices) {
            // `?[`/`^[`/`$[` 是选择（过滤）标记，`![` 是投影，不是条件
            if (tokens[index].type == SpelTokenType.SELECTION) {
                checkSelection(literalText, fragment, tokens, index, typeResolver, problems)
            }
        }
    }

    private fun checkSelection(
        literalText: String,
        fragment: SpelFragment,
        tokens: List<SpelToken>,
        index: Int,
        typeResolver: SpelTypeResolver,
        problems: MutableList<SpelConditionProblem>
    ) {
        val token = tokens[index]
        val close = matchingCloseBracket(tokens, index) ?: return
        val conditionStart = token.range.endOffset
        val conditionEnd = close.range.startOffset
        if (conditionEnd <= conditionStart) {
            return
        }
        val raw = literalText.substring(conditionStart, conditionEnd)
        val condition = raw.trim()
        if (condition.isEmpty()) {
            return
        }
        // 条件内部的 #this/裸标识符按当前元素类型解析
        val elementType = elementTypeAt(literalText, fragment, conditionStart, typeResolver)
        val type = typeResolver.resolvePsiType(condition, elementType) ?: return
        if (isBoolean(type)) {
            return
        }
        // 泛型类型变量的实际类型取决于运行期实参，无法静态判断
        if (type is PsiTypeParameter) {
            return
        }
        val offset = conditionStart + raw.indexOf(condition.first())
        problems += SpelConditionProblem(
            TextRange(offset, offset + condition.length),
            "过滤条件的结果类型必须是 boolean，当前为 '${type.presentableText}'"
        )
    }

    /** 与 [openIndex] 处的选择/投影方括号配对的 `]`；未闭合时返回 null。 */
    private fun matchingCloseBracket(tokens: List<SpelToken>, openIndex: Int): SpelToken? {
        var depth = 0
        for (index in openIndex + 1 until tokens.size) {
            when (tokens[index].type) {
                SpelTokenType.SELECTION, SpelTokenType.PROJECTION, SpelTokenType.LBRACKET -> depth++
                SpelTokenType.RBRACKET -> {
                    if (depth == 0) {
                        return tokens[index]
                    }
                    depth--
                }

                else -> Unit
            }
        }
        return null
    }

    /** 条件表达式起始位置所在的选择/投影元素类型；不在元素上下文中时返回 null。 */
    private fun elementTypeAt(
        literalText: String,
        fragment: SpelFragment,
        offset: Int,
        typeResolver: SpelTypeResolver
    ): PsiType? {
        val scopes = SpelElementScopes.enclosing(literalText, fragment.expressionRange.startOffset, offset)
        if (scopes.isEmpty()) {
            return null
        }
        return SpelElementContext.elementType(scopes, typeResolver)
    }

    /** `boolean` 与 `java.lang.Boolean` 都满足过滤条件。 */
    private fun isBoolean(type: PsiType): Boolean =
        type == PsiTypes.booleanType() || type.equalsToText(CommonClassNames.JAVA_LANG_BOOLEAN)
}
