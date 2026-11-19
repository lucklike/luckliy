package com.luckyframework.idea.debug.invoke

/**
 * 方法参数的调试元数据。
 *
 * typeName 是擦除泛型后的全限定名，launcher 侧用它定位重载；
 * kind 决定表单控件与类型标签，UNSUPPORTED 表示 P1 不支持的参数类型。
 */
data class DebugParameter(
    val index: Int,
    val name: String,
    val typeName: String,
    val kind: DebugParameterKind,
    val enumConstants: List<String> = emptyList(),
    val required: Boolean = true,
    val unsupportedReason: String? = null
)

enum class DebugParameterKind {
    STRING,
    BOOLEAN,
    INT,
    LONG,
    DOUBLE,
    CHAR,
    DECIMAL,
    BIGINT,
    ENUM,
    FILE,
    BYTES,
    LIST,
    MAP,
    OBJECT,
    UNSUPPORTED
}
