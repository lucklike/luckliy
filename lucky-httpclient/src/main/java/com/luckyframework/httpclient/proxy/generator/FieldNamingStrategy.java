package com.luckyframework.httpclient.proxy.generator;

/**
 * 字段命名策略枚举
 */
public enum FieldNamingStrategy {
    ORIGINAL,    // 保持原始字段名（不下划线转驼峰）
    CAMEL_CASE   // 转换为驼峰命名（下划线转驼峰）
}
