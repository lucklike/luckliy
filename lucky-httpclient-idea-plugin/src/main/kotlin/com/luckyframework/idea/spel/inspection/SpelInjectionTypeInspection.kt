package com.luckyframework.idea.spel.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiVariable
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.TypeConversionUtil
import com.luckyframework.idea.spel.activation.LuckyHttpClientPresence
import com.luckyframework.idea.spel.completion.SpelTypeResolver
import com.luckyframework.idea.spel.environment.SpringEnvironmentService
import com.luckyframework.idea.spel.inspection.quickfix.SpelSuppressFix
import com.luckyframework.idea.spel.metadata.SpelAnnotationResolver
import com.luckyframework.idea.spel.metadata.SpelInjection
import com.luckyframework.idea.spel.metadata.SpelInjectionScope
import com.luckyframework.idea.spel.metadata.SpelValueKind
import com.luckyframework.idea.spel.reference.SpelNavigation
import com.luckyframework.idea.spel.reference.SpelReferenceProvider
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelImportDeclaration
import com.luckyframework.idea.spel.symbol.SpelImportParser
import com.luckyframework.idea.spel.symbol.SpelSymbol
import com.luckyframework.idea.spel.symbol.SpelSymbolOrigin

/**
 * static 方法参数注入的类型兼容检查。
 *
 * 三类注入源分别校验：
 *
 * - `@Rar`/`@Var`：注入的变量必须能赋值给参数声明的类型。变量来自
 *   调用方参数时取其声明类型；来自 `@SpELImport` 时按声明值判断：
 *   `varLit` 的值是字面量（如 `2--206`，永远不能转成 Integer），
 *   `var` 的值按 SpEL 表达式推导结果类型；
 * - `@Param`：整段是 SpEL 模板，`#{...}` 的结果类型必须能注入到参数类型；
 * - `@Env`：配置值必须能转换为参数类型。环境可能在运行期被覆盖，只警告。
 *
 * 目标类型取自**调用方**方法的上下文（`@Post(func = "...")` 所在方法），
 * 注入表达式在调用方上下文中求值；多个调用方时任一个兼容即通过。
 * 只在能确定不兼容时报告，符号/表达式类型无法推导时跳过，不误报：
 *
 * - `Object` 参数接收任意值，不检查；
 * - `@Bean` 只按声明类检查；
 * - 环境键或 SpEL 导入变量找不到声明时跳过。
 *
 * 注入源与目标类型确定不兼容时按 ERROR 级别报错：运行期必然注入失败，
 * 不是可忽略的提示；`@Env` 是例外（环境值可被启动参数覆盖），只给 WARNING。
 */
class SpelInjectionTypeInspection : LocalInspectionTool() {

    /** 解析器无状态：跨字面量复用，避免每个字面量都新建一个。 */
    private val resolver = SpelAnnotationResolver()

    private val importParser = SpelImportParser()

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : JavaElementVisitor() {
            override fun visitLiteralExpression(expression: PsiLiteralExpression) {
                inspect(holder, expression)
            }
        }

    override fun isSuppressedFor(element: PsiElement): Boolean =
        super.isSuppressedFor(element) || SpelSuppression.isSuppressed(element, shortName)

    private fun inspect(holder: ProblemsHolder, literal: PsiLiteralExpression) {
        val injection = resolver.resolveInjection(literal) ?: return
        if (!LuckyHttpClientPresence.getInstance(literal.project).isEnabledFor(literal)) {
            return
        }
        val parameterType = injection.parameterType ?: return
        // Object 参数接收任意值，不参与类型检查
        if (isObjectType(parameterType)) {
            return
        }
        when (injection.kind) {
            SpelValueKind.PARAMETER_INJECTION_VAR ->
                inspectVariableInjection(holder, literal, injection, parameterType)

            SpelValueKind.PARAMETER_INJECTION_EXPRESSION ->
                inspectExpressionInjection(holder, literal, injection, parameterType)

            else -> Unit
        }
    }

