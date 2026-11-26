package com.luckyframework.idea.spel.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.environment.SpringEnvironmentService
import com.luckyframework.idea.spel.inspection.quickfix.SpelCreateVariableFix
import com.luckyframework.idea.spel.inspection.quickfix.SpelImportFunctionClassFix
import com.luckyframework.idea.spel.inspection.quickfix.SpelReferenceFormFix
import com.luckyframework.idea.spel.inspection.quickfix.SpelSuppressFix
import com.luckyframework.idea.spel.inspection.quickfix.SpelUseFunctionAliasFix
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelInjection
import com.luckyframework.idea.spel.metadata.SpelInjectionScope
import com.luckyframework.idea.spel.metadata.SpelValueKind
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.reference.SpelNavigation
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelContextBuilder
import com.luckyframework.idea.spel.symbol.SpelSymbolKind

/**
 * 未解析变量与函数检查（12.1，specs spel-diagnostics-quickfix）。
 *
 * 只在可确定上下文（注解属性已登记）中报告，默认 warning，可配置
 * 严重级别或完全关闭；每条结果附带「创建变量声明」/「导入函数类」
 * 与「抑制此提示」快速修复。
 *
 * 符号存在但书写形式不对时（`#param` 引用 Root 变量、裸 `varName`
 * 引用普通变量、`#fn(...)` 调用内置函数或用命名空间函数）不再给出创建/导入类修复，
 * 而是提示正确形式并提供「调整 SpEL 引用形式」修复。
 *
 * `@If` 条件导入的符号分三种情况：condition 静态为 `false` 时类未被导入，
 * 引用其函数/命名空间直接报错；condition 无法静态判定时按已导入处理，
 * 但只给出警告；条件为 `true` 时与普通导入一致。
 *
 * 函数名属性（`kind = FUNCTION`）整段就是运行期要查找的函数名，未解析即
 * 必然失败，固定按错误报告；同名方法带 `@FunctionAlias` 时只登记别名，
 * 按方法名书写不会生效，提示改用别名并过滤掉误导性的导入修复。
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
        val resolver = SpelAnnotationResolver()
        // 参数注入变量名（@Rar/@Var/@Bean/@Env）：整段是一个引用名，按作用域查符号
        val injection = if (literal.text.contains("#{")) {
            null
        } else {
            resolver.resolveInjection(literal)
                ?.takeIf { it.kind == SpelValueKind.PARAMETER_INJECTION_VAR }
        }
        if (injection != null) {
            inspectInjectionName(holder, literal, injection)
            return
        }
        // 函数名属性（`kind = FUNCTION`）整段就是函数名，同样参与未解析检查
        val functionNameLiteral = resolver.isFunctionNameLiteral(literal)
        if (!literal.text.contains("#{") && !functionNameLiteral) {
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
                reportUncertainCondition(holder, literal, usage)
                continue
            }
            report(holder, literal, usage, context, functionNameLiteral)
        }
    }

    /**
     * 参数注入变量名（`@Rar`/`@Var`/`@Bean`/`@Env`）的未解析检查。
     *
     * 名字来自注解值；value 未书写时运行期回退参数名，源码没有可检查的文本。
     * 按作用域在对应符号域查找，未命中才报告。变量作用域的符号来自**调用方**
     * 方法的上下文：找不到调用方时注入源未知（函数仅在外部被调用），
     * 无法静态判断，不做未解析检查。
     */
    private fun inspectInjectionName(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        injection: SpelInjection
    ) {
        if (!injection.explicitName) {
            return
        }
        if (!LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)) {
            return
        }
        val scope = injection.scope
        val name = injection.referenceName
        val range = injectionNameRange(literal) ?: return
        val resolved = when (scope) {
            SpelInjectionScope.ROOT_VAR -> {
                val context = contextBuilder.build(literal)
                if (!context.known || context.injectionSourceUnknown) {
                    return
                }
                context.rootVariables(name).isNotEmpty()
            }

            SpelInjectionScope.VAR -> {
                val context = contextBuilder.build(literal)
                if (!context.known || context.injectionSourceUnknown) {
                    return
                }
                context.variables(name).isNotEmpty()
            }

            SpelInjectionScope.BEAN -> {
                val context = contextBuilder.build(literal)
                !context.known ||
                    SpelNavigation(literal.project, context, anchor = literal).beans(name).isNotEmpty()
            }

            SpelInjectionScope.ENV -> {
                // `key:default` 语法：键缺失时运行期回退默认值，不算未定义
                if (name.contains(':')) {
                    return
                }
                val service = SpringEnvironmentService.getInstance(literal.project)
                if (!service.isEnabledFor(literal)) {
                    return
                }
                val model = service.modelFor(literal) ?: return
                model.resolve(name) != null || model.definitionsOf(name).isNotEmpty()
            }

            SpelInjectionScope.EXPRESSION -> return
        }
        if (resolved) {
            return
        }
        holder.registerProblem(
            literal,
            "${labelOfScope(scope)} '$name'",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            range,
            SpelSuppressFix(shortName)
        )
    }

    private fun labelOfScope(scope: SpelInjectionScope): String = when (scope) {
        SpelInjectionScope.ROOT_VAR -> "未解析的 Root 变量"
        SpelInjectionScope.VAR -> "未解析的 SpEL 变量"
        SpelInjectionScope.BEAN -> "未解析的 Bean"
        SpelInjectionScope.ENV -> "未定义的环境配置键"
        SpelInjectionScope.EXPRESSION -> "未解析的表达式"
    }

    /** 注入名的源码范围：引号之间的整段文本；`@Env` 的 `key:default` 只覆盖 key。 */
    private fun injectionNameRange(literal: PsiLiteralExpression): TextRange? {
        val text = literal.text
        if (text.length <= 2) {
            return null
        }
        val end = text.indexOf(':', 1).takeIf { it > 0 } ?: (text.length - 1)
        return if (end <= 1) null else TextRange(1, end)
    }

    /**
     * condition 无法静态判定的条件导入：符号按已导入参与补全与解析，
     * 但运行期可能不存在，这里降级为警告，不按未解析报错。
     */
    private fun reportUncertainCondition(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        usage: SpelUsage
    ) {
        // 同名符号只要有确定可用的来源，就不需要提醒
        if (usage.symbols.any { !it.conditional }) {
            return
        }
        holder.registerProblem(
            literal,
            uncertainConditionMessageOf(usage),
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            usage.range,
            SpelSuppressFix(shortName)
        )
    }

    private fun uncertainConditionMessageOf(usage: SpelUsage): String =
        "${labelOf(usage)} '${usage.name}' 来自条件导入（@If），condition 无法静态确定，运行期可能不存在"

    private fun report(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        usage: SpelUsage,
        context: SpelContext,
        functionNameLiteral: Boolean
    ) {
        // 条件静态为 false：类未被导入，引用其符号是确定错误
        if (usage.alternativeSymbols.isEmpty() && isDisabled(context, usage)) {
            reportDisabled(holder, literal, usage)
            return
        }
        val fixes = ArrayList<LocalQuickFix>()
        val aliasHints = if (functionNameLiteral) {
            functionAliasesOf(literal.project, usage.symbolName)
        } else {
            emptyList()
        }
        when {
            // 符号存在但书写形式不对：只提供「调整引用形式」修复
            usage.alternativeSymbols.isNotEmpty() -> fixes += referenceFormFix(usage)

            usage.kind == SpelTokenType.VARIABLE || usage.kind == SpelTokenType.ROOT_VARIABLE -> {
                // `name::type` 是参数专用写法，创建普通变量声明无法满足
                if (!usage.symbolName.contains("::")) {
                    fixes += SpelCreateVariableFix(usage.symbolName, false)
                    fixes += SpelCreateVariableFix(usage.symbolName, true)
                }
            }

            usage.kind == SpelTokenType.FUNCTION -> {
                aliasHints.forEach { fixes += SpelUseFunctionAliasFix(usage.symbolName, it) }
                fixes += functionImportFixes(literal, usage.symbolName)
            }
            else -> Unit
        }
        // 引用形式错误是硬性约定，不提供抑制选项，只引导改为正确写法
        if (usage.alternativeSymbols.isEmpty()) {
            fixes += SpelSuppressFix(shortName)
        }
        holder.registerProblem(
            literal,
            messageOf(usage, aliasHints.firstOrNull()),
            highlightTypeOf(usage, functionNameLiteral),
            usage.range,
            *fixes.toTypedArray()
        )
    }

    /**
     * 静态 `false` 条件导入未生效的类：运行期一定不存在，
     * 直接按错误报出，而不是泛泛的「未解析」。
     */
    private fun reportDisabled(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        usage: SpelUsage
    ) {
        val label = labelOf(usage)
        holder.registerProblem(
            literal,
            "$label '${usage.name}' 来自条件导入（@If），condition 求值为 false，类未被导入，该${label}不可用",
            ProblemHighlightType.ERROR,
            usage.range
        )
    }

    private fun isDisabled(context: SpelContext, usage: SpelUsage): Boolean {
        val kind = symbolKindOf(usage.kind) ?: return false
        return context.disabled(usage.symbolName, kind) != null
    }

    private fun symbolKindOf(type: SpelTokenType): SpelSymbolKind? = when (type) {
        SpelTokenType.VARIABLE -> SpelSymbolKind.VARIABLE
        SpelTokenType.ROOT_VARIABLE -> SpelSymbolKind.ROOT_VARIABLE
        SpelTokenType.FUNCTION -> SpelSymbolKind.FUNCTION
        SpelTokenType.NAMESPACE -> SpelSymbolKind.NAMESPACE
        else -> null
    }

    private fun labelOf(usage: SpelUsage): String = when (usage.kind) {
        SpelTokenType.FUNCTION -> "函数"
        SpelTokenType.NAMESPACE -> "命名空间"
        else -> "符号"
    }

    /**
     * 书写形式错误直接按错误报出：符号存在但写法不对，运行期一定解析失败；
     * 真正不存在的符号仍跟随检查级别（默认 warning）。
     */
    private fun highlightTypeOf(usage: SpelUsage, functionNameLiteral: Boolean): ProblemHighlightType = when {
        usage.alternativeSymbols.isNotEmpty() -> ProblemHighlightType.ERROR
        functionNameLiteral && usage.kind == SpelTokenType.FUNCTION -> ProblemHighlightType.ERROR
        else -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
    }

    /** 变量/函数存在时按当前写法与正确形式的差异调整引用。 */
    private fun referenceFormFix(usage: SpelUsage): LocalQuickFix {
        val namespace = usage.alternativeNamespace
        if (namespace != null) {
            return SpelReferenceFormFix.QualifyNamespace(namespace, usage.symbolName)
        }
        return if (usage.name.startsWith("#")) {
            SpelReferenceFormFix.RemoveHash()
        } else {
            SpelReferenceFormFix.AddHash()
        }
    }

    private fun messageOf(usage: SpelUsage, aliasHint: String?): String = when {
        // `#sha(...)` 对应的是 `#crypto.sha(...)`，不是去 `#`
        usage.kind == SpelTokenType.FUNCTION && usage.alternativeNamespace != null -> {
            val namespace = usage.alternativeNamespace
            "函数 '${usage.symbolName}' 属于命名空间 '#$namespace'，" +
                "只能以 #$namespace.${usage.symbolName}(...) 形式调用"
        }

        usage.kind == SpelTokenType.FUNCTION && usage.alternativeSymbols.isNotEmpty() ->
            "内置函数 '${usage.symbolName}' 只能以 ${usage.symbolName}(...) 形式调用，不支持 #${usage.symbolName}(...)"

        usage.alternativeSymbols.isNotEmpty() -> if (usage.name.startsWith("#")) {
            "Root 变量 '${usage.symbolName}' 只能用裸名称引用，请写成 ${usage.symbolName}"
        } else {
            "变量 '${usage.symbolName}' 必须使用 # 引用，请写成 #${usage.symbolName}"
        }

        usage.kind == SpelTokenType.FUNCTION && aliasHint != null ->
            "未解析的 SpEL 函数 '${usage.name}'：同名方法只登记了别名，请改用 '$aliasHint'"

        usage.kind == SpelTokenType.FUNCTION -> "未解析的 SpEL 函数 '${usage.name}'"
        usage.kind == SpelTokenType.NAMESPACE -> "未解析的 SpEL 命名空间 '${usage.name}'"
        else -> "未解析的 SpEL 变量 '${usage.name}'"
    }

    /** 按函数名查找 public static 方法所在类，每个类生成一个导入修复。 */
    private fun functionImportFixes(literal: PsiLiteralExpression, functionName: String): List<LocalQuickFix> {
        return functionsNamed(literal.project, functionName)
            // 带 @FunctionAlias 的方法只登记别名，按方法名导入类也不会解析到它
            .filter { aliasOf(it).let { alias -> alias.isNullOrEmpty() || alias == functionName } }
            .mapNotNull { it.containingClass }
            .distinctBy { it.qualifiedName }
            .take(MAX_FUNCTION_CLASS_CANDIDATES)
            .map { SpelImportFunctionClassFix(functionName, it) }
            .toList()
    }

    /** 同名可导入函数声明：public static、非构造器。 */
    private fun functionsNamed(project: Project, functionName: String): List<PsiMethod> {
        if (functionName.isBlank()) {
            return emptyList()
        }
        val methods: Array<PsiMethod> = runCatching {
            PsiShortNamesCache.getInstance(project)
                .getMethodsByName(functionName, GlobalSearchScope.allScope(project))
        }.getOrElse { PsiMethod.EMPTY_ARRAY }
        return methods.asSequence()
            .filter { !it.isConstructor }
            .filter { it.hasModifierProperty(PsiModifier.PUBLIC) && it.hasModifierProperty(PsiModifier.STATIC) }
            .toList()
    }

    /** 同名方法声明的别名；与方法名相同的别名不改变引用方式，不算提示。 */
    private fun functionAliasesOf(project: Project, functionName: String): List<String> =
        functionsNamed(project, functionName)
            .mapNotNull { aliasOf(it) }
            .filter { it.isNotBlank() && it != functionName }
            .distinct()

    private fun aliasOf(method: PsiMethod): String? =
        (method.getAnnotation(FUNCTION_ALIAS)?.findAttributeValue("value") as? PsiLiteralExpression)
            ?.value as? String

    private companion object {
        private const val MAX_FUNCTION_CLASS_CANDIDATES = 5

        private const val FUNCTION_ALIAS = "com.luckyframework.httpclient.proxy.spel.FunctionAlias"
    }
}
