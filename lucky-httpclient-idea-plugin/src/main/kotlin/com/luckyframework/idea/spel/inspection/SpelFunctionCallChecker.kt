package com.luckyframework.idea.spel.inspection

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import com.luckyframework.idea.spel.completion.SpelElementContext
import com.luckyframework.idea.spel.completion.SpelTypeResolver
import com.luckyframework.idea.spel.lexer.SpelElementScopes
import com.luckyframework.idea.spel.lexer.SpelExpressionText
import com.luckyframework.idea.spel.lexer.SpelScanner
import com.luckyframework.idea.spel.lexer.SpelSyntaxParser
import com.luckyframework.idea.spel.lexer.SpelTokenizer
import com.luckyframework.idea.spel.model.SpelFragment
import com.luckyframework.idea.spel.model.SpelToken
import com.luckyframework.idea.spel.model.SpelTokenType
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelSymbol

/**
 * 函数调用问题的类别，决定检查的默认严重级别。
 */
enum class SpelCallProblemKind {
    /** 参数数量与所有重载都不匹配，运行期一定失败 */
    ARGUMENT_COUNT,

    /** 参数类型与参数声明不兼容 */
    ARGUMENT_TYPE,

    /** 参数表达式的结果类型无法静态推导，只能提示而不断言 */
    ARGUMENT_TYPE_UNKNOWN
}

/**
 * 一个函数调用问题；[range] 相对所在字符串字面量的源码文本。
 */
data class SpelCallProblem(
    val kind: SpelCallProblemKind,
    val range: TextRange,
    val message: String
)

/**
 * SpEL 函数调用的静态检查：参数数量与参数类型。
 *
 * 只检查能在符号层解析到的函数（当前类静态方法、`@SpELImport` 导入的函数、
 * 内置函数）。参数表达式的类型先按字面量与符号声明快速推导，再交给
 * [SpelTypeResolver] 解析方法链与函数调用结果；仍无法确定时给出警告，
 * 由用户判断运行期行为。
 *
 * 类型兼容规则：数值/布尔之间的确定冲突直接报告，例如把字符串字面量
 * 传给 `Integer` 参数；形参与实参都能解析到类时按继承关系判断引用类型
 * 是否可赋值（例如把 `ApiDescribe` 传给 `String` 参数）；任一方无法解析
 * 时保守放行，避免对运行期转换（字符串转日期/枚举等）误报。
 */