    /** `@Var`/`@Rar`/`@Env`/`@Bean`：按作用域选择检查目标。 */
    private fun inspectVariableInjection(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        injection: SpelInjection,
        parameterType: PsiType
    ) {
        // 只有显式书写的引用名才有可标记的源码文本；回退参数名时无从报告
        if (!injection.explicitName) {
            return
        }
        when (injection.scope) {
            SpelInjectionScope.ROOT_VAR, SpelInjectionScope.VAR ->
                inspectVariableValue(holder, literal, injection, parameterType)

            SpelInjectionScope.BEAN ->
                inspectBeanValue(holder, literal, injection, parameterType)

            SpelInjectionScope.ENV ->
                inspectEnvironmentValue(holder, literal, injection, parameterType)

            SpelInjectionScope.EXPRESSION -> Unit
        }
    }

    /**
     * `@Rar`/`@Var` 的注入值：优先用调用方同名符号的声明类型；
     * 符号没有类型（来自 `@SpELImport` 的变量）时按声明值判断。
     */
    private fun inspectVariableValue(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        injection: SpelInjection,
        parameterType: PsiType
    ) {
        val context = SpelReferenceProvider.contextOf(literal)
        if (!context.known) {
            return
        }
        val symbols = if (injection.scope == SpelInjectionScope.ROOT_VAR) {
            context.rootVariables(injection.referenceName)
        } else {
            context.variables(injection.referenceName)
        }
        val targetTypes = symbols.mapNotNull { symbolType(it) }
        if (targetTypes.isNotEmpty()) {
            if (targetTypes.any { isCompatible(parameterType, it) }) {
                return
            }
            reportInjectionType(holder, literal, injection, parameterType, targetTypes.first())
            return
        }
        // @SpELImport 变量没有声明类型：按声明值判断（varLit 是字面量，var 是 SpEL）
        val declarations = importDeclarationsOf(symbols, injection.referenceName)
        if (declarations.isEmpty()) {
            return
        }
        val outcomes = declarations.mapNotNull { importedValueCompatible(context, literal.project, parameterType, it) }
        if (outcomes.isEmpty() || outcomes.any { it }) {
            return
        }
        reportImportedValue(holder, literal, injection, parameterType, declarations.first())
    }

    /** `@Bean`：注入的 Bean 声明类必须能赋值给参数类型。 */
    private fun inspectBeanValue(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        injection: SpelInjection,
        parameterType: PsiType
    ) {
        val context = SpelReferenceProvider.contextOf(literal)
        if (!context.known) {
            return
        }
        val targetTypes = SpelNavigation(literal.project, context, anchor = literal)
            .beans(injection.referenceName)
            .mapNotNull { it.target as? PsiClass }
            .map { JavaPsiFacade.getElementFactory(literal.project).createType(it) }
        if (targetTypes.isEmpty() || targetTypes.any { isCompatible(parameterType, it) }) {
            return
        }
        reportInjectionType(holder, literal, injection, parameterType, targetTypes.first())
    }

    /**
     * `@Env`：配置值必须能转换为参数类型。
     *
     * 环境值可在启动参数/环境变量中被覆盖，静态配置只能作为提示，
     * 因此报告 WARNING 而不是 ERROR；键未配置时没有可判断的值，跳过。
     */
    private fun inspectEnvironmentValue(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        injection: SpelInjection,
        parameterType: PsiType
    ) {
        val service = SpringEnvironmentService.getInstance(literal.project)
        if (!service.isEnabledFor(literal)) {
            return
        }
        val model = service.modelFor(literal) ?: return
        val raw = injection.referenceName
        val key = raw.substringBefore(':')
        // `key:default`：键未配置时用默认值判断
        val value = model.resolveValue(key)
            ?: raw.substringAfter(':', "").takeIf { ':' in raw }
            ?: return
        if (literalAccepts(parameterType, value) != false) {
            return
        }
        holder.registerProblem(
            literal,
            "参数注入类型不匹配：环境配置 '$key' 的值 '$value' 无法注入到 " +
                "${parameterType.presentableText} 类型的参数 '${injection.parameter.name}'",
            ProblemHighlightType.WARNING,
            injectionNameRange(literal),
            SpelSuppressFix(shortName)
        )
    }

