package com.luckyframework.idea.spel.metadata

/**
 * 注解属性中 SpEL 的书写形式。
 *
 * - [TEMPLATE] 整个字符串是 SpEL 模板，可以包含多个 `#{}` 片段；
 * - [KEY_VALUE] 按分隔符拆成键和值，键和值都可能包含 SpEL；
 * - [IF_CONDITION] 条件表达式，结果按布尔值使用；
 * - [NONE] 不是 SpEL；
 * - [UNKNOWN] 尚未确认，不参与未解析检查。
 */
enum class SpelValueKind {
    TEMPLATE,
    KEY_VALUE,
    IF_CONDITION,
    NONE,
    UNKNOWN
}

/**
 * 属性在请求生命周期中的执行阶段。
 *
 * 简化自框架的 16 个阶段，只保留与变量可见性相关的粒度。
 */
enum class SpelLifecycle {
    REQUEST_INIT,
    RESPONSE_INIT,
    RESPONSE,
    RETRY,
    THROWABLE,
    METHOD_RESULT,
    METHOD,
    UNKNOWN
}

data class SpelAttributeMetadata(
    val kind: SpelValueKind,
    val lifecycle: SpelLifecycle
)

data class SpelAnnotationMetadata(
    val fqName: String,
    val attributes: Map<String, SpelAttributeMetadata>
) {
    fun attribute(name: String): SpelAttributeMetadata? = attributes[name]
}

/**
 * 注解元数据表，由生成脚本从框架源码提取并人工确认。
 */
class AnnotationTable(
    val tableVersion: String,
    val frameworkVersion: String,
    private val annotations: Map<String, SpelAnnotationMetadata>
) {
    fun metadataFor(annotationFqName: String): SpelAnnotationMetadata? = annotations[annotationFqName]

    fun attribute(annotationFqName: String, attribute: String): SpelAttributeMetadata? =
        annotations[annotationFqName]?.attribute(attribute)

    fun all(): Map<String, SpelAnnotationMetadata> = annotations

    companion object {
        @JvmStatic
        val EMPTY = AnnotationTable("0", "unknown", emptyMap())
    }
}
