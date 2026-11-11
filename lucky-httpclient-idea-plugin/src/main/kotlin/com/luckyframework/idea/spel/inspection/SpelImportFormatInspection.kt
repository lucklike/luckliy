package com.luckyframework.idea.spel.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.util.PsiTreeUtil
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.symbol.BuiltinSymbolsLoader
import com.luckyframework.idea.spel.symbol.SpelImportDeclaration
import com.luckyframework.idea.spel.symbol.SpelImportParser
import com.luckyframework.idea.spel.symbol.SpelImportProblem
import com.luckyframework.idea.spel.symbol.SpelSymbolKind
import com.luckyframework.idea.spel.symbol.SpelSymbolOrigin

/**
 * `@SpELImport` 声明格式检查（12.2，specs spel-diagnostics-quickfix）。
 *
 * - 缺少分隔符、空变量名、同一作用域重复声明 → error；
 * - 覆盖框架内置变量 → warning。
 *
 * 只检查源码中实际书写的注解；元注解上的声明由被注解元素解析时生效，
 * 不重复报告。
 */
class SpelImportFormatInspection : LocalInspectionTool() {

    private val importParser = SpelImportParser()

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {
            override fun visitAnnotation(annotation: PsiAnnotation) {
                if (annotation.qualifiedName != SpelImportParser.SPEL_IMPORT) {
                    return
                }
                val owner = PsiTreeUtil.getParentOfType(
                    annotation, PsiModifierListOwner::class.java, true
                ) ?: return
                if (!LuckyHttpClientPresence.getInstance(annotation.project).isEnabledFor(annotation)) {
                    return
                }
                inspect(holder, owner, annotation)
            }
        }

    override fun isSuppressedFor(element: PsiElement): Boolean =
        super.isSuppressedFor(element) || SpelSuppression.isSuppressed(element, shortName)

    private fun inspect(holder: ProblemsHolder, owner: PsiModifierListOwner, annotation: PsiAnnotation) {
        val seen = HashSet<Pair<SpelSymbolKind, String>>()
        for (current in importParser.findImportAnnotations(owner)) {
            for (declaration in importParser.parse(current, SpelSymbolOrigin.UNKNOWN)) {
                val name = declaration.name?.takeIf { it.isNotBlank() }
                if (current != annotation) {
                    if (name != null) {
                        seen += declaration.kind to name
                    }
                    continue
                }
                if (reportFormatProblem(holder, annotation, declaration)) {
                    continue
                }
                if (name == null) {
                    continue
                }
                if (!seen.add(declaration.kind to name)) {
                    report(
                        holder,
                        annotation,
                        declaration,
                        "重复声明变量 '$name'",
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
                if (name in builtinNames()) {
                    report(
                        holder,
                        annotation,
                        declaration,
                        "覆盖内置变量 '$name'",
                        ProblemHighlightType.WARNING
                    )
                }
            }
        }
    }

    /** 报告结构化问题；已报告时返回 true。 */
    private fun reportFormatProblem(
        holder: ProblemsHolder,
        annotation: PsiAnnotation,
        declaration: SpelImportDeclaration
    ): Boolean {
        val message = when (declaration.problem) {
            SpelImportProblem.MISSING_SEPARATOR -> "缺少分隔符，应为 name=value"
            SpelImportProblem.EMPTY_NAME -> "变量名不能为空"
            null -> return false
        }
        report(holder, annotation, declaration, message, ProblemHighlightType.GENERIC_ERROR)
        return true
    }

    private fun report(
        holder: ProblemsHolder,
        annotation: PsiAnnotation,
        declaration: SpelImportDeclaration,
        message: String,
        highlightType: ProblemHighlightType
    ) {
        holder.registerProblem(annotation, message, highlightType, rangeOf(annotation, declaration))
    }

    private fun rangeOf(annotation: PsiAnnotation, declaration: SpelImportDeclaration): TextRange {
        val base = declaration.literal.textRange.startOffset - annotation.textRange.startOffset
        val name = declaration.nameRange
        if (name == null || name.length <= 0) {
            return TextRange(base, base + declaration.literal.textLength)
        }
        return TextRange(base + name.startOffset, base + name.endOffset)
    }

    private fun builtinNames(): Set<String> = BUILTIN_NAMES

    private companion object {
        val BUILTIN_NAMES: Set<String> by lazy {
            val table = BuiltinSymbolsLoader.load()
            (table.rootVariables + table.variables).mapTo(HashSet()) { it.name }
        }
    }
}
