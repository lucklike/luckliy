package com.luckyframework.idea.spel.symbol

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
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
 * [resolvedType] 是上下文推导出的精确类型（如 `$result$` 拆包后的泛型实参、
 * `$body$` 的 `@ConvertMetaType` 目标类型），类型解析优先于 [declaration] 的声明类型。
 * [jumpTarget] 是跳转优先目标：参数名来自 `@Param` 系列注解时指向注解中
 * 书写名称的属性值；`$result$`/`$body$` 能推导出类型时指向类型在源码中的
 * 书写位置（返回类型引用、注解中的类字面量）。此时 [declaration] 仍保留原值，
 * 供类型推导与重命名使用。
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
    val methodName: String? = null,
    val resolvedType: PsiType? = null,
    val jumpTarget: PsiElement? = null
) {
    val isRoot: Boolean
        get() = kind == SpelSymbolKind.ROOT_VARIABLE
}
