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
    val symbols: List<SpelSymbol>
) {

    fun find(name: String, kind: SpelSymbolKind? = null): List<SpelSymbol> =
        symbols.filter { it.name == name && (kind == null || it.kind == kind) }

    fun available(): List<SpelSymbol> =
        symbols.filter { it.visibility == SpelVisibility.AVAILABLE }

    fun ofKind(vararg kinds: SpelSymbolKind): List<SpelSymbol> =
        symbols.filter { it.kind in kinds }

    /** `#name` 与裸标识符共用的变量视图（普通变量与 Root 变量）。 */
    fun variables(name: String): List<SpelSymbol> =
        symbols.filter {
            it.name == name && (it.kind == SpelSymbolKind.VARIABLE || it.kind == SpelSymbolKind.ROOT_VARIABLE)
        }

    /**
     * 函数视图：命名空间函数与内置函数一样可以直接以平铺形式调用
     * （运行期 MethodSpaceMethodResolver 会从函数空间与变量表两处查找），
     * 因此未指定命名空间时返回全部同名函数；指定时按命名空间过滤。
     */
    fun functions(name: String, namespace: String? = null): List<SpelSymbol> =
        symbols.filter { symbol ->
            symbol.kind == SpelSymbolKind.FUNCTION && symbol.name == name &&
                (namespace == null || symbol.namespace == namespace)
        }

    fun namespaces(name: String): List<SpelSymbol> =
        symbols.filter { it.kind == SpelSymbolKind.NAMESPACE && it.name == name }

    companion object {
        fun unknown(): SpelContext =
            SpelContext(false, SpelLifecycle.UNKNOWN, null, emptyList())
    }
}
