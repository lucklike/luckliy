package com.luckyframework.idea.spel.metadata

/**
 * 注解属性中 SpEL 的书写形式，对应框架的 `SpelKind`。
 *
 * - [TEMPLATE] 整个字符串是 SpEL 模板，可以包含多个 `#{}` 片段；
 * - [KEY_VALUE] 按分隔符拆成键和值，键和值都可能包含 SpEL；
 * - [DECLARATION] 声明式字面量（例如变量声明），由框架按声明语法解析；
 * - [CONDITION] 条件表达式，结果按布尔值使用；
 * - [FUNCTION] 函数名；
 * - [LITERAL] 纯字面量，不参与求值；
 * - [NONE] 不是 SpEL；
 * - [EXPRESSION] 整串是一个 SpEL 表达式（不要求 `#{}` 包裹）；
 * - [EXPRESSION_LIST] 列表，每个元素是表达式；
 * - [MAP_EXPRESSION] Map，键与值都可能参与求值，由 `keySpel` 单独声明键是否求值；
 * - [DATA] 字面量数据，不参与求值；
 * - [STRUCT] 结构化嵌套配置，值继续按配置模型下探，本身不是表达式；
 * - [UNKNOWN] 尚未确认，不参与未解析检查。
 */
enum class SpelValueKind {
    TEMPLATE,
    KEY_VALUE,
    DECLARATION,
    CONDITION,
    FUNCTION,
    LITERAL,
    NONE,
    EXPRESSION,
    EXPRESSION_LIST,
    MAP_EXPRESSION,
    DATA,
    STRUCT,
    UNKNOWN;

    companion object {
        /**
         * 值中可以出现 SpEL 片段的类型（白名单）。
         *
         * 白名单之外的 `LITERAL`/`NONE`/`DATA`/`STRUCT`/`UNKNOWN` 运行期
         * 不会把值当作表达式求值：`STRUCT` 是子配置、`DATA` 是原始数据，
         * 其余明确声明为不解析 SpEL。
         */
        val EXPRESSION_KINDS: Set<SpelValueKind> = setOf(
            TEMPLATE,
            KEY_VALUE,
            DECLARATION,
            CONDITION,
            FUNCTION,
            EXPRESSION,
            EXPRESSION_LIST,
            MAP_EXPRESSION
        )
    }
}

/**
 * 属性在请求生命周期中的执行阶段，对应框架 `Lifecycle` 的 16 个阶段。
 *
 * 枚举声明顺序即阶段先后顺序，[com.luckyframework.idea.spel.symbol.LifecycleTable]
 * 直接按 ordinal 比较可见性。
 *
 * [METHOD] 同时是「未声明阶段」的哨兵值：未写 `stage` 的属性按它处理，不参与阶段过滤。
 * [NON] 表示「不属于任何生命周期」，只用于 `@SpelSymbol#until` 的默认值。
 */
enum class SpelLifecycle {
    CLASS,
    METHOD_META,
    METHOD,
    REQUEST_INIT,
    CONFIG_API_INIT,
    CONFIG_API_METHOD_INIT,
    CONFIG_API_INIT_COMPLETE,
    REQUEST,
    INVOKE_WRAPPER_METHOD,
    RETRY,
    RESPONSE_INIT,
    RESPONSE,
    METHOD_RESULT,
    THROWABLE,
    DESTROY,
    NON,
    UNKNOWN
}

/**
 * 单个注解元素（或配置字段）的 SpEL 语义，来自 `@SpelAttribute`。
 *
 * [explicit] 记录注解里显式书写的属性名：为空表示全部取默认值，
 * 阶段/分隔符据此判断是否回退到 `@SpelStage`/`@SpelSeparator` 声明的元素。
 */
data class SpelAttributeMetadata(
    val kind: SpelValueKind,
    val lifecycle: SpelLifecycle,
    /** 注解里显式书写的属性名（不含单值写法的 `value`）。 */
    val explicit: Set<String> = emptySet(),
    /** `result` 声明里 class 字面量能表达的类型候选。 */
    val result: List<String> = emptyList(),
    /** `typeExpression` 声明里的完整类型表达式候选。 */
    val typeExpression: List<String> = emptyList(),
    /** 粗粒度形态约束；空表示不限。 */
    val constraint: List<String> = emptyList(),
    /** 显式书写的字面量分隔符，`KEY_VALUE` 使用。 */
    val separator: String = "",
    /** key 部分是否参与 SpEL 求值。 */
    val keySpel: Boolean = false,
    /** value 部分是否参与 SpEL 求值。 */
    val valueSpel: Boolean = true,
    /** 静态生效条件，例如 `enable != false`。 */
    val condition: String = "",
    /** 配置 key；空串表示由字段名与所在模型的 `SpelConfig#naming()` 推导。 */
    val key: String = "",
    /** 单个结果类型（`Object` 视为未指定，读取时已归一为 null）。 */
    val resultType: String? = null,
    /** 集合值是否展开为多个同名参数。 */
    val expandable: Boolean = false
)

/**
 * 一个注解的 SpEL 元数据。
 *
 * [separatorElement]/[stageElement]/[nameElement] 分别由 `@SpelSeparator`、
 * `@SpelStage`、`@SpelName` 标注，表示「分隔符/阶段/名称」取自哪个元素在
 * 使用处的实际值。
 */
data class SpelAnnotationMetadata(
    val fqName: String,
    val attributes: Map<String, SpelAttributeMetadata>,
    val separatorElement: String? = null,
    val stageElement: String? = null,
    val nameElement: String? = null
) {
    fun attribute(name: String): SpelAttributeMetadata? = attributes[name]
}

/** `@LuckyComponent` 声明的组件身份。 */
data class LuckyComponentMetadata(
    val className: String,
    val title: String,
    /** `AUTO`、`API`、`CONFIG`、`FUNCTION`、`SUPPORT`。 */
    val type: String,
    val declared: Boolean,
    val source: String,
    /** `@SpelConfig` 的配置前缀。 */
    val prefix: String? = null,
    /** `@SpelConfig` 的命名方式。 */
    val naming: String? = null
)
