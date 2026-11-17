package com.luckyframework.httpclient.proxy.spel.meta;

/**
 * SpEL表达式结果的粗粒度约束。
 *
 * <p>用于{@code Class<?>}无法精确表达的形态约束，由{@link SpelAttribute#constraint()}声明。
 * 与{@link SpelAttribute#result()}配合：{@code result}为空而{@code constraint}非空时，
 * 本约束作为独立约束生效。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
public enum SpelConstraint {

    /**
     * 不限制形态。为约束数组缺省时的默认语义。
     */
    ANY,

    /**
     * 布尔值。
     */
    BOOLEAN,

    /**
     * 任意数值。
     */
    NUMBER,

    /**
     * 整数。
     */
    INTEGER,

    /**
     * 字符串。
     */
    STRING,

    /**
     * 二进制形态，例如{@code byte[]}、{@code InputStream}、{@code File}等。
     */
    BINARY,

    /**
     * 类型元数据，例如{@code Type}、{@code Class}、{@code ResolvableType}、
     * {@code SerializationTypeToken}等。
     */
    TYPE,

    /**
     * 类对象。
     */
    CLASS
}
