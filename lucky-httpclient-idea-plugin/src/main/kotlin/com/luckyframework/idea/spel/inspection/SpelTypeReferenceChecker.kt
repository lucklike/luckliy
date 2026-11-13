package com.luckyframework.idea.spel.inspection

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.luckyframework.idea.spel.completion.SpelTypeResolver
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelSyntaxParser
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.reference.SpelReferenceCollector
import com.luckyframework.idea.spel.symbol.SpelContext

/**
 * 一个类型引用问题；[range] 相对所在字符串字面量的源码文本。
 */
data class SpelTypeProblem(
    val range: TextRange,
    val message: String
)

/**
 * `T(...)`（含 `instanceof T(...)`）与 `new` 之后的类型引用存在性检查。
 *
 * 类型都由 tokenizer 标记为 [SpelTokenType.TYPE_REFERENCE]，这里按
 * [SpelReferenceCollector.typeReferenceAt] 合并点链后交给 [SpelTypeResolver]
 * 查找；查不到对应的类时报错，帮助尽早发现拼错的类名与不存在的全限定名。
 *
 * 保守放行的场景：语法不完整的片段、`Any`/`?` 等没有确定类的类型标记、
 * 基本类型（由包装类承载），以及点链中间的类型名（由链首整体检查，
 * 避免同一个错误重复报告）。`Object` 是合法的 `java.lang` 类，照常检查。
 */
class SpelTypeReferenceChecker(
    private val scanner: SpelScanner = SpelScanner(),
    private val tokenizer: SpelTokenizer = SpelTokenizer(),
    private val syntax: SpelSyntaxParser = SpelSyntaxParser()
) {

    fun check(literalText: String, context: SpelContext, project: Project): List<SpelTypeProblem> {
        val problems = ArrayList<SpelTypeProblem>()
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
        problems: MutableList<SpelTypeProblem>
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
        problems: MutableList<SpelTypeProblem>
    ) {
        val nested = fragment.children.map { it.textRange }
        val tokens = tokenizer.tokenize(literalText, fragment)
            .filter { token -> nested.none { it.contains(token.range) } }
        if (tokens.isEmpty()) {
            return
        }
        val typeResolver = SpelTypeResolver(project, context)
        for (index in tokens.indices) {
            checkTypeReference(literalText, tokens, index, typeResolver, problems)
        }
    }

    private fun checkTypeReference(
        literalText: String,
        tokens: List<SpelToken>,
        index: Int,
        typeResolver: SpelTypeResolver,
        problems: MutableList<SpelTypeProblem>
    ) {
        if (tokens[index].type != SpelTokenType.TYPE_REFERENCE) {
            return
        }
        // 点链的中间段会被链首整体检查，这里跳过避免同一错误重复报告
        if (isChainContinuation(tokens, index)) {
            return
        }
        val reference = SpelReferenceCollector.typeReferenceAt(tokens, literalText, index) ?: return
        val raw = literalText.substring(reference.range.startOffset, reference.range.endOffset)
        val name = raw.trim()
        if (!isCheckable(name)) {
            return
        }
        if (typeResolver.findTypeReference(name) != null) {
            return
        }
        problems += SpelTypeProblem(
            trimmedRange(reference.range, raw, name),
            "未解析的 SpEL 类型 '$name'"
        )
    }

    /** `index` 处的类型名是否属于从更早 token 开始的点链（`java.util.List` 的 `util`、`List`）。 */
    private fun isChainContinuation(tokens: List<SpelToken>, index: Int): Boolean {
        if (tokens.getOrNull(index - 1)?.type != SpelTokenType.DOT) {
            return false
        }
        return tokens.getOrNull(index - 2)?.type == SpelTokenType.TYPE_REFERENCE
    }

    /**
     * 需要做存在性检查的类型名。
     *
     * `Any`/`?` 没有可校验的类，基本类型由包装类承载，判定为不存在没有意义，
     * 统一放行；`Object` 是合法的 `java.lang.Object`，仍然参与检查。
     */
    private fun isCheckable(typeName: String): Boolean {
        val simple = simpleName(typeName)
        if (simple.isEmpty()) {
            return false
        }
        return simple != "?" && simple != "Any" && simple !in PRIMITIVE_TYPES
    }

    /** 去掉泛型实参、数组标记与空白后的裸类型名。 */
    private fun simpleName(typeName: String): String {
        var name = typeName.substringBefore('<').trim()
        while (name.endsWith("[]")) {
            name = name.removeSuffix("[]").trim()
        }
        if (name.endsWith("...")) {
            name = name.removeSuffix("...").trim()
        }
        return name.substringAfterLast(' ').trim()
    }

    /** 去掉 `T( 类型 )` 括号内首尾空白后的精确高亮范围；无法定位时回退原范围。 */
    private fun trimmedRange(range: TextRange, raw: String, name: String): TextRange {
        val start = raw.indexOf(name)
        if (start < 0) {
            return range
        }
        return TextRange(range.startOffset + start, range.startOffset + start + name.length)
    }

    private companion object {
        val PRIMITIVE_TYPES = setOf(
            "boolean", "byte", "char", "short", "int", "long", "float", "double", "void"
        )
    }
}
