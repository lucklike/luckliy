package com.luckyframework.httpclient.proxy.spel.meta;

/**
 * 配置字段名到配置key的命名策略。
 *
 * <p>由{@link SpelConfig#naming()}声明，作用于同一个配置模型内的所有字段。
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2026/11/16
 */
public enum SpelNaming {

    /**
     * 保持原样，例如{@code maxCount} → {@code maxCount}。
     */
    AS_IS,

    /**
     * 小驼峰，例如{@code max-count} → {@code maxCount}。
     */
    CAMEL_CASE,

    /**
     * 短横线连接，例如{@code maxCount} → {@code max-count}。
     */
    KEBAB_CASE,

    /**
     * 下划线连接，例如{@code maxCount} → {@code max_count}。
     */
    SNAKE_CASE
}
