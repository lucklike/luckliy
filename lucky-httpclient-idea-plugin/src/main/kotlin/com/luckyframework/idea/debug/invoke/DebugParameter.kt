package com.luckyframework.idea.debug.invoke

/**
 * 方法参数（或对象字段）的调试元数据。
 *
 * typeName 是擦除泛型后的全限定名，launcher 侧用它定位重载；
 * kind 决定表单控件与类型标签，UNSUPPORTED 表示 P1 不支持的参数类型。
 * [fields] 让对象参数在表单里直接展开成字段输入框，[element] 描述列表元素，
 * 两者共同支撑 JSON 模式的参数骨架；[description] 来自 Javadoc 或文档注解。
 */
data class DebugParameter(
    val index: Int,
    val name: String,
    val typeName: String,
    val kind: DebugParameterKind,
    val enumConstants: List<String> = emptyList(),
    val required: Boolean = true,
    val unsupportedReason: String? = null,
    /** 参数/字段说明；没有文档时为 null。 */
    val description: String? = null,
    /** OBJECT 的可解析字段；为空表示按原始 JSON 编辑。 */
    val fields: List<DebugParameter> = emptyList(),
    /** LIST 的元素结构；元素类型无法解析时为 null。 */
    val element: DebugParameter? = null
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