    /** `@Param`：整段是 SpEL 模板，单个 `#{...}` 片段的结果类型必须能注入。 */
    private fun inspectExpressionInjection(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        injection: SpelInjection,
        parameterType: PsiType
    ) {
        val text = (literal.value as? String)?.trim() ?: return
        // 只检查整段就是单个 #{...} 的情况：多片段模板的结果是字符串
        val expression = singleSpelFragment(text) ?: return
        val context = SpelReferenceProvider.contextOf(literal)
        if (!context.known) {
            return
        }
        val resultType = expressionResultType(context, literal.project, expression) ?: return
        if (expressionCompatible(context, literal.project, parameterType, expression) != false) {
            return
        }
        holder.registerProblem(
            literal,
            "参数注入类型不匹配：表达式结果类型是 ${resultType.presentableText}，无法注入到 " +
                "${parameterType.presentableText} 类型的参数 '${injection.parameter.name}'",
            ProblemHighlightType.ERROR,
            TextRange(1, (literal.text.length - 1).coerceAtLeast(1)),
            SpelSuppressFix(shortName)
        )
    }

    /** 报告「来源类型 → 参数类型」不匹配。 */
    private fun reportInjectionType(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        injection: SpelInjection,
        parameterType: PsiType,
        targetType: PsiType
    ) {
        val range = injectionNameRange(literal) ?: return
        holder.registerProblem(
            literal,
            "参数注入类型不匹配：'${injection.referenceName}' 是 ${targetType.presentableText}，" +
                "无法注入到 ${parameterType.presentableText} 类型的参数 '${injection.parameter.name}'",
            ProblemHighlightType.ERROR,
            range,
            SpelSuppressFix(shortName)
        )
    }

    /** 报告 `@SpELImport` 变量声明的值无法注入。 */
    private fun reportImportedValue(
        holder: ProblemsHolder,
        literal: PsiLiteralExpression,
        injection: SpelInjection,
        parameterType: PsiType,
        declaration: SpelImportDeclaration
    ) {
        val range = injectionNameRange(literal) ?: return
        holder.registerProblem(
            literal,
            "参数注入类型不匹配：变量 '${injection.referenceName}' 的值 '${declaration.value}' " +
                "无法注入到 ${parameterType.presentableText} 类型的参数 '${injection.parameter.name}'",
            ProblemHighlightType.ERROR,
            range,
            SpelSuppressFix(shortName)
        )
    }

    /** 符号类型：上下文推导的精确类型优先，其次声明元素自身的类型。 */
    private fun symbolType(symbol: SpelSymbol): PsiType? =
        symbol.resolvedType ?: (symbol.declaration as? PsiVariable)?.type

    /** 兼容判断：按 Java 可赋值转换判断，基本类型与包装类型之间的装箱/拆箱都已覆盖。 */
    private fun isCompatible(parameter: PsiType, target: PsiType): Boolean =
        TypeConversionUtil.isAssignable(parameter, target)

    /**
     * 符号对应的 `@SpELImport` 变量声明。
     *
     * `@SpELImport` 生成的符号没有声明类型（declaration 是注解里的字面量），
     * 类型检查需要回到注解上读取原始声明：`var` 的值按 SpEL 求值，
     * `varLit` 的值是原始字面量，两者判断方式不同。
     */
    private fun importDeclarationsOf(symbols: List<SpelSymbol>, name: String): List<SpelImportDeclaration> =
        symbols.mapNotNull { symbol ->
            val literal = symbol.declaration as? PsiLiteralExpression ?: return@mapNotNull null
            val annotation = PsiTreeUtil.getParentOfType(literal, PsiAnnotation::class.java, false)
                ?: return@mapNotNull null
            if (annotation.qualifiedName != SpelImportParser.SPEL_IMPORT) {
                return@mapNotNull null
            }
            importParser.parse(annotation, SpelSymbolOrigin.CLASS_IMPORT)
                .firstOrNull { it.literal == literal && it.name == name }
        }.distinct()

