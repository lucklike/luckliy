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
import com.luckyframework.idea.spel.symbol.SpelSymbolKind

/**
 * 轻量 SpEL 类型推导（design D7）。
 *
 * 支持变量/函数/命名空间符号的内置或导入类型、`T(...)` 类型引用、
 * 字符串与数字字面量，以及字段访问和方法调用的返回值；无法推断时返回 null，
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

    /** 解析表达式（如 `#user`、`#user.address`、`#time()`）的静态类型。 */
    fun resolve(expression: String): PsiClass? {
        val expr = expression.trim()
        if (expr.isEmpty()) {
            return null
        }
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

    private fun symbolClass(name: String): PsiClass? {
        val symbol = context.symbols.firstOrNull {
            it.name == name && it.kind in SYMBOL_KINDS
        } ?: return null
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
        return candidates.firstOrNull { it.qualifiedName?.startsWith(FRAMEWORK_PREFIX) == true }
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
        val IDENTIFIER = Regex("""[A-Za-z_$][A-Za-z0-9_$]*""")
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
            "com.luckyframework.httpclient.proxy"
        )
        const val FRAMEWORK_PREFIX = "com.luckyframework."
    }
}
