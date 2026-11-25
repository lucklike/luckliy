package com.luckyframework.idea.spel.symbol

import com.intellij.psi.PsiType
import com.luckyframework.idea.spel.metadata.ResolvedSpelAttribute
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 一个 SpEL 片段可用的符号视图。
 *
 * [known] 为 false 表示表达式不在 lucky-httpclient 上下文中（普通字符串，
 * 或注解属性未标注 `@SpelAttribute`/声明为 LITERAL、NONE、UNKNOWN）：
 * 运行期不会被求值，补全、引用、高亮与所有检查器都完全不参与。
 */
data class SpelContext(
    val known: Boolean,
    val stage: SpelLifecycle,
    val attribute: ResolvedSpelAttribute?,
    val symbols: List<SpelSymbol>,
    /**
     * 静态 `false` 条件导入未生效的类提供的符号：运行期不存在，
     * 只用于诊断「类未被导入」，不参与补全、解析与类型推导。
     */
    val disabledSymbols: List<SpelSymbol> = emptyList(),
    /**
     * 表达式结果的期望类型。YAML 配置中取命中的 `@SpelConfig` 字段
     * （`resultType` 优先，否则 `valueType`）；Java 注解路径为 null。
     */
    val expectedType: PsiType? = null,
    /**
     * 参数注入字面量找不到可静态定位的调用方（注入源方法未知）：
     * 符号表不含函数自身的参数（它们不是注入源），未解析检查应跳过，
     * 避免把仅在外部调用的函数参数误报为未解析。
     */
    val injectionSourceUnknown: Boolean = false
) {

    /** 条件导入未生效的同名符号；不存在时返回 null。 */
    fun disabled(name: String, kind: SpelSymbolKind): SpelSymbol? =
        disabledSymbols.firstOrNull { it.name == name && it.kind == kind }

    fun find(name: String, kind: SpelSymbolKind? = null): List<SpelSymbol> =
        symbols.filter { it.name == name && (kind == null || it.kind == kind) }

    fun available(): List<SpelSymbol> =
        symbols.filter { it.visibility == SpelVisibility.AVAILABLE }

    fun ofKind(vararg kinds: SpelSymbolKind): List<SpelSymbol> =
        symbols.filter { it.kind in kinds }

    /**
     * `#name` 形式的普通变量（`@SpELImport(var = ...)`、内置 `variables` 等）。
     *
     * Root 变量不在此列：它们运行期注册在 Root 变量空间，只能用裸名称引用。
     */
    fun variables(name: String): List<SpelSymbol> =
        symbols.filter { it.name == name && it.kind == SpelSymbolKind.VARIABLE }

    /**
     * 裸 `name` 形式的 Root 变量（方法参数、`@SpELImport(root = ...)`、内置 Root 变量）。
     *
     * 这些变量只能用裸名称引用，`#name` 在运行期解析不到。
     */
    fun rootVariables(name: String): List<SpelSymbol> =
        symbols.filter { it.name == name && it.kind == SpelSymbolKind.ROOT_VARIABLE }

    /** 同名变量不区分书写形式，用于提示「换一种写法即可解析」。 */
    fun anyVariables(name: String): List<SpelSymbol> =
        symbols.filter {
            it.name == name && (it.kind == SpelSymbolKind.VARIABLE || it.kind == SpelSymbolKind.ROOT_VARIABLE)
        }

    /**
     * 函数视图：无命名空间函数与内置函数一样可以直接以平铺形式调用
     * （运行期 MethodSpaceMethodResolver 会从函数空间与变量表两处查找），
     * 因此未指定命名空间时返回全部同名函数；指定时按命名空间过滤。
     *
     * [includeBuiltin] 为 false 时按书写形式过滤 `#name(...)`：
     * - 未注册的内置函数在 `__Func::*` 函数空间，`#name(...)` 运行期不可用，排除；
     * - 注册函数与 `@SpELImport` 导入的函数在变量表有平铺 `Method`，`#name(...)` 可用；
     * - 带命名空间的函数只能写 `#ns.name(...)`，由 [namespace] 精确匹配，不受此参数影响。
     */
    fun functions(
        name: String,
        namespace: String? = null,
        includeBuiltin: Boolean = true
    ): List<SpelSymbol> =
        symbols.filter { symbol ->
            if (symbol.kind != SpelSymbolKind.FUNCTION || symbol.name != name) {
                return@filter false
            }
            if (namespace != null && symbol.namespace != namespace) {
                return@filter false
            }
            if (includeBuiltin || namespace != null) {
                return@filter true
            }
            // 裸 `name(...)` 之外只能用 `#name(...)`：变量表里没有平铺 Method 的内置函数排除
            symbol.namespace == null &&
                (symbol.origin != SpelSymbolOrigin.BUILTIN || symbol.registered)
        }

    fun namespaces(name: String): List<SpelSymbol> =
        symbols.filter { it.kind == SpelSymbolKind.NAMESPACE && it.name == name }

    /**
     * 纯函数名可用的函数（`kind = FUNCTION` 属性值只写函数名时的解析目标）。
     *
     * 运行期 `Context#autoInjectParamExecuteFunction` 先解析属性值，不含 `#{}` 时
     * 原样得到函数名，再用 `getVar(name, Method.class)`（即 `#name`）从变量表
     * 按平铺名取 `Method`。因此只有平铺注册的函数可用：
     * - 带 `@Namespace` 的类注册为 `var[namespace] = Map<name, Method>`
     *   （`ClassStaticElement#getAllStaticMethods`），平铺名不存在，只能 `#ns.name`；
     * - 内置函数注册在 `__Func::*` 函数空间，且未注册的内置函数（`registered = false`）
     *   根本不在变量表里。
     *
     * 与 [functions] 的区别：后者按 SpEL 表达式里的函数调用解析，内置函数与命名空间
     * 函数都能命中；而纯函数名是变量表查询，两者都不可用。
     */
    fun functionNames(name: String): List<SpelSymbol> =
        symbols.filter {
            it.kind == SpelSymbolKind.FUNCTION && it.name == name &&
                it.namespace == null && it.registered
        }

    /** 可作为纯函数名使用的全部函数（供补全）；带 `@Namespace` 与内置函数不参与。 */
    fun availableFunctionNames(): List<SpelSymbol> =
        available().filter {
            it.kind == SpelSymbolKind.FUNCTION && it.namespace == null && it.registered
        }

    companion object {
        /** 不属于 SpEL 求值上下文的字面量：完全不解析（无引用、补全、高亮与检查）。 */
        fun unknown(): SpelContext =
            SpelContext(false, SpelLifecycle.UNKNOWN, null, emptyList())
    }
}
