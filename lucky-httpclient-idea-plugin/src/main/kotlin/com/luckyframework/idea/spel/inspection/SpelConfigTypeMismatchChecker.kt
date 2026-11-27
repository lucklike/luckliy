package com.luckyframework.idea.spel.inspection

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import com.luckyframework.idea.spel.completion.SpelTypeResolver
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelSyntaxParser
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.symbol.SpelContext

/**
 * 一个「表达式结果类型与配置字段类型不兼容」的问题；[range] 相对 YAML 标量文本。
 */
data class SpelConfigTypeProblem(
    val range: TextRange,
    val message: String
)

/**
 * YAML 配置值中表达式结果类型与 `@SpelConfig` 字段期望类型的兼容性检查。
 *
 * 只检查「唯一片段且覆盖整个值」的纯表达式：多个片段或片段外还有文本时
 * 结果是 String，内容无法静态确定，交由用户判断。
 *
 * 框架运行期通过 `ConversionUtils` 把求值结果转换成字段声明类型，
 * 转换范围很宽（String、基本类型、容器、POJO 之间互转），因此只在
 * **一定失败或一定静默失真**的组合上报告：
 *
 * - 字段是 boolean/Boolean，结果是数值、字符或 Map：`Boolean.parseBoolean`
 *   对非 "true" 的输入静默返回 false，配置永远不生效；
 * - 字段是数值，结果是 boolean、字符或 Map：数值解析直接抛
 *   `TypeConversionException`。
 *
 * 其余组合保守放行：String 的内容未知（`"123"` 可以转成数值、`"true"`
 * 可以转成布尔）、集合会取首元素后转换、POJO 的 toString 也可能恰好可解析，
 * 都不足以判定不兼容，避免误报。
 */
class SpelConfigTypeMismatchChecker(
    private val scanner: SpelScanner = SpelScanner(),
    private val syntax: SpelSyntaxParser = SpelSyntaxParser()
) {

    fun check(
        text: String,
        context: SpelContext,
        project: Project,
        anchor: PsiElement? = null
    ): List<SpelConfigTypeProblem> {
        val expected = context.expectedType ?: return emptyList()
        val fragment = scanner.scanLiteral(text).singleOrNull() ?: return emptyList()
        if (!coversValue(text, fragment) || fragment.problems.isNotEmpty()) {
            return emptyList()
        }
        // 语法不完整的片段交给语法高亮，避免在错误文本上叠加类型告警
        if (syntax.problemsOf(text, fragment).isNotEmpty()) {
            return emptyList()
        }
        val expression = text.substring(
            fragment.expressionRange.startOffset,
            fragment.expressionRange.endOffset
        ).trim()
        if (expression.isEmpty()) {
            return emptyList()
        }
        // 推导不出结果类型时 resultType 回退为期望类型，结果一定兼容，不会误报
        val result = SpelTypeResolver(project, context, anchor).resultType(expression) ?: return emptyList()
        if (isCompatible(result, expected)) {
            return emptyList()
        }
        return listOf(
            SpelConfigTypeProblem(
                fragment.textRange,
                "表达式结果类型为 '${result.presentableText}'，" +
                    "与配置字段期望的类型 '${expected.presentableText}' 不兼容"
            )
        )
    }

    /**
     * 标量文本去掉包裹引号与首尾空白后的值范围。
     *
     * 片段范围与标量文本同一坐标（都含起始引号），据此判断片段是否覆盖整个值。
     */
    private fun coversValue(text: String, fragment: SpelFragment): Boolean {
        var start = 0
        var end = text.length
        if (end >= 2 &&
            ((text.first() == '"' && text.last() == '"') ||
                (text.first() == '\'' && text.last() == '\''))
        ) {
            start = 1
            end = text.length - 1
        }
        while (start < end && text[start].isWhitespace()) {
            start++
        }
        while (end > start && text[end - 1].isWhitespace()) {
            end--
        }
        return fragment.textRange.startOffset == start && fragment.textRange.endOffset == end
    }

    private fun isCompatible(result: PsiType, expected: PsiType): Boolean =
        when (categoryOf(expected)) {
            ValueCategory.BOOLEAN -> categoryOf(result) !in NON_BOOLEAN_RESULTS
            ValueCategory.NUMBER -> categoryOf(result) !in NON_NUMBER_RESULTS
            else -> true
        }

    /** 类型归类：只区分会影响转换结果的几类，其余统一为 [ValueCategory.OTHER]。 */
    private enum class ValueCategory {
        BOOLEAN, NUMBER, CHARACTER, MAP, COLLECTION, STRING, ENUM, OTHER
    }

    private fun categoryOf(type: PsiType): ValueCategory = when (type) {
        // 按 canonicalText 判断：声明类型解析出的基本类型与 PsiTypes 常量
        // 不保证是同一实例
        is PsiPrimitiveType -> when (type.canonicalText) {
            "boolean" -> ValueCategory.BOOLEAN
            "char" -> ValueCategory.CHARACTER
            else -> ValueCategory.NUMBER
        }

        is PsiArrayType -> ValueCategory.COLLECTION

        is PsiClassType -> categoryOfClass(type.resolve())

        else -> ValueCategory.OTHER
    }

    private fun categoryOfClass(psiClass: PsiClass?): ValueCategory {
        if (psiClass == null) {
            return ValueCategory.OTHER
        }
        return when {
            psiClass.qualifiedName == JAVA_LANG_BOOLEAN -> ValueCategory.BOOLEAN
            psiClass.qualifiedName == JAVA_LANG_CHARACTER -> ValueCategory.CHARACTER
            isType(psiClass, JAVA_LANG_NUMBER) -> ValueCategory.NUMBER
            isType(psiClass, JAVA_UTIL_MAP) -> ValueCategory.MAP
            isType(psiClass, JAVA_LANG_ITERABLE) -> ValueCategory.COLLECTION
            isType(psiClass, JAVA_LANG_CHAR_SEQUENCE) -> ValueCategory.STRING
            psiClass.isEnum -> ValueCategory.ENUM
            else -> ValueCategory.OTHER
        }
    }

    /** `Map`/`Number` 等本身不满足 `isInheritor`，需要先比较全限定名。 */
    private fun isType(psiClass: PsiClass, fqName: String): Boolean =
        psiClass.qualifiedName == fqName || InheritanceUtil.isInheritor(psiClass, fqName)

    private companion object {
        const val JAVA_LANG_BOOLEAN = "java.lang.Boolean"
        const val JAVA_LANG_CHARACTER = "java.lang.Character"
        const val JAVA_LANG_NUMBER = "java.lang.Number"
        const val JAVA_LANG_ITERABLE = "java.lang.Iterable"
        const val JAVA_LANG_CHAR_SEQUENCE = "java.lang.CharSequence"
        const val JAVA_UTIL_MAP = "java.util.Map"

        /** 布尔字段拿到这些类型时静默变成 `false`。 */
        val NON_BOOLEAN_RESULTS = setOf(
            ValueCategory.NUMBER,
            ValueCategory.CHARACTER,
            ValueCategory.MAP
        )

        /** 数值字段拿到这些类型时转换抛异常。 */
        val NON_NUMBER_RESULTS = setOf(
            ValueCategory.BOOLEAN,
            ValueCategory.CHARACTER,
            ValueCategory.MAP
        )
    }
}
