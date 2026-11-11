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
 * - 未闭合表达式、未闭合字符串、括号不配平以 ERROR 标记呈现；
 * - 只处理包含 `#{` 的字符串字面量，普通字符串不进入词法分析。
 */
class SpelAnnotator : Annotator {

    private val scanner = SpelScanner()

    private val highlighter = SpelFragmentHighlighter()

    private val importParser = SpelImportParser()

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element !is PsiLiteralExpression) {
            return
        }
        val text = element.text
        val parent = element.parent
        // `@SpELImport(var = "name=...")` 的声明值可能不含 `#{`，需单独识别
        val importCandidate = parent is PsiNameValuePair || parent is PsiArrayInitializerMemberValue
        if (!text.contains("#{") && !importCandidate) {
            return
        }
        if (!LuckyHttpClientPresence.getInstance(element.project).isEnabledFor(element)) {
            return
        }

        if (importCandidate) {
            annotateImportDeclaration(element, holder)
        }
        if (!text.contains("#{")) {
            return
        }

        highlighter.annotate(
            holder,
            text,
            element.textRange.startOffset,
            scanner.scanLiteral(text)
        )
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
