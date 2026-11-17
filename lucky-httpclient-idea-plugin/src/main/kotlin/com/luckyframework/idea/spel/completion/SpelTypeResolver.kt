package com.luckyframework.idea.spel.completion

import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypes
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiUtil
import com.luckyframework.idea.spel.lexer.SpelExpressionText
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelSymbol

/**
 * 轻量 SpEL 类型推导（design D7）。
 *
 * 支持变量/函数/命名空间符号的内置或导入类型、`T(...)` 类型引用、
 * 字符串与数字字面量、字段访问、方法调用与函数调用的返回值；
 * 算术（`+`/`-`/`*`/`/`/`%`/`^`）、比较与逻辑（`==`/`<`/`and` 等）
 * 按 SpEL 语义推导结果类型；无法推断时返回 null，由成员补全降级为通用成员。
 *
 * 有 PSI 声明时直接取声明类型（准确区分同名类与泛型）；内置符号只有
 * 短类型名（如 `Request`），按框架包优先的顺序查找，避免命中依赖里同名
 * 的无关类（如 `okhttp3.Request`）。
 *
 * 选择/投影表达式（`.?[]`/`.^[]`/`.$[]`/`.![]`）按 Spring 的结果类型
 * 推导，方括号内的表达式以 `#this` 为当前元素（Map 为 `Map.Entry`）。
 */
