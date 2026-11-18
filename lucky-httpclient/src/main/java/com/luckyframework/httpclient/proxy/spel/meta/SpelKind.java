package com.luckyframework.httpclient.proxy.spel.meta;

/**
 * SpEL属性的值形态。
 *
 * <p>用于描述一个属性值该按什么方式解析，由{@link SpelAttribute#kind()}声明。
 * 不存在"继承类级默认"的第三态，未声明时使用{@link #TEMPLATE}。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
public enum SpelKind {

    /**
     * 字符串模板。
     * <p>值可以包含多个{@code #{...}}表达式，解析结果为{@code String}。默认值。
     */
    TEMPLATE,

    /**
     * 键值对。
     * <p>按分隔符拆成key/value，再由{@link SpelAttribute#keySpel()}、
     * {@link SpelAttribute#valueSpel()}决定两部分是否参与求值；
     * 分隔符来自{@link SpelAttribute#separator()}或同注解内{@link SpelSeparator}标注的元素。
     */
    KEY_VALUE,

    /**
     * 框架自定义声明语法。
     * <p>当前{@code @SpELImport}归入{@link #KEY_VALUE}，本值保留给其他声明语法。
     */
    DECLARATION,

    /**
     * 条件表达式。
     * <p>解析结果必须是布尔值，常见于{@code enable}/{@code condition}类属性。
     */
    CONDITION,

    /**
     * 函数引用。
     * <p>值可以是函数名字面量，也可以是求值结果为函数名的SpEL表达式；
     * 函数的返回值须兼容{@link SpelAttribute#result()}。
     */
    FUNCTION,

    /**
     * 字面量。
     * <p>不解析SpEL，原样保存。
     */
    LITERAL,

    /**
     * 不参与SpEL解析。
     * <p>显式声明"该配置字段按字面量处理"，用于IDE在配置模型中建立完整字段清单。
     */
    NONE,

    /**
     * 单个SpEL表达式。
     * <p>整个值按SpEL解析，区别于{@link #TEMPLATE}的"字符串模板"语义。
     */
    EXPRESSION,

    /**
     * SpEL表达式列表。
     * <p>列表中的每个元素都是一条需要单独求值的表达式。
     */
    EXPRESSION_LIST,

    /**
     * Map参数。
     * <p>键与值都参与SpEL求值，集合值会展开为多个同名参数。
     */
    MAP_EXPRESSION,

    /**
     * 字面量数据。
     * <p>作为数据源暴露给SpEL使用，但自身不参与表达式解析。
     */
    DATA,

    /**
     * 嵌套配置结构。
     * <p>值本身不是表达式，其子字段由对应类型上的{@link SpelConfig}和
     * {@link SpelAttribute}继续描述。
     */
    STRUCT
}
