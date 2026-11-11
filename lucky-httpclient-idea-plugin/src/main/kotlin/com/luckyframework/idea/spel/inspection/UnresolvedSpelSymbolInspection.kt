package com.luckyframework.idea.spel.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.inspection.quickfix.SpelCreateVariableFix
import com.luckyframework.idea.spel.inspection.quickfix.SpelImportFunctionClassFix
import com.luckyframework.idea.spel.inspection.quickfix.SpelSuppressFix
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.symbol.SpelContextBuilder

/**
 * 未解析变量与函数检查（12.1，specs spel-diagnostics-quickfix）。
 *
 * 只在可确定上下文（注解属性已登记）中报告，默认 warning，可配置
 * 严重级别或完全关闭；每条结果附带「创建变量声明」/「导入函数类」
 * 与「抑制此提示」快速修复。
 */
class UnresolvedSpelSymbolInspection : LocalInspectionTool() {

    private val contextBuilder = SpelContextBuilder()

    private val usageScanner = SpelUsageScanner()

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {
            override fun visitLiteralExpression(expression: PsiLiteralExpression) {
                inspect(holder, expression)
            }
        }

    override fun isSuppressedFor(element: PsiElement): Boolean =
        super.isSuppressedFor(element) || SpelSuppression.isSuppressed(element, shortName)

    private fun inspect(holder: ProblemsHolder, literal: PsiLiteralExpression) {
        if (!literal.text.contains("#{")) {
            return
        }
        if (!LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)) {
            return
        }
        // 未知上下文不做未解析检查，避免把动态注册的变量误报为错误
        val context = contextBuilder.build(literal)
        if (!context.known) {
            return
        }
        for (usage in usageScanner.scan(literal, context)) {
            if (usage.symbols.isNotEmpty()) {
                continue
            }
            report(holder, literal, usage)
        }
    }

    private fun report(holder: ProblemsHolder, literal: PsiLiteralExpression, usage: SpelUsage) {
        val fixes = ArrayList<LocalQuickFix>()
        when (usage.kind) {
            SpelTokenType.VARIABLE, SpelTokenType.ROOT_VARIABLE -> {
                // `name::type` 是参数专用写法，创建普通变量声明无法满足
                if (!usage.symbolName.contains("::")) {
                    fixes += SpelCreateVariableFix(usage.symbolName, false)
                    fixes += SpelCreateVariableFix(usage.symbolName, true)
                }
            }

            SpelTokenType.FUNCTION -> fixes += functionImportFixes(literal, usage.symbolName)
            else -> Unit
        }
        fixes += SpelSuppressFix(shortName)
        holder.registerProblem(
            literal,
            messageOf(usage),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            usage.range,
            *fixes.toTypedArray()
        )
    }

    private fun messageOf(usage: SpelUsage): String = when (usage.kind) {
        SpelTokenType.FUNCTION -> "未解析的 SpEL 函数 '${usage.name}'"
        SpelTokenType.NAMESPACE -> "未解析的 SpEL 命名空间 '${usage.name}'"
        else -> "未解析的 SpEL 变量 '${usage.name}'"
    }

    /** 按函数名查找 public static 方法所在类，每个类生成一个导入修复。 */
    private fun functionImportFixes(literal: PsiLiteralExpression, functionName: String): List<LocalQuickFix> {
        if (functionName.isBlank()) {
            return emptyList()
        }
        val project = literal.project
        val methods: Array<PsiMethod> = runCatching {
            PsiShortNamesCache.getInstance(project)
                .getMethodsByName(functionName, GlobalSearchScope.allScope(project))
        }.getOrElse { PsiMethod.EMPTY_ARRAY }
        return methods.asSequence()
            .filter { !it.isConstructor }
            .filter { it.hasModifierProperty(PsiModifier.PUBLIC) && it.hasModifierProperty(PsiModifier.STATIC) }
            .mapNotNull { it.containingClass }
            .distinctBy { it.qualifiedName }
            .take(MAX_FUNCTION_CLASS_CANDIDATES)
            .map { SpelImportFunctionClassFix(functionName, it) }
            .toList()
    }

    private companion object {
        private const val MAX_FUNCTION_CLASS_CANDIDATES = 5
    }
}