class SpelTypeResolver(
    private val project: Project,
    private val context: SpelContext
) {

    /**
     * 解析表达式（如 `#user`、`#user.address`、`#time()`、`add(1, 2).size()`）的静态类型。
     *
     * [elementType] 非空时表示当前处于选择/投影方括号内部：`#this` 与
     * 裸标识符按元素类型解析。
     */
    fun resolve(expression: String, elementType: PsiType? = null): PsiClass? {
        val expr = expression.trim()
        if (expr.isEmpty()) {
            return null
        }
        // `T(X)` 作为成员接收者时按 X 解析（`T(X).static`）
        if (isTypeReference(expr)) {
            return findTypeReference(expr.substring(2, expr.length - 1))
        }
        return resolveClass(resolvePsiType(expr, elementType))
    }

    /**
     * 表达式结果的静态类型，保留泛型实参。
     *
     * [resolve] 只返回裸类，供成员补全与导航使用；这里保留 `List<Integer>`
     * 这类类型实参，用于泛型方法返回值的回填（`add(1, 3).get(0)` → `Integer`）。
     */
    internal fun resolvePsiType(expression: String, elementType: PsiType? = null): PsiType? {
        val expr = expression.trim()
        if (expr.isEmpty()) {
            return null
        }
        selectionLikeType(expr, elementType)?.let { return it }
        // 括号包住整个表达式时按内部表达式推导（`(1 + 2)`）
        unwrapParentheses(expr)?.let { return resolvePsiType(it, elementType) }
        // 识别为二元/一元表达式时直接返回其结果（类型未知也不继续按点链解析）
        binaryType(expr, elementType)?.let { return it.type }
        // 元素上下文中的裸方法调用（`.?[name.length() > 3]`）按元素成员解析
        if (elementType != null && isSimpleCall(expr)) {
            memberType(elementType, expr)?.let { return it }
        }
        functionCallType(expr)?.let { return it }
        indexAccessType(expr, elementType)?.let { return it }
        newExpressionType(expr)?.let { return it }
        val dot = lastTopLevelDot(expr)
        if (dot >= 0) {
            // `?.` 安全导航的 `?` 属于操作符，不属于接收者
            val receiverExpr = expr.substring(0, dot).trim().removeSuffix("?")
            val receiver = receiverType(receiverExpr, elementType) ?: return null
            return memberType(receiver, expr.substring(dot + 1).trim())
        }
        return atomType(expr, elementType)
    }

    /** 二元/一元表达式的推导结果；[type] 为 null 表示识别为运算但类型未知。 */
    private data class BinaryOutcome(val type: PsiType?)

    private data class TopLevelOperator(val index: Int, val text: String)

    /**
     * 二元/一元表达式的结果类型。
     *
     * 按 SpEL 优先级从低到高查找顶层运算符，找不到二元运算符时按一元处理：
     * 比较（`==`/`<`/`matches`/`between`/`instanceof` 等）与逻辑（`&&`/`and`）
     * 的结果一定是 boolean，不依赖操作数类型；`+` 只要有一个操作数是 String
     * 就是字符串拼接，其余算术按数值提升。不是运算表达式时返回 null（类型未知也是 null）。
     */
    private fun binaryType(expr: String, elementType: PsiType?): BinaryOutcome? {
        for (operators in BINARY_OPERATOR_LEVELS) {
            val operator = lastTopLevelOperator(expr, operators) ?: continue
            return BinaryOutcome(binaryResult(operator, expr, elementType))
        }
        return unaryResult(expr, elementType)?.let { BinaryOutcome(it) }
    }

    private fun binaryResult(operator: TopLevelOperator, expr: String, elementType: PsiType?): PsiType? {
        if (operator.text in BOOLEAN_OPERATORS) {
            return PsiTypes.booleanType()
        }
        val left = resolvePsiType(expr.substring(0, operator.index), elementType)
        val right = resolvePsiType(expr.substring(operator.index + operator.text.length), elementType)
        // SpEL 的 `+`：任一操作数是 String 时结果就是 String 拼接
        if (operator.text == "+" && (isStringType(left) || isStringType(right))) {
            return classType(findClass("String"))
        }
        val leftKind = left?.let { numericKind(it) } ?: return null
        val rightKind = right?.let { numericKind(it) } ?: return null
        return numericType(maxOf(leftKind, rightKind))
    }

    /** 一元 `!`/`not` 结果是 boolean；`-x`/`+x` 保持操作数的数值类型。 */
    private fun unaryResult(expr: String, elementType: PsiType?): PsiType? {
        if (expr.startsWith("!") && !expr.startsWith("!=")) {
            return PsiTypes.booleanType()
        }
        if (expr.startsWith("not") && expr.getOrNull(3)?.isWhitespace() == true) {
            return PsiTypes.booleanType()
        }
        if (expr.startsWith("-") || expr.startsWith("+")) {
            val operand = resolvePsiType(expr.substring(1), elementType) ?: return null
            return numericKind(operand)?.let { numericType(it) }
        }
        return null
    }

    /** 括号包住整个表达式时返回内部文本；否则返回 null。 */
    private fun unwrapParentheses(expr: String): String? {
        if (!expr.startsWith("(") || !expr.endsWith(")")) {
            return null
        }
        if (matchingOpenParen(expr, expr.lastIndex) != 0) {
            return null
        }
        return expr.substring(1, expr.lastIndex).trim()
    }

    /** 从右向左查找最后一个顶层运算符；括号与字符串中的内容不参与。 */
    private fun lastTopLevelOperator(expr: String, operators: List<String>): TopLevelOperator? {
        val depths = operatorDepths(expr)
        for (index in expr.length - 1 downTo 0) {
            if (depths[index] != 0) {
                continue
            }
            for (operator in operators) {
                if (matchesOperator(expr, index, operator)) {
                    return TopLevelOperator(index, operator)
                }
            }
        }
        return null
    }

    /** 运算符是否从 [index] 开始，且是二元运算符（一元 `+`/`-`、方法名与属性名不算）。 */
    private fun matchesOperator(expr: String, index: Int, operator: String): Boolean {
        if (index + operator.length > expr.length || !expr.regionMatches(index, operator, 0, operator.length)) {
            return false
        }
        if (!operator.first().isLetter()) {
            return when (operator) {
                "+", "-" -> !isUnaryPrefix(expr, index)
                else -> true
            }
        }
        // 关键字运算符（`and`/`or`/`matches`/`between`/`instanceof` 等）要求词边界，
        // 且前面不能是点，避免把 `#str.matches('x')` 当成运算符
        val before = expr.getOrNull(index - 1)
        if (before != null && (before.isLetterOrDigit() || before == '_' || before == '$' || before == '.')) {
            return false
        }
        val after = expr.getOrNull(index + operator.length)
        return after == null || !(after.isLetterOrDigit() || after == '_' || after == '$')
    }

    /** `+`/`-` 是否是一元前缀：左侧为空或紧邻另一个运算符、左括号、逗号。 */
    private fun isUnaryPrefix(expr: String, index: Int): Boolean {
        var current = index - 1
        while (current >= 0 && expr[current].isWhitespace()) {
            current--
        }
        if (current < 0) {
            return true
        }
        return expr[current] in "+-*/%^<>=!&|?:,([{"
    }

    /** 每个下标处的括号深度；字符串内与括号字符本身标记为 -1。 */
    private fun operatorDepths(expr: String): IntArray {
        val depths = IntArray(expr.length) { -1 }
        var depth = 0
        var quote: Char? = null
        var index = 0
        while (index < expr.length) {
            val current = expr[index]
            if (quote != null) {
                when (current) {
                    '\\' -> {
                        index += 2
                        continue
                    }

                    quote -> quote = null
                }
                index++
                continue
            }
            when (current) {
                '\'', '"' -> quote = current
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                else -> depths[index] = depth
            }
            index++
        }
        return depths
    }

    /** 表达式是否是 `String`/`CharSequence`：SpEL 的 `+` 遇到它们按字符串拼接。 */
    private fun isStringType(type: PsiType?): Boolean {
        val owner = (type as? PsiClassType)?.resolve() ?: return false
        return owner.qualifiedName == CommonClassNames.JAVA_LANG_STRING ||
            InheritanceUtil.isInheritor(owner, CommonClassNames.JAVA_LANG_CHAR_SEQUENCE)
    }

    /** 数值类型；非数值（boolean、集合、未知类型）返回 null。 */
    private fun numericKind(type: PsiType): NumericKind? {
        if (type is PsiPrimitiveType) {
            return when (type.canonicalText) {
                "byte", "short", "int", "char" -> NumericKind.INT
                "long" -> NumericKind.LONG
                "float" -> NumericKind.FLOAT
                "double" -> NumericKind.DOUBLE
                else -> null
            }
        }
        val owner = (type as? PsiClassType)?.resolve() ?: return null
        return when (owner.qualifiedName) {
            "java.lang.Byte", "java.lang.Short", "java.lang.Integer", "java.lang.Character" -> NumericKind.INT
            "java.lang.Long" -> NumericKind.LONG
            "java.lang.Float" -> NumericKind.FLOAT
            "java.lang.Double" -> NumericKind.DOUBLE
            "java.math.BigInteger" -> NumericKind.BIG_INTEGER
            "java.math.BigDecimal" -> NumericKind.BIG_DECIMAL
            else -> null
        }
    }

    private fun numericType(kind: NumericKind): PsiType =
        classType(findClass(kind.className)) ?: PsiTypes.intType()

    /** 成员接收者：`T(X)` 按 X 解析，其余按普通表达式解析。 */
    private fun receiverType(expr: String, elementType: PsiType?): PsiType? {
        if (isTypeReference(expr)) {
            return classType(findTypeReference(expr.substring(2, expr.length - 1)))
        }
        return resolvePsiType(expr, elementType)
    }

    private fun resolveClass(type: PsiType?): PsiClass? {
        val classType = type as? PsiClassType ?: return null
        return classType.resolve() ?: findClass(classType.presentableText)
    }

    /** 类型对应的类；用于把补全/检查/引用侧拿到的 [PsiType] 转回 `PsiClass`。 */
    internal fun classOf(type: PsiType?): PsiClass? = resolveClass(type)

    /**
     * 选择/投影方括号内的元素类型：数组取组件类型、`Iterable` 取类型实参、
     * `Map` 取 `Map.Entry<K,V>`，与 [resolvePsiType] 的 `#this` 语义一致。
     */
    internal fun elementTypeOf(type: PsiType?): PsiType? = type?.let { collectionElementType(it) }

    private fun atomType(expr: String, elementType: PsiType?): PsiType? = when {
        // `#this` 是选择/投影方括号内的当前元素
        expr == "#this" -> elementType
        isTypeReference(expr) -> classType(findClass("Class"))
        expr.startsWith("'") || expr.startsWith("\"") -> classType(findClass("String"))
        NUMBER.matches(expr) -> classType(findClass(if (expr.contains('.')) "Double" else "Integer"))
        // 布尔字面量是过滤条件最常见的形式，必须能推导为 boolean
        expr == "true" || expr == "false" -> PsiTypes.booleanType()
        expr.startsWith("@") -> classType(resolveBeanClass(expr.removePrefix("@").trim()))
        expr.startsWith("#") ->
            symbolType(expr.removePrefix("#").removeSuffix("()").trim(), root = false)
        // 元素上下文中的裸标识符是元素属性/方法，不回退到 Root 变量
        IDENTIFIER.matches(expr) ->
            if (elementType != null) memberType(elementType, expr) else symbolType(expr, root = true)

        else -> null
    }

    /**
     * 表达式整体是否为 `T(类型)` 类型引用。
     *
     * `T(X)` 作为值使用时结果是 `Class<X>`，与作为成员接收者
     * （`T(X).静态成员` 按 X 解析）的语义不同。
     */
    fun isTypeReference(expression: String): Boolean {
        val expr = expression.trim()
        if (expr.length < 4 || !expr.startsWith("T(")) {
            return false
        }
        return matchingOpenParen(expr, expr.lastIndex) == 1
    }

    /**
     * 函数调用表达式的返回值类型。
     *
     * 只接受整体形如 `name(...)`、`#name(...)`、`#ns.name(...)` 的表达式；
     * `#user.getName()` 这类变量方法链会因找不到同名函数而返回 null，
     * 继续按成员访问解析。
     */
    private fun functionCallType(expr: String): PsiType? {
        if (!expr.endsWith(")")) {
            return null
        }
        val open = matchingOpenParen(expr, expr.lastIndex) ?: return null
        if (open <= 0) {
            return null
        }
        val (namespace, name) = callableName(expr.substring(0, open).trim()) ?: return null
        if (namespace != null && context.namespaces(namespace).isEmpty()) {
            return null
        }
        // `#fn(...)` 只能引用非内置函数；内置函数只支持裸 `name(...)` 调用
        val hashedDirectCall = expr.startsWith("#") && namespace == null
        val functions = context.functions(name, namespace, includeBuiltin = !hashedDirectCall)
        if (functions.isEmpty()) {
            return null
        }
        val arguments = splitArguments(expr.substring(open + 1, expr.lastIndex))
        return selectFunction(functions, arguments)?.let { functionReturnType(it) }
    }

    /** 函数符号的返回值类型：有 PSI 声明时取真实类型，内置函数按声明文本查找。 */
    private fun functionReturnType(function: SpelSymbol): PsiType? {
        (function.declaration as? PsiMethod)?.returnType?.let { return it }
        return classType(findClass(function.type))
    }

    /** 调用表达式前缀拆成命名空间与函数名；不是可调用名称时返回 null。 */
    private fun callableName(callee: String): Pair<String?, String>? {
        if (!callee.startsWith("#")) {
            return if (IDENTIFIER.matches(callee)) null to callee else null
        }
        val body = callee.removePrefix("#")
        val dot = body.lastIndexOf('.')
        if (dot < 0) {
            return if (IDENTIFIER.matches(body)) null to body else null
        }
        val namespace = body.substring(0, dot).trim()
        val name = body.substring(dot + 1).trim()
        if (namespace.isEmpty() || !IDENTIFIER.matches(name)) {
            return null
        }
        return namespace to name
    }

    /** 与 [close] 位置右括号匹配的左括号下标；括号内的字符串不参与配对。 */
    private fun matchingOpenParen(expr: String, close: Int): Int? {
        val stack = ArrayList<Int>()
        var quote: Char? = null
        var index = 0
        while (index <= close) {
            val current = expr[index]
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
                '(' -> stack.add(index)
                ')' -> {
                    if (stack.isEmpty()) {
                        return null
                    }
                    val open = stack.removeAt(stack.size - 1)
                    if (index == close) {
                        return open
                    }
                }
            }
            index++
        }
        return null
    }

    /** 由符号声明或类型名解析变量的静态类型；[root] 选择 Root 变量视图。 */
    private fun symbolType(name: String, root: Boolean): PsiType? {
        val symbol = (if (root) context.rootVariables(name) else context.variables(name))
            .firstOrNull() ?: return null
        // 上下文推导的精确类型优先于声明类型：`$result$` 需要拆掉 Optional/Future 等包装，
        // `$body$` 的类型由 @ConvertMetaType 决定，都与方法声明不同
        symbol.resolvedType?.let { return it }
        // `name::type`/`$n::type` 的值是 ResolvableType 本体，不能按参数声明类型解析
        if (name.endsWith(TYPE_SUFFIX)) {
            return classType(findClass(symbol.type))
        }
        // 导入/类静态函数等有 PSI 声明时按真实类型解析。
        // `@SpelSymbol` 字段是变量名的常量声明，字段类型恒为 String，
        // 变量类型以符号表（type/typeExpression）为准。
        if (symbol.declaration !is PsiField) {
            declarationType(symbol.declaration)?.let { return it }
        }
        return typeOfText(symbol.type)
    }

    /**
     * 符号的类型文本 → [PsiType]，保留数组维度。
     *
     * 内置符号只有类型文本（`ResolvableType[]`、`byte[]`），没有 PSI 声明；
     * [findClass] 会剥掉 `[]` 而不能直接使用，否则数组变量被当成裸类，
     * `$mpts$.length` 会被误报为「不存在属性」。
     */
    private fun typeOfText(typeName: String?): PsiType? {
        var name = typeName?.trim() ?: return null
        var dimensions = 0
        while (name.endsWith("[]")) {
            dimensions++
            name = name.removeSuffix("[]").trim()
        }
        val simple = name.substringBefore('<').substringAfterLast('.').trim()
        val component = PRIMITIVE_TYPES[simple] ?: classType(findClass(name)) ?: return null
        // 多维数组按维度逐层嵌套（`PsiArrayType` 没有维度构造参数）
        var result = component
        repeat(dimensions) { result = PsiArrayType(result) }
        return result
    }

    private fun declarationType(element: PsiElement?): PsiType? = when (element) {
        is PsiClass -> classType(element)
        is PsiMethod -> element.returnType
        is PsiParameter -> element.type
        is PsiField -> element.type
        else -> null
    }

    /**
     * 成员表达式的静态类型：字段直接取声明类型，方法按参数选择重载后取返回类型。
     *
     * 接收者带泛型实参（`List<Integer>`）时，用替换器把方法返回的 `E`
     * 回填为 `Integer`，保证后续链的类型推导不断裂。属性写法找不到字段时
     * 回退到 `getXxx`/`isXxx`（`Map.Entry.key`、`user.name` 都按属性访问）。
     */
    private fun memberType(ownerType: PsiType, segment: String): PsiType? {
        if (segment.isEmpty()) {
            return ownerType
        }
        // 数组的 `length` 是伪字段（Spring 反射可读），`clone()` 返回同类型数组
        if (ownerType is PsiArrayType) {
            return when (segment) {
                "length" -> PsiTypes.intType()
                "clone()" -> ownerType
                else -> null
            }
        }
        val classType = ownerType as? PsiClassType
        val resolved = classType?.resolveGenerics()
        val owner = resolved?.element ?: (classType?.resolve() ?: return null)
        val substitutor = resolved?.substitutor
        if (segment.endsWith(")")) {
            val name = segment.substringBefore('(').trim()
            val arguments = splitArguments(segment.substringAfter('(', "").substringBeforeLast(')'))
            val method = selectMethod(owner.findMethodsByName(name, true), arguments) ?: return null
            val returnType = method.returnType ?: return null
            return substitutor?.substitute(returnType) ?: returnType
        }
        val field = owner.findFieldByName(segment, true)
        if (field != null) {
            return substitutor?.substitute(field.type) ?: field.type
        }
        val getter = getterMethod(owner, segment) ?: return null
        val returnType = getter.returnType ?: return null
        return substitutor?.substitute(returnType) ?: returnType
    }

    /** 属性的 getter：`getXxx`/`isXxx` 无参实例方法。 */
    private fun getterMethod(owner: PsiClass, property: String): PsiMethod? {
        val capitalized = property.replaceFirstChar { it.uppercaseChar() }
        for (name in listOf("get$capitalized", "is$capitalized")) {
            owner.findMethodsByName(name, true)
                .firstOrNull {
                    it.parameterList.parametersCount == 0 && !it.hasModifierProperty(PsiModifier.STATIC)
                }
                ?.let { return it }
        }
        return null
    }

    /** `@bean` 接收者：按「类名首字母大写」反推 Spring Bean 的声明类。 */
    fun resolveBeanClass(name: String): PsiClass? = resolveBeanClasses(name).firstOrNull()

    /**
     * Bean 名 → 候选声明类。
     *
     * 优先精确匹配「首字母小写的类名 = Bean 名」；找不到时按 Spring 常见约定
     * 追加 `<Name>Impl` 候选（`@userMapper` → `UserMapperImpl`），避免接口实现类
     * 因命名差异导致成员补全、悬停与参数检查全部回退为静默。
     */
    fun resolveBeanClasses(name: String): List<PsiClass> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return emptyList()
        }
        val simpleName = trimmed.replaceFirstChar { it.uppercaseChar() }
        val exact = classesBySimpleName(simpleName).filter { it.decapitalizedName() == trimmed }
        if (exact.isNotEmpty()) {
            return exact
        }
        val implementation = simpleName + "Impl"
        return classesBySimpleName(implementation).filter { it.name == implementation }
    }

    private fun classesBySimpleName(simpleName: String): List<PsiClass> = runCatching {
        PsiShortNamesCache.getInstance(project)
            .getClassesByName(simpleName, GlobalSearchScope.allScope(project))
            .toList()
    }.getOrElse { emptyList() }

    private fun PsiClass.decapitalizedName(): String? =
        name?.replaceFirstChar { character -> character.lowercaseChar() }

    /** 下标访问 `receiver[index]`：数组取组件类型，集合取 `get(int)` 的返回类型。 */
    private fun indexAccessType(expr: String, elementType: PsiType?): PsiType? {
        if (!expr.endsWith("]")) {
            return null
        }
        val open = matchingOpenBracket(expr, expr.lastIndex) ?: return null
        if (open <= 0) {
            return null
        }
        val receiver = resolvePsiType(expr.substring(0, open), elementType) ?: return null
        return indexedElementType(receiver)
    }

    /** 下标访问的元素类型：数组取组件类型，集合取 `get(int)` 的返回类型。 */
    private fun indexedElementType(type: PsiType): PsiType? = when (type) {
        is PsiArrayType -> type.componentType
        is PsiClassType -> {
            val resolved = type.resolveGenerics()
            val owner = resolved.element ?: return null
            val getter = owner.findMethodsByName("get", true)
                .firstOrNull { it.parameterList.parametersCount == 1 } ?: return null
            val returnType = getter.returnType ?: return null
            resolved.substitutor.substitute(returnType)
        }

        else -> null
    }

    /**
     * 选择/投影表达式的结果类型：`.?[]`/`.^[]`/`.$[]`/`.![]`，
     * 以及省略接收者的 `?[]`/`^[]`/`$[]`/`![]`。
     *
     * 标记必须是表达式起始或 `.` 之后（允许 `.` 与标记之间夹杂空白，
     * 如 `. ?[`）的单个字符，且与 `[` 紧邻；
     * `$body$[0]` 这类普通下标访问（标记前不是 `.`）不在此处理。
     * 标记前的接收者与方括号内的条件/投影都处于同一元素上下文。
     */
    private fun selectionLikeType(expr: String, outerElementType: PsiType?): PsiType? {
        if (!expr.endsWith("]")) {
            return null
        }
        val open = matchingOpenBracket(expr, expr.lastIndex) ?: return null
        val markerIndex = open - 1
        if (markerIndex < 0 || expr[markerIndex] !in SELECTION_MARKERS) {
            return null
        }
        val head = expr.substring(0, markerIndex).trimEnd()
        if (head.isNotEmpty() && !head.endsWith(".")) {
            return null
        }
        val base = head.removeSuffix("?.").removeSuffix(".").trimEnd()
        val target = if (base.isEmpty()) {
            outerElementType
        } else {
            resolvePsiType(base, outerElementType)
        } ?: return null
        val inner = expr.substring(open + 1, expr.lastIndex).trim()
        return if (expr[markerIndex] == '!') {
            projectionType(target, inner)
        } else {
            selectionType(target, single = expr[markerIndex] != '?')
        }
    }

    /**
     * 选择表达式的结果类型（Spring `Selection`）：
     * 数组保持数组，`Iterable` 变为 `List<T>` 或单个元素 `T`，`Map` 保持 `Map<K,V>`。
     */
    private fun selectionType(target: PsiType, single: Boolean): PsiType? = when {
        target is PsiArrayType -> if (single) target.componentType else target
        isMapType(target) -> target
        isIterableType(target) -> {
            val element = collectionElementType(target)
            if (single) element else listType(element)
        }

        else -> null
    }

    /**
     * 投影表达式的结果类型（Spring `Projection`）：
     * 数组仍为数组，`Map`/`Iterable` 变为 `List<R>`，`R` 未知时用裸 `List`。
     */
    private fun projectionType(target: PsiType, innerExpression: String): PsiType? {
        val element = collectionElementType(target) ?: return null
        val projected = resolvePsiType(innerExpression, element)
        return if (target is PsiArrayType) {
            projected?.let { PsiArrayType(it) }
        } else {
            listType(projected)
        }
    }

    /**
     * 集合元素类型：数组取组件类型，`Iterable` 取类型实参，
     * `Map` 取 `Map.Entry<K,V>`（选择/投影中 `#this` 的类型）。
     */
    private fun collectionElementType(type: PsiType): PsiType? = when (type) {
        is PsiArrayType -> type.componentType
        is PsiClassType -> {
            val owner = type.resolveGenerics().element ?: return null
            when {
                InheritanceUtil.isInheritor(owner, "java.util.Map") -> mapEntryType(type)
                InheritanceUtil.isInheritor(owner, "java.lang.Iterable") ->
                    PsiUtil.extractIterableTypeParameter(type, false)

                else -> null
            }
        }

        else -> null
    }

    /** `Map.Entry<K,V>` 类型；类型实参未知时回退为裸 `Map.Entry`。 */
    private fun mapEntryType(type: PsiType): PsiType? {
        val entryClass = findClass("java.util.Map.Entry")
            ?: findClass("java.util.Map")?.findInnerClassByName("Entry", false)
            ?: return null
        val factory = JavaPsiFacade.getElementFactory(project)
        val keyType = PsiUtil.substituteTypeParameter(type, "java.util.Map", 0, false)
        val valueType = PsiUtil.substituteTypeParameter(type, "java.util.Map", 1, false)
        return if (keyType != null && valueType != null) {
            factory.createType(entryClass, keyType, valueType)
        } else {
            factory.createType(entryClass)
        }
    }

    /** `List<element>`；元素未知时用裸 `List`，保留后续链的成员推导。 */
    private fun listType(element: PsiType?): PsiType? {
        val listClass = findClass("java.util.List") ?: return null
        val factory = JavaPsiFacade.getElementFactory(project)
        val boxed = element?.let { boxedType(it) }
        return if (boxed == null) {
            factory.createType(listClass)
        } else {
            factory.createType(listClass, boxed)
        }
    }

    /** 基本类型装箱：泛型实参不能是 `int`，`![length()]` 的结果应为 `List<Integer>`。 */
    private fun boxedType(type: PsiType): PsiType {
        val primitive = type as? PsiPrimitiveType ?: return type
        return BOXED_TYPES[primitive.canonicalText]?.let { classType(findClass(it)) } ?: type
    }

    private fun isMapType(type: PsiType): Boolean =
        type is PsiClassType && InheritanceUtil.isInheritor(type, "java.util.Map")

    private fun isIterableType(type: PsiType): Boolean =
        type is PsiClassType && InheritanceUtil.isInheritor(type, "java.lang.Iterable")

    /** 整体形如 `name(...)` 的裸方法调用；`#fn(...)`/`new X(...)` 不算。 */
    private fun isSimpleCall(expr: String): Boolean {
        if (!expr.endsWith(")")) {
            return false
        }
        val open = matchingOpenParen(expr, expr.lastIndex) ?: return false
        return open > 0 && IDENTIFIER.matches(expr.substring(0, open).trim())
    }

    /** `new X(...)` 构造表达式的类型。 */
    private fun newExpressionType(expr: String): PsiType? {
        if (!expr.startsWith("new ")) {
            return null
        }
        val body = expr.removePrefix("new ").trim()
        if (!body.endsWith(")")) {
            return null
        }
        val open = matchingOpenParen(body, body.lastIndex) ?: return null
        return classType(findTypeReference(body.substring(0, open).trim()))
    }

    private fun classType(psiClass: PsiClass?): PsiType? =
        psiClass?.let { JavaPsiFacade.getElementFactory(project).createType(it) }

    /** 多个函数重载时按实参数量与字面量类型选择；无法确定时取第一个。 */
    private fun selectFunction(functions: List<SpelSymbol>, arguments: List<String>): SpelSymbol? {
        if (functions.isEmpty()) {
            return null
        }
        val countCompatible = functions.filter { acceptsCount(it, arguments.size) }
        if (countCompatible.isEmpty()) {
            return null
        }
        if (countCompatible.size == 1) {
            return countCompatible.first()
        }
        return countCompatible.firstOrNull { matchesArguments(it, arguments) } ?: countCompatible.first()
    }

    private fun acceptsCount(function: SpelSymbol, count: Int): Boolean {
        val method = function.declaration as? PsiMethod
        if (method != null) {
            return acceptsCount(method, count)
        }
        val parameterTypes = signatureParameterTypes(function.signature) ?: return true
        val varargs = parameterTypes.lastOrNull()?.endsWith("...") == true
        val fixed = if (varargs) parameterTypes.size - 1 else parameterTypes.size
        return if (varargs) count >= fixed else count == fixed
    }

    private fun matchesArguments(function: SpelSymbol, arguments: List<String>): Boolean {
        val method = function.declaration as? PsiMethod
        if (method != null) {
            return matchesArguments(method, arguments)
        }
        val parameterTypes = signatureParameterTypes(function.signature) ?: return false
        for ((index, argument) in arguments.withIndex()) {
            val parameter = parameterTypes.getOrNull(index)
                ?: parameterTypes.lastOrNull()?.takeIf { it.endsWith("...") } ?: return false
            if (matchesArgument(parameter.removeSuffix("..."), argument) == false) {
                return false
            }
        }
        return true
    }

    /** 方法重载：先按参数个数，再按字面量类型筛选；无法确定时取第一个。 */
    internal fun selectMethod(methods: Array<PsiMethod>, arguments: List<String>): PsiMethod? {
        if (methods.isEmpty()) {
            return null
        }
        val countCompatible = methods.filter { acceptsCount(it, arguments.size) }
        if (countCompatible.isEmpty()) {
            return null
        }
        if (countCompatible.size == 1) {
            return countCompatible.first()
        }
        return countCompatible.firstOrNull { matchesArguments(it, arguments) } ?: countCompatible.first()
    }

    private fun acceptsCount(method: PsiMethod, count: Int): Boolean {
        val parameters = method.parameterList.parameters
        val varargs = method.isVarArgs
        val fixed = if (varargs) parameters.size - 1 else parameters.size
        return if (varargs) count >= fixed else count == fixed
    }

    private fun matchesArguments(method: PsiMethod, arguments: List<String>): Boolean {
        val parameters = method.parameterList.parameters
        for ((index, argument) in arguments.withIndex()) {
            val parameter = parameters.getOrNull(index)
                ?: parameters.lastOrNull()?.takeIf { method.isVarArgs } ?: return false
            if (matchesArgument(parameter.type.presentableText, argument) == false) {
                return false
            }
        }
        return true
    }

    /** 字面量实参与形参类型是否肯定不兼容；引用类型与未知值返回 null（不做判断）。 */
    private fun matchesArgument(parameterType: String?, argument: String): Boolean? {
        val expected = parameterCategory(parameterType) ?: return null
        if (expected == AtomCategory.OTHER) {
            return null
        }
        val actual = literalCategory(argument) ?: return null
        return when (expected) {
            AtomCategory.STRING -> true
            AtomCategory.NUMBER -> actual == AtomCategory.NUMBER || actual == AtomCategory.NULL
            AtomCategory.BOOLEAN -> actual == AtomCategory.BOOLEAN || actual == AtomCategory.NULL
            else -> null
        }
    }

    private fun literalCategory(text: String): AtomCategory? {
        val value = text.trim()
        return when {
            (value.startsWith("'") && value.endsWith("'")) ||
                (value.startsWith("\"") && value.endsWith("\"")) -> AtomCategory.STRING
            NUMBER.matches(value) -> AtomCategory.NUMBER
            value == "true" || value == "false" -> AtomCategory.BOOLEAN
            value == "null" -> AtomCategory.NULL
            else -> null
        }
    }

    private fun parameterCategory(parameterType: String?): AtomCategory? {
        val name = parameterSimpleName(parameterType) ?: return null
        return when (name) {
            "String", "CharSequence" -> AtomCategory.STRING
            "boolean", "Boolean" -> AtomCategory.BOOLEAN
            "int", "Integer", "long", "Long", "short", "Short", "byte", "Byte",
            "float", "Float", "double", "Double",
            "BigInteger", "BigDecimal", "Number" -> AtomCategory.NUMBER
            else -> AtomCategory.OTHER
        }
    }

    private fun parameterSimpleName(type: String?): String? {
        var name = type?.trim() ?: return null
        name = name.substringBefore('<').trim()
        while (name.endsWith("[]")) {
            name = name.removeSuffix("[]").trim()
        }
        name = name.removeSuffix("...").trim()
        return name.substringAfterLast('.').trim().takeIf { it.isNotEmpty() && it != "?" }
    }

    /** 函数签名中的参数类型；签名缺失时返回 null。 */
    private fun signatureParameterTypes(signature: String?): List<String>? {
        val text = signature ?: return null
        val open = text.indexOf('(')
        val close = text.lastIndexOf(')')
        if (open < 0 || close <= open) {
            return null
        }
        return splitArguments(text.substring(open + 1, close), generics = true)
    }

    /** 按顶层逗号切分参数文本；实现与引用收集、补全共用 [SpelExpressionText]。 */
    private fun splitArguments(text: String, generics: Boolean = false): List<String> =
        SpelExpressionText.splitArguments(text, generics)

    /** 与 [close] 位置右方括号匹配的左方括号下标；方括号内的字符串不参与配对。 */
    private fun matchingOpenBracket(expr: String, close: Int): Int? {
        val stack = ArrayList<Int>()
        var quote: Char? = null
        var index = 0
        while (index <= close) {
            val current = expr[index]
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
                '[' -> stack.add(index)
                ']' -> {
                    if (stack.isEmpty()) {
                        return null
                    }
                    val open = stack.removeAt(stack.size - 1)
                    if (index == close) {
                        return open
                    }
                }
            }
            index++
        }
        return null
    }

    /**
     * 按全限定名、常用包与框架包、短名缓存依次查找类型。
     *
     * 短名只在内置符号里出现（`Request`、`MethodContext` 等），同名类
     * 可能来自任意依赖，因此兜底时优先 `com.luckyframework` 下的类。
     *
     * 类型推导把 `Object` 当作「无法确定」并返回 null，避免成员补全与检查
     * 把未知类型当成确定的 `java.lang.Object`；类型引用本身是明确的类，
     * 用 [findTypeReference] 查找。
     */
    fun findClass(typeName: String?): PsiClass? = findClass(typeName, includeObject = false)

    /**
     * 类型引用（`T(...)`、`new`、`instanceof T(...)`）的类查找。
     *
     * 与 [findClass] 的唯一区别：短名 `Object` 按 `java.lang.Object` 解析，
     * 保证这类明确写出的类型也能跳转、展示文档并参与检查。
     */
    fun findTypeReference(typeName: String?): PsiClass? = findClass(typeName, includeObject = true)

    private fun findClass(typeName: String?, includeObject: Boolean): PsiClass? {
        val cleaned = cleanTypeName(typeName, includeObject) ?: return null
        BOXED_TYPES[cleaned]?.let { return findClass(it, includeObject) }
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        facade.findClass(cleaned, scope)?.let { return it }
        // 全限定名（首段是包名，习惯小写）直查失败即不存在；
        // 短名内部类（`Thread.State`、`Map.Entry`）按常用包逐个尝试
        val firstSegment = cleaned.substringBefore('.')
        if (cleaned.contains('.') && (firstSegment.isEmpty() || !firstSegment.first().isUpperCase())) {
            return null
        }
        for (prefix in DEFAULT_PACKAGES) {
            facade.findClass("$prefix.$cleaned", scope)?.let { return it }
        }
        if (cleaned.contains('.')) {
            return null
        }
        val candidates = runCatching {
            PsiShortNamesCache.getInstance(project).getClassesByName(cleaned, scope)
        }.getOrElse { PsiClass.EMPTY_ARRAY }
        return candidates.firstOrNull { it.qualifiedName?.startsWith(LUCKY_FRAMEWORK_PREFIX) == true }
            ?: candidates.firstOrNull()
    }

    private fun cleanTypeName(typeName: String?, includeObject: Boolean): String? {
        var name = typeName?.trim() ?: return null
        while (name.endsWith("[]")) {
            name = name.removeSuffix("[]").trim()
        }
        if (name.endsWith("...")) {
            name = name.removeSuffix("...").trim()
        }
        val generic = name.indexOf('<')
        if (generic >= 0) {
            name = name.substring(0, generic).trim()
        }
        name = name.substringAfterLast(' ').trim()
        return name.takeIf {
            it.isNotEmpty() && it != "?" && it != "Any" && (includeObject || it != "Object")
        }
    }

    /** 最后一个顶层 `.`；跳过括号、字符串与小数中的点。 */
    private fun lastTopLevelDot(expr: String): Int {
        var depth = 0
        var quote: Char? = null
        var result = -1
        var index = 0
        while (index < expr.length) {
            val current = expr[index]
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
                '(', '[' -> depth++
                ')', ']' -> depth--
                // 小数点的两侧都是数字时属于数字字面量（`1.0`），不是成员访问
                '.' -> if (depth == 0 && !isDecimalPoint(expr, index)) result = index
            }
            index++
        }
        return result
    }

    private fun isDecimalPoint(expr: String, index: Int): Boolean =
        index > 0 && index + 1 < expr.length && expr[index - 1].isDigit() && expr[index + 1].isDigit()

    private companion object {
        val NUMBER = Regex("""\d+(?:\.\d+)?""")
        // `name`/`#name`/`$n`，以及 `name::type`/`$n::type` 类型引用
        val IDENTIFIER = Regex("""[A-Za-z_$][A-Za-z0-9_$]*(?:::type)?""")
        val DEFAULT_PACKAGES = listOf(
            "java.lang",
            "java.util",
            "java.util.function",
            "java.io",
            "java.lang.reflect",
            // 内置变量的类型名（`Request`、`MethodContext` 等）来自框架源码
            "com.luckyframework.httpclient.core.meta",
            "com.luckyframework.httpclient.core.executor",
            "com.luckyframework.httpclient.proxy.context",
            "com.luckyframework.httpclient.proxy.async",
            "com.luckyframework.httpclient.proxy.mock",
            "com.luckyframework.httpclient.proxy.sse.standard",
            "com.luckyframework.httpclient.proxy",
            "com.luckyframework.httpclient.generalapi.describe",
            // 参数 `::type` 的值类型
            "org.springframework.core"
        )
        private const val TYPE_SUFFIX = "::type"

        /** 选择/投影标记：`?[]`、`^[]`、`$[]` 与 `![]`。 */
        val SELECTION_MARKERS = setOf('?', '^', '$', '!')

        /** 二元运算符按优先级从低到高分组；同一组内长符号在前（`<=` 先于 `<`）。 */
        val BINARY_OPERATOR_LEVELS = listOf(
            listOf("||", "or"),
            listOf("&&", "and"),
            listOf(
                "==", "!=", "<=", ">=", "<", ">",
                "eq", "ne", "le", "lt", "ge", "gt",
                "matches", "between", "instanceof"
            ),
            listOf("+", "-"),
            listOf("*", "/", "%", "div", "mod"),
            listOf("^")
        )

        /** 结果一定是 boolean 的运算符（比较与逻辑），不依赖操作数类型。 */
        val BOOLEAN_OPERATORS = setOf(
            "||", "or", "&&", "and",
            "==", "!=", "<", "<=", ">", ">=",
            "eq", "ne", "lt", "le", "gt", "ge",
            "matches", "between", "instanceof"
        )

        /** 类型文本中的基本类型：数组组件需要保留基本类型本身（`byte[]`）。 */
        val PRIMITIVE_TYPES: Map<String, PsiType> = mapOf(
            "boolean" to PsiTypes.booleanType(),
            "byte" to PsiTypes.byteType(),
            "char" to PsiTypes.charType(),
            "short" to PsiTypes.shortType(),
            "int" to PsiTypes.intType(),
            "long" to PsiTypes.longType(),
            "float" to PsiTypes.floatType(),
            "double" to PsiTypes.doubleType(),
            "void" to PsiTypes.voidType()
        )

        /** 基本类型没有对应的 PsiClass，成员补全与类型检查统一按包装类处理。 */
        val BOXED_TYPES = mapOf(
            "boolean" to "Boolean",
            "byte" to "Byte",
            "char" to "Character",
            "short" to "Short",
            "int" to "Integer",
            "long" to "Long",
            "float" to "Float",
            "double" to "Double",
            "void" to "Void"
        )
    }
}

/** 框架类包前缀：短名解析与类型补全优先命中框架类，避免被同名依赖类抢走。 */
internal const val LUCKY_FRAMEWORK_PREFIX = "com.luckyframework."

/** 字面量与形参的粗粒度类别，仅用于重载筛选时排除确定冲突。 */
private enum class AtomCategory { STRING, NUMBER, BOOLEAN, NULL, OTHER }

/** 算术结果的数值类型；ordinal 递增表示更宽的类型（`int + long` → `Long`）。 */
private enum class NumericKind(val className: String) {
    INT("Integer"),
    LONG("Long"),
    BIG_INTEGER("BigInteger"),
    BIG_DECIMAL("BigDecimal"),
    FLOAT("Float"),
    DOUBLE("Double")
}
