package com.luckyframework.idea.spel.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.TypeConversionUtil
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.inspection.quickfix.SpelSuppressFix
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelInjection
import com.luckyframework.idea.spel.metadata.SpelInjectionScope
import com.luckyframework.idea.spel.metadata.SpelValueKind
import com.luckyframework.idea.spel.reference.SpelNavigation
import com.luckyframework.idea.spel.reference.SpelReferenceProvider
import com.luckyframework.idea.spel.symbol.SpelSymbol

/**
 * static 方法参数注入的类型兼容检查。
 *
 * `@Rar`/`@Var` 注入的变量与 `@Bean` 注入的 Bean 必须能赋值给参数声明的
 * 类型，否则运行期注入必然失败。注入表达式在**调用方**方法的上下文求值
 * （`@Post(func = "...")` 所在方法），因此目标类型取自调用方符号；
 * 多个调用方时任一类型兼容即通过（同一个函数会被多个方法调用）。
 * 只在引用目标能确定类型时检查：
 *
 * - `@Env` 的值是配置字符串，参数转换由运行期完成，不检查；
 * - `@Param` 是 SpEL 表达式，结果类型无法静态推导，不检查；
 * - 符号没有可解析的类型（内置变量、动态注册）时跳过，不误报；
 * - `Object` 参数接收任意值，不检查。
 *
 * 注入源与目标类型确定不兼容时按 ERROR 级别报错：运行期必然注入失败，
 * 不是可忽略的提示。
 */
class SpelInjectionTypeInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {
            override fun visitLiteralExpression(expression: PsiLiteralExpression) {
                inspect(holder, expression)
            }
        }

    override fun isSuppressedFor(element: PsiElement): Boolean =
        super.isSuppressedFor(element) || SpelSuppression.isSuppressed(element, shortName)

    private fun inspect(holder: ProblemsHolder, literal: PsiLiteralExpression) {
        if (literal.text.contains("#{")) {
            return
        }
        val injection = SpelAnnotationResolver().resolveInjection(literal) ?: return
        // 只有显式书写的引用名才有可标记的源码文本；回退参数名时无从报告
        if (injection.kind != SpelValueKind.PARAMETER_INJECTION_VAR || !injection.explicitName) {
            return
        }
        if (!LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)) {
            return
        }
        val parameterType = injection.parameterType ?: return
        if (parameterType.canonicalText == JAVA_LANG_OBJECT) {
            return
        }
        val targetTypes = targetTypesOf(literal, injection)
        if (targetTypes.isEmpty()) {
            return
        }
        if (targetTypes.any { isCompatible(parameterType, it) }) {
            return
        }
        val range = injectionNameRange(literal) ?: return
        val targetType = targetTypes.first()
        holder.registerProblem(
            literal,
            "参数注入类型不匹配：'${injection.referenceName}' 是 ${targetType.presentableText}，" +
                "无法注入到 ${parameterType.presentableText} 类型的参数 '${injection.parameter.name}'",
            ProblemHighlightType.ERROR,
            range,
            SpelSuppressFix(shortName)
        )
    }

    /**
     * 引用目标候选类型：变量取调用方符号的推导类型或声明类型，Bean 取声明类类型。
     *
     * 多个调用方时同名符号可能对应多个声明类型，任一个可赋值即视为兼容；
     * 上下文未知或没有任何可推导类型时返回空（跳过检查）。
     * 未书写名字时运行期注入的变量名等于参数名，目标往往就是参数自身，
     * 类型必然一致，因此只看显式名字（调用方已保证）。
     */
    private fun targetTypesOf(literal: PsiLiteralExpression, injection: SpelInjection): List<PsiType> =
        when (injection.scope) {
            SpelInjectionScope.ROOT_VAR, SpelInjectionScope.VAR -> {
                val context = SpelReferenceProvider.contextOf(literal)
                if (!context.known) {
                    emptyList()
                } else {
                    val symbols = if (injection.scope == SpelInjectionScope.ROOT_VAR) {
                        context.rootVariables(injection.referenceName)
                    } else {
                        context.variables(injection.referenceName)
                    }
                    symbols.mapNotNull { symbolType(it) }
                }
            }

            SpelInjectionScope.BEAN -> {
                val context = SpelReferenceProvider.contextOf(literal)
                if (!context.known) {
                    emptyList()
                } else {
                    SpelNavigation(literal.project, context, anchor = literal)
                        .beans(injection.referenceName)
                        .mapNotNull { it.target as? PsiClass }
                        .map { JavaPsiFacade.getElementFactory(literal.project).createType(it) }
                }
            }

            SpelInjectionScope.ENV, SpelInjectionScope.EXPRESSION -> emptyList()
        }

    /** 符号类型：上下文推导的精确类型优先，其次声明元素自身的类型。 */
    private fun symbolType(symbol: SpelSymbol): PsiType? =
        symbol.resolvedType ?: (symbol.declaration as? PsiVariable)?.type

    /** 兼容判断：按 Java 可赋值转换判断，基本类型与包装类型之间的装箱/拆箱都已覆盖。 */
    private fun isCompatible(parameter: PsiType, target: PsiType): Boolean =
        TypeConversionUtil.isAssignable(parameter, target)

    /** 注入名的源码范围：引号之间的整段文本；`@Env` 的 `key:default` 只覆盖 key。 */
    private fun injectionNameRange(literal: PsiLiteralExpression): TextRange? {
        val text = literal.text
        if (text.length <= 2) {
            return null
        }
        val end = text.indexOf(':', 1).takeIf { it > 0 } ?: (text.length - 1)
        return if (end <= 1) null else TextRange(1, end)
    }

    private companion object {
        const val JAVA_LANG_OBJECT = "java.lang.Object"
    }
}
