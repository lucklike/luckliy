package com.luckyframework.idea.spel.symbol

import com.luckyframework.idea.spel.metadata.ResolvedSpelAttribute
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * 一个 SpEL 片段可用的符号视图。
 *
 * [known] 为 false 时表示表达式不在 lucky-httpclient 上下文中：
 * 只做语法高亮，不产生未解析告警。
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
    val disabledSymbols: List<SpelSymbol> = emptyList()
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
     * 函数视图：命名空间函数与内置函数一样可以直接以平铺形式调用
     * （运行期 MethodSpaceMethodResolver 会从函数空间与变量表两处查找），
     * 因此未指定命名空间时返回全部同名函数；指定时按命名空间过滤。
     *
     * [includeBuiltin] 为 false 时排除框架内置函数：内置函数只支持裸 `name(...)`
     * 调用，`#name(...)` 在运行期不可用，引用与检查需要按书写形式过滤。
     */
    fun functions(
        name: String,
        namespace: String? = null,
        includeBuiltin: Boolean = true
    ): List<SpelSymbol> =
        symbols.filter { symbol ->
            symbol.kind == SpelSymbolKind.FUNCTION && symbol.name == name &&
                (namespace == null || symbol.namespace == namespace) &&
                (includeBuiltin || symbol.origin != SpelSymbolOrigin.BUILTIN)
        }

    fun namespaces(name: String): List<SpelSymbol> =
        symbols.filter { it.kind == SpelSymbolKind.NAMESPACE && it.name == name }

    companion object {
        fun unknown(): SpelContext =
            SpelContext(false, SpelLifecycle.UNKNOWN, null, emptyList())
    }
}