class SpelFunctionCallChecker(
    private val scanner: SpelScanner = SpelScanner(),
    private val tokenizer: SpelTokenizer = SpelTokenizer(),
    private val syntax: SpelSyntaxParser = SpelSyntaxParser()
) {

    fun check(literalText: String, context: SpelContext, project: Project): List<SpelCallProblem> {
        val problems = ArrayList<SpelCallProblem>()
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
        problems: MutableList<SpelCallProblem>
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
        problems: MutableList<SpelCallProblem>
    ) {
        val nested = fragment.children.map { it.textRange }
        val tokens = tokenizer.tokenize(literalText, fragment)
            .filter { token -> nested.none { it.contains(token.range) } }
        if (tokens.isEmpty()) {
            return
        }
        val typeResolver = SpelTypeResolver(project, context)
        val matching = matchingParens(tokens)
        val expressionStart = fragment.expressionRange.startOffset
        for (index in tokens.indices) {
            // 调用位置的元素上下文：`users.?[name.length()]` 的接收者与
            // 实参按当前元素类型解析；裸函数是元素方法，不是全局函数
            val scopes = if (tokens[index].type == SpelTokenType.FUNCTION ||
                tokens[index].type == SpelTokenType.IDENTIFIER
            ) {
                SpelElementScopes.enclosing(literalText, expressionStart, tokens[index].range.startOffset)
            } else {
                emptyList()
            }
            val elementType = if (scopes.isEmpty()) null else SpelElementContext.elementType(scopes, typeResolver)
            if (checkFunctionCall(
                    literalText, tokens, matching, index,
                    scopes.isNotEmpty(), elementType, context, typeResolver, problems
                )
            ) {
                continue
            }
            if (checkMethodCall(
                    literalText, fragment, tokens, matching, index,
                    elementType, context, typeResolver, problems
                )
            ) {
                continue
            }
            checkConstructorCall(literalText, tokens, matching, index, elementType, context, typeResolver, problems)
        }
    }

    /** 全局函数与命名空间函数：`fn(...)`、`#ns.fn(...)`。 */
    private fun checkFunctionCall(
        literalText: String,
        tokens: List<SpelToken>,
        matching: Map<Int, Int>,
        index: Int,
        inElement: Boolean,
        elementType: PsiType?,
        context: SpelContext,
        typeResolver: SpelTypeResolver,
        problems: MutableList<SpelCallProblem>
    ): Boolean {
        val call = callAt(tokens, index, context, inElement) ?: return false
        val open = index + 1
        val close = matching[open] ?: return false
        val arguments = splitArguments(literalText, tokens, open, close) ?: return false
        val shapes = context.functions(call.name, call.namespace)
            .mapNotNull { shapeOf(it) }
            .distinctBy { it.key() }
        if (shapes.isEmpty()) {
            return false
        }
        checkCall(
            literalText, tokens, index, close, call, arguments,
            shapes, elementType, context, typeResolver, problems
        )
        return true
    }

    /** 方法调用：`receiver.method(...)`，接收者支持函数结果、方法链、下标与 `@bean`。 */
    private fun checkMethodCall(
        literalText: String,
        fragment: SpelFragment,
        tokens: List<SpelToken>,
        matching: Map<Int, Int>,
        index: Int,
        elementType: PsiType?,
        context: SpelContext,
        typeResolver: SpelTypeResolver,
        problems: MutableList<SpelCallProblem>
    ): Boolean {
        val call = methodCallAt(tokens, index, literalText, fragment.expressionRange.startOffset, context)
            ?: return false
        val open = index + 1
        val close = matching[open] ?: return false
        val arguments = splitArguments(literalText, tokens, open, close) ?: return false
        val owner = methodOwner(call.receiver, typeResolver, elementType) ?: return false
        val shapes = owner.findMethodsByName(call.name, true)
            .mapNotNull { shapeOf(it) }
            .distinctBy { it.key() }
        if (shapes.isEmpty()) {
            return false
        }
        checkCall(
            literalText, tokens, index, close,
            Call(call.name, null, "方法"), arguments, shapes,
            elementType, context, typeResolver, problems
        )
        return true
    }

    /** 构造器调用：`new X(...)`。 */
    private fun checkConstructorCall(
        literalText: String,
        tokens: List<SpelToken>,
        matching: Map<Int, Int>,
        index: Int,
        elementType: PsiType?,
        context: SpelContext,
        typeResolver: SpelTypeResolver,
        problems: MutableList<SpelCallProblem>
    ) {
        val call = constructorAt(tokens, index) ?: return
        val close = matching[call.openIndex] ?: return
        val arguments = splitArguments(literalText, tokens, call.openIndex, close) ?: return
        val psiClass = typeResolver.findTypeReference(call.typeName) ?: return
        val shapes = psiClass.constructors
            .mapNotNull { shapeOf(it) }
            .distinctBy { it.key() }
        if (shapes.isEmpty()) {
            return
        }
        checkCall(
            literalText, tokens, index, close,
            Call(psiClass.name ?: call.typeName, null, "构造器"),
            arguments, shapes, elementType, context, typeResolver, problems
        )
    }

    private fun checkCall(
        literalText: String,
        tokens: List<SpelToken>,
        nameIndex: Int,
        closeIndex: Int,
        call: Call,
        arguments: List<CallArgument>,
        shapes: List<CallableShape>,
        elementType: PsiType?,
        context: SpelContext,
        typeResolver: SpelTypeResolver,
        problems: MutableList<SpelCallProblem>
    ) {
        val countCompatible = shapes.filter { it.acceptsCount(arguments.size) }
        if (countCompatible.isEmpty()) {
            val expected = shapes.map { it.countLabel() }.distinct().joinToString(" 或 ")
            problems += SpelCallProblem(
                SpelCallProblemKind.ARGUMENT_COUNT,
                TextRange(tokens[nameIndex].range.startOffset, tokens[closeIndex].range.endOffset),
                "${call.label} '${call.name}' 的参数数量不匹配：应为 $expected，实际提供 ${arguments.size} 个"
            )
            return
        }

        val actualTypes = arguments.map { inferType(it, elementType, context, typeResolver) }
        val mismatches = countCompatible.map { shape ->
            actualTypes.mapIndexed { index, actual ->
                actual != null && !shape.accepts(index, actual)
            }
        }
        for (index in actualTypes.indices) {
            val actual = actualTypes[index]
            if (actual == null) {
                // 表达式结果不确定时不断言错误，只提醒用户无法校验
                val message = unknownTypeMessage(countCompatible, index) ?: continue
                problems += SpelCallProblem(
                    SpelCallProblemKind.ARGUMENT_TYPE_UNKNOWN,
                    arguments[index].range,
                    message
                )
                continue
            }
            // 只有所有可接受参数数量的重载都拒绝该参数时才提示，避免重载误报
            if (mismatches.any { !it[index] }) {
                continue
            }
            val expected = countCompatible
                .mapNotNull { it.parameterAt(index)?.label() }
                .distinct()
                .joinToString(" 或 ")
            problems += SpelCallProblem(
                SpelCallProblemKind.ARGUMENT_TYPE,
                arguments[index].range,
                "参数类型不匹配：需要 $expected，实际为 ${actual.label}"
            )
        }
    }

    /**
     * 无法推导时带上可接受重载的形参类型，帮助用户自行核对。
     *
     * 形参本身就是 `Object` 或签名缺失时任何值都合法，不产生无意义的提示。
     */
    private fun unknownTypeMessage(shapes: List<CallableShape>, index: Int): String? {
        val parameters = shapes.mapNotNull { it.parameterAt(index) }
        if (parameters.all { it.isUnconstrained() }) {
            return null
        }
        val expected = parameters
            .mapNotNull { it.label().takeIf { it != "?" } }
            .distinct()
            .joinToString(" 或 ")
        return if (expected.isEmpty()) {
            "无法推导参数表达式的类型，无法校验参数类型"
        } else {
            "无法推导参数表达式的类型，无法校验参数类型（需要 $expected）"
        }
    }

    /** 从 `名称(` 识别函数调用；命名空间成员调用（`#ns.fn(...)`）按符号事实过滤。 */
    private fun callAt(
        tokens: List<SpelToken>,
        index: Int,
        context: SpelContext,
        inElement: Boolean
    ): Call? {
        val token = tokens[index]
        val next = tokens.getOrNull(index + 1) ?: return null
        if (next.type != SpelTokenType.LPAREN) {
            return null
        }
        return when (token.type) {
            // 元素上下文里的裸函数是元素类型的方法（`users.![getName()]`），
            // 交给成员访问检查；`#fn(...)` 仍是全局函数
            SpelTokenType.FUNCTION -> if (inElement && !token.text.startsWith("#")) {
                null
            } else {
                Call(token.text.removePrefix("#"), null, "SpEL 函数")
            }

            SpelTokenType.IDENTIFIER -> {
                if (tokens.getOrNull(index - 1)?.type != SpelTokenType.DOT) {
                    return null
                }
                val qualifier = tokens.getOrNull(index - 2) ?: return null
                // `#name.` 可能是命名空间（`#crypto.md5()`），也可能是普通变量
                // （`#user.fn()`）：只有符号表登记为命名空间时才按函数调用检查
                if (qualifier.type != SpelTokenType.NAMESPACE ||
                    context.namespaces(qualifier.text.removePrefix("#")).isEmpty()
                ) {
                    return null
                }
                Call(token.text, qualifier.text.removePrefix("#"), "SpEL 函数")
            }

            else -> null
        }
    }

    /**
     * 从 `成员(` 识别方法调用：`receiver.method(...)`。
     *
     * 接收者文本用 [SpelExpressionText.receiverStart] 从访问符向左回溯，
     * 支持函数结果（`add(1, 3).get(0)`）、方法链、下标与 `@bean`；
     * `#ns.fn(...)` 属于命名空间函数，交给 [checkFunctionCall]。
     */
    private fun methodCallAt(
        tokens: List<SpelToken>,
        index: Int,
        literalText: String,
        expressionStart: Int,
        context: SpelContext
    ): MethodCall? {
        val token = tokens[index]
        if (token.type != SpelTokenType.IDENTIFIER) {
            return null
        }
        if (tokens.getOrNull(index + 1)?.type != SpelTokenType.LPAREN) {
            return null
        }
        val access = tokens.getOrNull(index - 1) ?: return null
        if (!isMemberAccess(access)) {
            return null
        }
        // `#ns.fn(...)` 属于命名空间函数（以符号表为准）；其余 `#var.fn()`
        // /`#this.fn()` 是接收者对象的成员方法
        if (tokens.getOrNull(index - 2)?.let {
                it.type == SpelTokenType.NAMESPACE &&
                    context.namespaces(it.text.removePrefix("#")).isNotEmpty()
            } == true
        ) {
            return null
        }
        val accessOffset = access.range.startOffset
        if (accessOffset <= expressionStart) {
            return null
        }
        val prefix = literalText.substring(expressionStart, accessOffset)
        val receiverStart = expressionStart + SpelExpressionText.receiverStart(prefix)
        val receiver = literalText.substring(receiverStart, accessOffset).trim()
        if (receiver.isEmpty()) {
            return null
        }
        return MethodCall(receiver, token.text)
    }

    /** 从 `new 类型(` 识别构造器调用，返回类型名与左括号 token 下标。 */
    private fun constructorAt(tokens: List<SpelToken>, index: Int): ConstructorCall? {
        val token = tokens[index]
        if (token.type != SpelTokenType.KEYWORD || token.text != "new") {
            return null
        }
        val typeName = StringBuilder()
        var cursor = index + 1
        while (cursor < tokens.size) {
            val current = tokens[cursor]
            if (current.type !in TYPE_NAME_TOKENS) {
                break
            }
            typeName.append(current.text)
            cursor++
        }
        if (typeName.isEmpty() || tokens.getOrNull(cursor)?.type != SpelTokenType.LPAREN) {
            return null
        }
        return ConstructorCall(typeName.toString().trim(), cursor)
    }

    /** 方法接收者：`@bean` 按 Bean 声明类，其余交给类型推导（透传元素上下文）。 */
    private fun methodOwner(
        receiver: String,
        typeResolver: SpelTypeResolver,
        elementType: PsiType?
    ): PsiClass? {
        val trimmed = receiver.trim()
        if (trimmed.startsWith("@")) {
            return typeResolver.resolveBeanClass(trimmed.removePrefix("@").trim())
        }
        return typeResolver.resolve(trimmed, elementType)
    }

    /** 成员访问符：`.` 或安全导航 `?.`。 */
    private fun isMemberAccess(token: SpelToken): Boolean =
        token.type == SpelTokenType.DOT ||
            (token.type == SpelTokenType.OPERATOR && token.text == "?.")

    /** 每个左括号 token 下标 → 匹配的右括号 token 下标。 */
    private fun matchingParens(tokens: List<SpelToken>): Map<Int, Int> {
        val matching = HashMap<Int, Int>()
        val stack = ArrayList<Int>()
        for ((index, token) in tokens.withIndex()) {
            when (token.type) {
                SpelTokenType.LPAREN -> stack.add(index)
                SpelTokenType.RPAREN -> if (stack.isNotEmpty()) {
                    matching[stack.removeAt(stack.size - 1)] = index
                }

                else -> Unit
            }
        }
        return matching
    }

    /**
     * 按顶层逗号切分参数；返回 null 表示存在空参数（语法错误）。
     *
     * 括号、方括号与内联 `{...}` 内部的逗号不切分。
     */
    private fun splitArguments(literalText: String, tokens: List<SpelToken>, open: Int, close: Int): List<CallArgument>? {
        if (close == open + 1) {
            return emptyList()
        }
        val result = ArrayList<CallArgument>()
        var depth = 0
        var current = ArrayList<SpelToken>()
        for (index in open + 1 until close) {
            val token = tokens[index]
            // 选择/投影标记自身包含 `[`，与右侧 `]` 配对，
            // 避免条件内部的逗号被误当作参数分隔
            if (isBraceOpen(token) || token.type == SpelTokenType.LPAREN ||
                token.type == SpelTokenType.LBRACKET ||
                token.type == SpelTokenType.SELECTION || token.type == SpelTokenType.PROJECTION
            ) {
                depth++
            } else if (isBraceClose(token) || token.type == SpelTokenType.RPAREN ||
                token.type == SpelTokenType.RBRACKET
            ) {
                depth--
            }
            if (depth == 0 && token.type == SpelTokenType.COMMA) {
                if (current.isEmpty()) {
                    return null
                }
                result += argumentOf(literalText, current)
                current = ArrayList()
            } else {
                current += token
            }
        }
        if (current.isEmpty()) {
            return null
        }
        result += argumentOf(literalText, current)
        return result
    }

    private fun argumentOf(literalText: String, tokens: List<SpelToken>): CallArgument {
        val range = TextRange(tokens.first().range.startOffset, tokens.last().range.endOffset)
        return CallArgument(tokens, range, literalText.substring(range.startOffset, range.endOffset))
    }

    private fun isBraceOpen(token: SpelToken): Boolean =
        token.type == SpelTokenType.IDENTIFIER && token.text == "{"

    private fun isBraceClose(token: SpelToken): Boolean =
        token.type == SpelTokenType.IDENTIFIER && token.text == "}"

    /** 推断参数表达式的静态类型；无法确定时返回 null。 */
    private fun inferType(
        argument: CallArgument,
        elementType: PsiType?,
        context: SpelContext,
        typeResolver: SpelTypeResolver
    ): ActualType? = inferSimple(argument, context, typeResolver)
        ?: typeResolver.resolve(argument.text, elementType)?.let { actualOfClass(it) }

    /** 字面量、变量、`T(...)` 与嵌套函数调用的快速推导，避免每次都查 PSI。 */
    private fun inferSimple(
        argument: CallArgument,
        context: SpelContext,
        typeResolver: SpelTypeResolver
    ): ActualType? {
        val tokens = argument.tokens
        val single = tokens.singleOrNull()
        if (single != null) {
            return inferToken(single, context, typeResolver)
        }
        // 一元正负号
        if (tokens.size == 2 &&
            tokens[0].type == SpelTokenType.OPERATOR && tokens[0].text in UNARY_SIGNS &&
            tokens[1].type == SpelTokenType.NUMBER
        ) {
            return numberType(tokens[1].text)
        }
        // 嵌套函数调用整体作为参数：取返回值类型
        if (tokens.size >= 3 &&
            tokens[0].type == SpelTokenType.FUNCTION &&
            tokens[1].type == SpelTokenType.LPAREN &&
            tokens.last().type == SpelTokenType.RPAREN &&
            matchingParens(tokens)[1] == tokens.lastIndex
        ) {
            val name = tokens[0].text.removePrefix("#")
            return context.functions(name, null)
                .firstOrNull { !it.type.isNullOrBlank() }
                ?.let { actualOf(it.type, typeResolver) }
        }
        return null
    }

    private fun inferToken(
        token: SpelToken,
        context: SpelContext,
        typeResolver: SpelTypeResolver
    ): ActualType? = when (token.type) {
        SpelTokenType.STRING -> ActualType("String", TypeCategory.STRING)
        SpelTokenType.NUMBER -> numberType(token.text)
        SpelTokenType.KEYWORD -> when (token.text) {
            "true", "false" -> ActualType("boolean", TypeCategory.BOOLEAN)
            "null" -> ActualType("null", TypeCategory.NULL)
            else -> null
        }

        SpelTokenType.VARIABLE -> variableType(token.text.removePrefix("#"), context, typeResolver)
        SpelTokenType.ROOT_VARIABLE -> variableType(token.text, context, typeResolver)
        SpelTokenType.TYPE_REFERENCE -> typeReferenceType(token.text, typeResolver)

        else -> null
    }

    /** `T(X)` 的值是 `Class<X>`，不是 X 本身。 */
    private fun typeReferenceType(text: String, typeResolver: SpelTypeResolver): ActualType {
        val inner = text.removePrefix("T").removePrefix("(").removeSuffix(")").trim()
        val display = inner.substringAfterLast('.')
        return ActualType(
            if (display.isEmpty()) "Class" else "Class<$display>",
            TypeCategory.OTHER,
            typeResolver.findClass("Class")
        )
    }

    private fun variableType(
        name: String,
        context: SpelContext,
        typeResolver: SpelTypeResolver
    ): ActualType? =
        context.variables(name)
            .firstOrNull { !it.type.isNullOrBlank() }
            ?.let { actualOf(it.type, typeResolver) }

    private fun numberType(text: String): ActualType {
        val label = when {
            text.endsWith("L") || text.endsWith("l") -> "long"
            text.endsWith("F") || text.endsWith("f") -> "float"
            text.endsWith("D") || text.endsWith("d") || text.contains('.') -> "double"
            else -> "int"
        }
        return ActualType(label, TypeCategory.NUMBER)
    }

    /**
     * 由类型文本构造实参类型。
     *
     * 保留解析到的 [PsiClass]，参数兼容性检查才能按继承关系判断；
     * 解析失败时只保留名称与类别，由调用方保守放行。
     */
    private fun actualOf(type: String?, typeResolver: SpelTypeResolver): ActualType? {
        val name = simpleName(type) ?: return null
        val category = categoryOf(name)
        // Object/未知类型不提供任何类型信息
        return if (category == TypeCategory.UNKNOWN) {
            null
        } else {
            ActualType(name, category, typeResolver.findClass(type))
        }
    }

    private fun actualOfClass(psiClass: PsiClass): ActualType? {
        val name = psiClass.name ?: return null
        val category = categoryOf(name)
        return if (category == TypeCategory.UNKNOWN) {
            null
        } else {
            ActualType(name, category, psiClass)
        }
    }

    private fun shapeOf(symbol: SpelSymbol): CallableShape? {
        (symbol.declaration as? PsiMethod)?.let { return shapeOf(it) }
        val signature = symbol.signature?.takeIf { it.isNotBlank() } ?: return null
        return shapeOfSignature(signature)
    }

    private fun shapeOf(method: PsiMethod): CallableShape {
        val parameters = method.parameterList.parameters
        val varargs = method.isVarArgs
        return CallableShape(
            parameters.mapIndexed { index, parameter ->
                ParameterShape(
                    type = parameter.type.presentableText,
                    varargs = varargs && index == parameters.size - 1,
                    psiClass = (parameter.type as? PsiClassType)?.resolve()
                )
            }
        )
    }

    private fun shapeOfSignature(signature: String): CallableShape? {
        val open = signature.indexOf('(')
        val close = signature.lastIndexOf(')')
        if (open < 0 || close < open) {
            return null
        }
        val types = splitParameterTypes(signature.substring(open + 1, close).trim())
        return CallableShape(
            types.mapIndexed { index, type ->
                ParameterShape(
                    type = type.removeSuffix("..."),
                    varargs = type.endsWith("...") && index == types.size - 1
                )
            }
        )
    }

    /** 按顶层逗号切分签名参数；泛型内部的逗号不切分。 */
    private fun splitParameterTypes(body: String): List<String> {
        if (body.isEmpty()) {
            return emptyList()
        }
        val result = ArrayList<String>()
        var depth = 0
        val current = StringBuilder()
        for (character in body) {
            when (character) {
                '<' -> depth++
                '>' -> if (depth > 0) depth--
                ',' -> if (depth == 0) {
                    result += current.toString().trim()
                    current.setLength(0)
                    continue
                }
            }
            current.append(character)
        }
        if (current.isNotBlank()) {
            result += current.toString().trim()
        }
        return result.filter { it.isNotEmpty() }
    }

    private data class Call(val name: String, val namespace: String?, val label: String)

    private data class MethodCall(val receiver: String, val name: String)

    private data class ConstructorCall(val typeName: String, val openIndex: Int)

    private data class CallArgument(val tokens: List<SpelToken>, val range: TextRange, val text: String)

    private class ParameterShape(
        val type: String?,
        val varargs: Boolean,
        val psiClass: PsiClass? = null
    ) {

        fun label(): String = type?.removeSuffix("...") ?: "?"

        /** 形参为 Object/未知类型或签名缺失时，任何实参都可以接受。 */
        fun isUnconstrained(): Boolean {
            val expected = simpleName(type) ?: return true
            return categoryOf(expected) == TypeCategory.UNKNOWN
        }

        fun accepts(actual: ActualType): Boolean {
            val expected = simpleName(type) ?: return true
            return when (categoryOf(expected)) {
                TypeCategory.UNKNOWN -> true
                // 数字/布尔虽然运行期可被转成字符串，但显式传入多半是笔误；
                // `String` 是 final 类，其余引用类型一定无法赋值；`CharSequence`
                // 等父类型还需要按继承关系判断（如 `StringBuilder`）
                TypeCategory.STRING -> when (actual.category) {
                    TypeCategory.NULL, TypeCategory.STRING -> true
                    TypeCategory.NUMBER, TypeCategory.BOOLEAN -> false
                    else -> expected != "String" && assignable(actual)
                }

                TypeCategory.NUMBER -> actual.category == TypeCategory.NUMBER ||
                    actual.category == TypeCategory.NULL

                TypeCategory.BOOLEAN -> actual.category == TypeCategory.BOOLEAN ||
                    actual.category == TypeCategory.NULL

                // 字符串到日期、枚举等引用类型的转换由框架在运行期处理；
                // 其余引用类型按继承关系判断
                TypeCategory.OTHER -> when (actual.category) {
                    TypeCategory.OTHER -> assignable(actual)
                    else -> true
                }

                TypeCategory.NULL -> true
            }
        }

        /**
         * 形参与实参都能解析到类时按继承关系判断可赋值性。
         *
         * 任一方缺失（泛型变量、数组、依赖未导入）时保守放行，
         * 避免对运行期转换与泛型代码误报。
         */
        private fun assignable(actual: ActualType): Boolean {
            val expectedClass = psiClass ?: return true
            val actualClass = actual.psiClass ?: return true
            return InheritanceUtil.isInheritorOrSelf(actualClass, expectedClass, true)
        }
    }

    private class CallableShape(private val parameters: List<ParameterShape>) {

        fun acceptsCount(count: Int): Boolean {
            val varargs = parameters.lastOrNull()?.varargs == true
            val fixed = if (varargs) parameters.size - 1 else parameters.size
            return if (varargs) count >= fixed else count == fixed
        }

        fun parameterAt(index: Int): ParameterShape? =
            parameters.getOrNull(index) ?: parameters.lastOrNull()?.takeIf { it.varargs }

        fun accepts(index: Int, actual: ActualType): Boolean =
            parameterAt(index)?.accepts(actual) ?: true

        fun countLabel(): String {
            val varargs = parameters.lastOrNull()?.varargs == true
            return if (varargs) "${parameters.size - 1} 个以上" else "${parameters.size} 个"
        }

        fun key(): List<Pair<String?, Boolean>> = parameters.map { it.type to it.varargs }
    }

    private companion object {
        val UNARY_SIGNS = setOf("-", "+")

        /** `new` 之后组成类型名的 token：类型名本体与点链分隔符。 */
        val TYPE_NAME_TOKENS = setOf(
            SpelTokenType.TYPE_REFERENCE,
            SpelTokenType.IDENTIFIER,
            SpelTokenType.DOT
        )
    }
}

private data class ActualType(
    val label: String,
    val category: TypeCategory,
    val psiClass: PsiClass? = null
)

private enum class TypeCategory { STRING, BOOLEAN, NUMBER, NULL, OTHER, UNKNOWN }

private fun simpleName(type: String?): String? {
    var name = type?.trim() ?: return null
    if (name.isEmpty() || name == "?") {
        return null
    }
    name = name.substringBefore('<').trim()
    while (name.endsWith("[]")) {
        name = name.removeSuffix("[]").trim()
    }
    name = name.removeSuffix("...").trim()
    return name.substringAfterLast('.').trim().takeIf { it.isNotEmpty() }
}

private fun categoryOf(name: String): TypeCategory = when (name) {
    "String", "CharSequence" -> TypeCategory.STRING
    "boolean", "Boolean" -> TypeCategory.BOOLEAN
    "int", "Integer", "long", "Long", "short", "Short", "byte", "Byte",
    "float", "Float", "double", "Double",
    "BigInteger", "BigDecimal", "Number" -> TypeCategory.NUMBER

    "Object", "Any" -> TypeCategory.UNKNOWN
    else -> TypeCategory.OTHER
}
