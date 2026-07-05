package com.luckyframework.httpclient.proxy.generator;

/**
 * 嵌套类生成策略枚举
 */
public enum NestedClassStrategy {
    INNER_CLASS,      // 生成内部类（在同一个文件中）
    SEPARATE_CLASS    // 生成独立的类（单独的文件）
}