    /**
     * `@SpELImport` 声明值能否注入到参数类型。
     *
     * `varLit`/`rootLit` 的值是字面量字符串（运行期不做解析），按文本判断；
     * `var`/`root` 的值按 SpEL 求值，推导结果类型后再判断。
     * 返回 null 表示无法静态判断。
     */
    private fun importedValueCompatible(
        context: SpelContext,
        project: Project,
        parameterType: PsiType,
        declaration: SpelImportDeclaration
    ): Boolean? {
        if (!declaration.valueIsSpel) {
            return literalAccepts(parameterType, declaration.value)
        }
        val value = declaration.value
        val expression = singleSpelFragment(value)
            ?: value.takeUnless { it.contains("#{") || it.contains("\${") }
            ?: return null
        return expressionCompatible(context, project, parameterType, expression)
    }

    /** 表达式结果类型；只用于报告消息。 */
    private fun expressionResultType(context: SpelContext, project: Project, expression: String): PsiType? =
        SpelTypeResolver(project, context).resolvePsiType(expression)

    /** 表达式结果能否注入到参数类型；null 表示无法静态判断。 */
    private fun expressionCompatible(
        context: SpelContext,
        project: Project,
        parameterType: PsiType,
        expression: String
    ): Boolean? {
        val expr = expression.trim()
        if (expr.isEmpty()) {
            return null
        }
        // 字符串字面量的值已知：直接按内容判断能否转换
        stringLiteralContent(expr)?.let { return literalAccepts(parameterType, it) }
        val result = SpelTypeResolver(project, context).resolvePsiType(expr) ?: return null
        return resultCompatible(parameterType, result)
    }

    /**
     * 表达式结果类型与参数类型是否兼容。
     *
     * 先按 `PsiType` 结构判断（数组/非数组无法互转），再跟进数值/字符串
     * 等标量转换规则；标量级启发式只看去掉泛型与包名的短名。
     *
     * 运行期会把 SpEL 结果转换成参数类型：目标 String 任意结果都能转，
     * 数值之间按数值转换；String 结果无法静态确定内容，对数值/布尔/字符
     * 目标按确定不兼容处理（`md5(...)` 返回 `byte[]` 注入 Integer 这类场景
     * 必须提示）。数组与标量之间没有隐式转换，不能因为短名剥掉 `[]`
     * 相同就放行。
     */
    private fun resultCompatible(parameter: PsiType, result: PsiType): Boolean? {
        if (TypeConversionUtil.isAssignable(parameter, result)) {
            return true
        }
        val target = valueCategory(parameter)
        if (target == ValueCategory.STRING) {
            return true
        }
        val parameterArray = isArrayType(parameter)
        val resultArray = isArrayType(result)
        if (parameterArray != resultArray) {
            // 数组不能注入标量；标量注入数组能否逐元素转换无法静态确定
            return if (resultArray) false else null
        }
        if (parameterArray) {
            return arrayCompatible(parameter, result)
        }
        val actual = valueCategory(result)
        if (target == ValueCategory.NUMBER && actual == ValueCategory.NUMBER) {
            return true
        }
        if (actual == ValueCategory.STRING &&
            (target == ValueCategory.NUMBER || target == ValueCategory.BOOLEAN || target == ValueCategory.CHAR)
        ) {
            return false
        }
        // 未解析的类按短名相同时视为兼容
        if (simpleTypeName(parameter) == simpleTypeName(result)) {
            return true
        }
        return null
    }

