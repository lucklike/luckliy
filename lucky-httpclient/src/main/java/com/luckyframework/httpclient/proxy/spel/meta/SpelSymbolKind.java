package com.luckyframework.httpclient.proxy.spel.meta;

/**
 * SpEL符号的类型。
 *
 * <p>由{@link SpelSymbol#kind()}声明；{@link #AUTO}表示按符号所在位置推导。
 * 注解类型上的符号由{@link SpelSymbol#kind()}显式指定，默认按{@link #PARAM}处理。
 *
 * <p>函数不在本枚举的声明范围内：函数是否可用及其生命周期由{@code @SPELImport}与
 * {@code HttpClientProxyObjectFactory#addSpringElFunctionClass(Class)}决定，
 * 不使用{@link SpelSymbol}标注函数方法。{@link #FUNCTION}只用于描述函数名常量。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
public enum SpelSymbolKind {

    /**
     * 自动推导。推导规则：
     * <ul>
     *     <li>字段常量：以{@code $}开头且以{@code $}结尾 → {@link #INTERNAL}，否则 → {@link #VAR}；</li>
     *     <li>参数：→ {@link #PARAM}；</li>
     *     <li>Spring{@code @Component}/{@code @Bean}：→ {@link #BEAN}。</li>
     * </ul>
     * <p>方法不参与推导：函数不通过{@link SpelSymbol}标注。
     */
    AUTO,

    /**
     * Root变量，表达式中以{@code name}形式引用。
     */
    ROOT,

    /**
     * 普通变量，表达式中以{@code #name}形式引用。
     */
    VAR,

    /**
     * Spring Bean，表达式中以{@code @name}形式引用。
     */
    BEAN,

    /**
     * 环境变量，表达式中以{@code ${name}}形式引用。
     */
    ENV,

    /**
     * 框架内置变量，指以{@code $...$}形式命名的框架保留变量。
     */
    INTERNAL,

    /**
     * 函数名常量。只在需要描述一个函数名常量的类型时使用；
     * 函数方法本身不使用{@link SpelSymbol}标注，可用性与生命周期
     * 由{@code @SPELImport}与{@code HttpClientProxyObjectFactory#addSpringElFunctionClass(Class)}决定。
     */
    FUNCTION,

    /**
     * 参数注解声明的变量。
     */
    PARAM
}
