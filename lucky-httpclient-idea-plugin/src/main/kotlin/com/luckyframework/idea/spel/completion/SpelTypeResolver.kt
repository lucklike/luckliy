package com.luckyframework.idea.spel.completion

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.luckyframework.idea.spel.symbol.SpelContext
import com.luckyframework.idea.spel.symbol.SpelSymbol
import com.luckyframework.idea.spel.symbol.SpelSymbolKind

/**
 * 轻量 SpEL 类型推导（design D7）。
 *
 * 支持变量/函数/命名空间符号的内置或导入类型、`T(...)` 类型引用、
 * 字符串与数字字面量、字段访问、方法调用与函数调用的返回值；无法推断时返回 null，
 * 由成员补全降级为通用成员。
 *
 * 有 PSI 声明时直接取声明类型（准确区分同名类与泛型）；内置符号只有
 * 短类型名（如 `Request`），按框架包优先的顺序查找，避免命中依赖里同名
 * 的无关类（如 `okhttp3.Request`）。
 */
class SpelTypeResolver(
    private val project: Project,
    private val context: SpelContext
) {

    /** 解析表达式（如 `#user`、`#user.address`、`#time()`、`add(1, 2)`）的静态类型。 */
    fun resolve(expression: String): PsiClass? {
        val expr = expression.trim()
        if (expr.isEmpty()) {
            return null
        }
        // 整体是函数调用时按返回值解析：`fn(1)`、`#fn(1)`、`#ns.fn(1)`
        functionCallClass(expr)?.let { return it }
        val dot = lastTopLevelDot(expr)
        if (dot >= 0) {
            val receiver = resolve(expr.substring(0, dot)) ?: return null
            return memberType(receiver, expr.substring(dot + 1).trim())
        }
        return resolveAtom(expr)
    }

    private fun resolveAtom(expr: String): PsiClass? = when {
        expr.startsWith("T(") && expr.endsWith(")") -> findClass(expr.substring(2, expr.length - 1))
        expr.startsWith("'") || expr.startsWith("\"") -> findClass("String")
        NUMBER.matches(expr) -> findClass(if (expr.contains('.')) "Double" else "Integer")
        expr.startsWith("#") -> symbolClass(expr.removePrefix("#").removeSuffix("()").trim())
        IDENTIFIER.matches(expr) -> symbolClass(expr)
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
    private fun functionCallClass(expr: String): PsiClass? {
        if (!expr.endsWith(")")) {
            return null
        }
        val open = matchingOpenParen(expr, expr.lastIndex) ?: return null
        if (open <= 0) {
            return null
        }
        val (namespace, name) = callableName(expr.substring(0, open).trim()) ?: return null
        val function = context.functions(name, namespace)
            .firstOrNull { !it.type.isNullOrBlank() } ?: return null
        return functionReturnClass(function)
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

    private fun functionReturnClass(function: SpelSymbol): PsiClass? {
        psiTypeClass(function.declaration)?.let { return it }
        return findClass(function.type)
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

    private fun symbolClass(name: String): PsiClass? {
        val symbol = context.symbols.firstOrNull {
            it.name == name && it.kind in SYMBOL_KINDS
        } ?: return null
        // `name::type`/`$n::type` 的值是 ResolvableType 本体，不能按参数声明类型解析
        if (name.endsWith(TYPE_SUFFIX)) {
            return findClass(symbol.type)
        }
        // 导入/类静态函数等有 PSI 声明时按真实类型解析
        psiTypeClass(symbol.declaration)?.let { return it }
        return findClass(symbol.type)
    }

    private fun psiTypeClass(element: PsiElement?): PsiClass? = when (element) {
        is PsiMethod -> resolveType(element.returnType)
        is PsiParameter -> resolveType(element.type)
        is PsiField -> resolveType(element.type)
        else -> null
    }

    private fun resolveType(type: PsiType?): PsiClass? {
        val classType = type as? PsiClassType ?: return findClass(type?.presentableText)
        return classType.resolve() ?: findClass(type.presentableText)
    }

    private fun memberType(owner: PsiClass, segment: String): PsiClass? {
        if (segment.isEmpty()) {
            return owner
        }
        if (segment.endsWith(")")) {
            val methodName = segment.substringBefore('(').trim()
            val method = owner.findMethodsByName(methodName, true).firstOrNull() ?: return null
            return resolveType(method.returnType)
        }
        val field = owner.findFieldByName(segment, true) ?: return null
        return resolveType(field.type)
    }

    /**
     * 按全限定名、常用包与框架包、短名缓存依次查找类型。
     *
     * 短名只在内置符号里出现（`Request`、`MethodContext` 等），同名类
     * 可能来自任意依赖，因此兜底时优先 `com.luckyframework` 下的类。
     */
    fun findClass(typeName: String?): PsiClass? {
        val cleaned = cleanTypeName(typeName) ?: return null
        BOXED_TYPES[cleaned]?.let { return findClass(it) }
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        facade.findClass(cleaned, scope)?.let { return it }
        if (cleaned.contains('.')) {
            return null
        }
        for (prefix in DEFAULT_PACKAGES) {
            facade.findClass("$prefix.$cleaned", scope)?.let { return it }
        }
        val candidates = runCatching {
            PsiShortNamesCache.getInstance(project).getClassesByName(cleaned, scope)
        }.getOrElse { PsiClass.EMPTY_ARRAY }
        return candidates.firstOrNull { it.qualifiedName?.startsWith(LUCKY_FRAMEWORK_PREFIX) == true }
            ?: candidates.firstOrNull()
    }

    private fun cleanTypeName(typeName: String?): String? {
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
        return name.takeIf { it.isNotEmpty() && it != "?" && it != "Object" && it != "Any" }
    }

    /** 最后一个顶层 `.`；跳过括号与字符串中的点。 */
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
                '.' -> if (depth == 0) result = index
            }
            index++
        }
        return result
    }

    private companion object {
        val NUMBER = Regex("""\d+(?:\.\d+)?""")
        // `name`/`#name`/`$n`，以及 `name::type`/`$n::type` 类型引用
        val IDENTIFIER = Regex("""[A-Za-z_$][A-Za-z0-9_$]*(?:::type)?""")
        val SYMBOL_KINDS = setOf(
            SpelSymbolKind.VARIABLE,
            SpelSymbolKind.ROOT_VARIABLE,
            SpelSymbolKind.FUNCTION,
            SpelSymbolKind.NAMESPACE,
            SpelSymbolKind.BEAN
        )
        val DEFAULT_PACKAGES = listOf(
            "java.lang",
            "java.util",
            "java.io",
            "java.lang.reflect",
            // 内置变量的类型名（`Request`、`MethodContext` 等）来自框架源码
            "com.luckyframework.httpclient.core.meta",
            "com.luckyframework.httpclient.proxy.context",
            "com.luckyframework.httpclient.proxy",
            // 参数 `::type` 的值类型
            "org.springframework.core"
        )
        private const val TYPE_SUFFIX = "::type"

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
