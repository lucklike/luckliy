package com.luckyframework.idea.spel.highlight

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.symbol.SpelImportParser
import com.luckyframework.idea.spel.symbol.SpelSymbolKind
import com.luckyframework.idea.spel.symbol.SpelSymbolOrigin

/**
 * SpEL 片段高亮与基础语法错误（design D8 / specs spel-expression-highlighting）。
 *
 * - 按 token 类型分别为变量、函数、属性、Root 变量、类型、Bean、字符串、数字、
 *   运算符、关键字与括号着色；
 * - `#{`、`}` 定界符单独着色；
 * - `@SpELImport(var = "name=...")` 的声明名按变量色着色；
 * - 未闭合表达式、未闭合字符串、括号不配平等以 ERROR 标记呈现；
 * - 含 `#{` 的字符串字面量按 token 着色，其余字符串不进入词法分析；
 * - `kind = FUNCTION` 的属性整段文本就是函数名（不含 `#{`），按函数色整段着色；
 * - 只有显式标注 `@SpelAttribute` 的属性才参与高亮：未标注（如自定义注解
 *   `@DemoLiteral#value`）与 `kind = LITERAL/NONE/UNKNOWN` 的属性运行期不求值，
 *   不做任何高亮与语法校验。
 */
class SpelAnnotator : Annotator {

    private val scanner = SpelScanner()

    private val highlighter = SpelFragmentHighlighter()

    private val importParser = SpelImportParser()

    private val resolver = SpelAnnotationResolver()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiLiteralExpression) {
            return
        }
        val text = element.text
        val parent = element.parent
        // `@SpELImport(var = "name=...")` 的声明值可能不含 `#{`，需单独识别
        val importCandidate = parent is PsiNameValuePair || parent is PsiArrayInitializerMemberValue
        // `kind = FUNCTION` 的属性整段文本就是函数名，同样可能不含 `#{`
        val hasFragment = text.contains("#{")
        val functionName = if (hasFragment) false else resolver.isFunctionNameLiteral(element)
        if (!hasFragment && !importCandidate && !functionName) {
            return
        }
        if (!LuckyHttpClientPresence.getInstance(element.project).isEnabledFor(element)) {
            return
        }
        // 只有显式标注 @SpelAttribute 的属性才参与高亮：未标注的属性
        // （自定义注解如 @DemoLiteral#value）与 LITERAL/NONE/UNKNOWN 一样
        // 运行期不求值，不能产生任何着色或语法错误。
        if (!resolver.isSpelLiteral(element)) {
            return
        }

        if (importCandidate) {
            annotateImportDeclaration(element, holder)
        }
        if (!hasFragment) {
            if (functionName) {
                annotateFunctionName(element, holder)
            }
            return
        }

        highlighter.annotate(
            holder,
            text,
            element.textRange.startOffset,
            scanner.scanLiteral(text)
        )
    }

    /**
     * 函数名字面量：`kind = FUNCTION` 属性里整段文本就是函数名（不含 `#{`），
     * 与 `#{...}` 内的函数 token 同样按函数色着色。
     */
    private fun annotateFunctionName(literal: PsiLiteralExpression, holder: AnnotationHolder) {
        val range = contentRangeOf(literal) ?: return
        val base = literal.textRange.startOffset
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(TextRange(base + range.startOffset, base + range.endOffset))
            .textAttributes(SpelTextAttributes.FUNCTION)
            .create()
    }

    /** 字符串字面量引号之间的范围；空内容返回 null。 */
    private fun contentRangeOf(literal: PsiLiteralExpression): TextRange? {
        val text = literal.text
        if (text.length <= 2) {
            return null
        }
        return TextRange(1, text.length - 1)
    }

    /** `@SpELImport` 的 `root`/`var` 等属性里，`name=value` 的声明名按变量色着色。 */
    private fun annotateImportDeclaration(
        literal: PsiLiteralExpression,
        holder: AnnotationHolder
    ) {
        val pair = PsiTreeUtil.getParentOfType(literal, PsiNameValuePair::class.java)
            ?: return
        if (pair.name !in IMPORT_ATTRIBUTES) {
            return
        }
        val annotation = pair.parent?.parent as? PsiAnnotation ?: return
        if (annotation.qualifiedName != SpelImportParser.SPEL_IMPORT) {
            return
        }
        val base = literal.textRange.startOffset
        for (declaration in importParser.parse(annotation, SpelSymbolOrigin.UNKNOWN)) {
            if (declaration.literal != literal) {
                continue
            }
            val range = declaration.nameRange ?: continue
            if (range.isEmpty) {
                continue
            }
            val attributes = when (declaration.kind) {
                SpelSymbolKind.ROOT_VARIABLE -> SpelTextAttributes.ROOT_VARIABLE
                else -> SpelTextAttributes.VARIABLE
            }
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(TextRange(base + range.startOffset, base + range.endOffset))
                .textAttributes(attributes)
                .create()
        }
    }

    private companion object {
        val IMPORT_ATTRIBUTES = setOf("root", "rootLit", "var", "varLit")
    }
}