    /** 数组间兼容：按组件类型判断，不能把 `String[]` 当成 `String` 标量规则处理。 */
    private fun arrayCompatible(parameter: PsiType, result: PsiType): Boolean? {
        val parameterComponent = (parameter as? PsiArrayType)?.componentType ?: return null
        val resultComponent = (result as? PsiArrayType)?.componentType ?: return null
        if (TypeConversionUtil.isAssignable(parameterComponent, resultComponent)) {
            return true
        }
        val parameterCategory = valueCategory(parameterComponent)
        val resultCategory = valueCategory(resultComponent)
        return if (parameterCategory == ValueCategory.NUMBER && resultCategory == ValueCategory.NUMBER) {
            true
        } else {
            null
        }
    }

    /** 字面量文本能否转换为参数类型；null 表示无法静态判断。 */
    private fun literalAccepts(type: PsiType, text: String): Boolean? {
        val value = text.trim()
        return when (valueCategory(type)) {
            ValueCategory.STRING -> true
            ValueCategory.NUMBER -> NUMBER_LITERAL.matches(value)
            ValueCategory.BOOLEAN -> value.equals("true", true) || value.equals("false", true)
            ValueCategory.CHAR -> text.length == 1
            ValueCategory.OTHER -> null
        }
    }

    private fun valueCategory(type: PsiType): ValueCategory {
        // 数组不是标量：`byte[]` 不能因为剥掉 `[]` 被当成 `byte`（数值类别）
        if (isArrayType(type)) {
            return ValueCategory.OTHER
        }
        return when (simpleTypeName(type)) {
            "String", "CharSequence" -> ValueCategory.STRING
            "boolean", "Boolean" -> ValueCategory.BOOLEAN
            "char", "Character" -> ValueCategory.CHAR
            "byte", "short", "int", "long", "float", "double",
            "Byte", "Short", "Integer", "Long", "Float", "Double",
            "BigInteger", "BigDecimal", "Number" -> ValueCategory.NUMBER
            else -> ValueCategory.OTHER
        }
    }

    /** 是否为数组类型；未解析类型按 `canonicalText` 的 `[]` 后缀识别。 */
    private fun isArrayType(type: PsiType): Boolean =
        type is PsiArrayType || type.canonicalText.endsWith("[]")

    /** `Object` 参数接收任意值；数组是独立类型，不能因剥掉 `[]` 被当成 Object。 */
    private fun isObjectType(type: PsiType): Boolean =
        !isArrayType(type) && simpleTypeName(type) == "Object"

    /** 类型的短名：剥掉泛型、数组与包名；未解析的类同样适用。 */
    private fun simpleTypeName(type: PsiType): String =
        type.canonicalText.substringBefore('<').substringBefore('[').substringAfterLast('.').trim()

    /** SpEL 字符串字面量的内容；不是字符串字面量时返回 null。 */
    private fun stringLiteralContent(expression: String): String? {
        if (expression.length < 2) {
            return null
        }
        val quote = expression.first()
        if ((quote != '\'' && quote != '"') || expression.last() != quote) {
            return null
        }
        return expression.substring(1, expression.length - 1)
    }

    /** 整段是单个 `#{...}` 时返回内部表达式；模板拼接或多片段时返回 null。 */
    private fun singleSpelFragment(text: String): String? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("#{") || !trimmed.endsWith("}")) {
            return null
        }
        var depth = 0
        var quote: Char? = null
        var index = 1
        while (index < trimmed.length) {
            val current = trimmed[index]
            if (quote != null) {
                when {
                    current == '\\' -> {
                        index += 2
                        continue
                    }

                    current == quote -> quote = null
                }
                index++
                continue
            }
            when (current) {
                '\'', '"' -> quote = current
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) {
                        return if (index == trimmed.lastIndex) trimmed.substring(2, index) else null
                    }
                }
            }
            index++
        }
        return null
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

    private enum class ValueCategory { STRING, NUMBER, BOOLEAN, CHAR, OTHER }

    private companion object {
        /** 可与 Java 数值类型互转的文本：可选符号、小数与指数，允许类型后缀。 */
        val NUMBER_LITERAL = Regex("""[+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?[lLfFdD]?""")
    }
}
