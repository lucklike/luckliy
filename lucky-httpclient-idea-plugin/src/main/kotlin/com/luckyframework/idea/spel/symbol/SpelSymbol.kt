package com.luckyframework.idea.spel.symbol

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.luckyframework.idea.spel.metadata.SpelLifecycle

/**
 * SpEL 符号类别，高亮、补全、跳转、文档与检查共用。
 */
enum class SpelSymbolKind {
    VARIABLE,
    ROOT_VARIABLE,
    FUNCTION,
    NAMESPACE,
    TYPE,
    BEAN
}

/**
 * 符号来源，按解析优先级从高到低排列。
 *
 * [CLASS_STATIC] 表示当前类（含父类、父接口）声明的 public static 方法：
 * 运行期 `ClassContext.loadClassSpELFun` 会把它们注册为 SpEL 函数，
 * 可以直接以 `chunk_success(...)` 的形式调用。
 */
enum class SpelSymbolOrigin {
    PARAMETER,
    METHOD_IMPORT,
    CLASS_IMPORT,
    CLASS_STATIC,
    BUILTIN,
    GLOBAL,
    UNKNOWN
}

/**
 * 符号在当前表达式阶段是否可见。
 */
enum class SpelVisibility {
    AVAILABLE,
    STAGE_UNAVAILABLE
}

/**
 * 一个可解析的 SpEL 符号。
 *
 * [declaration] 是跳转目标；[sourceRange] 是声明在 [declaration] 内的相对范围
 * （例如 `@SpELImport(var = "token=...")` 中 `token` 的位置）。
 * [lifecycle] 表示符号从哪个阶段开始可用；[visibility] 由所在上下文阶段填充。
 * [owner] 是函数/命名空间的所属类，供悬停文档展示。
 * [registered] 为 false 的内置函数未被 `HttpClientProxyObjectFactory` 注册
 * （如 `BankCardGenerator`），需要 `@SpELImport` 导入后才可用。
 * [methodName] 是函数对应的真实方法名（可带 `@FunctionAlias` 别名，如 `str_ne` → `strNe`），
 * 内置函数没有 PSI 声明，跳转时按它到 [owner] 类中定位。
 */
data class SpelSymbol(
    val name: String,
    val kind: SpelSymbolKind,
    val type: String? = null,
    val origin: SpelSymbolOrigin = SpelSymbolOrigin.UNKNOWN,
    val declaration: PsiElement? = null,
    val lifecycle: SpelLifecycle = SpelLifecycle.REQUEST_INIT,
    val visibility: SpelVisibility = SpelVisibility.AVAILABLE,
    val sourceRange: TextRange? = null,
    val signature: String? = null,
    val namespace: String? = null,
    val owner: String? = null,
    val description: String? = null,
    val registered: Boolean = true,
    val methodName: String? = null
) {
    val isRoot: Boolean
        get() = kind == SpelSymbolKind.ROOT_VARIABLE
}
