package com.luckyframework.httpclient.proxy.generator;

/**
 * 序列化框架类型枚举
 */
public enum SerializationFramework {
    // JSON框架
    JACKSON,        // Jackson JSON
    GSON,           // Gson
    FASTJSON,       // Fastjson
    FASTJSON2,      // Fastjson2

    // XML框架
    JACKSON_XML,    // Jackson XML
    XSTREAM,        // XStream
    JAXB,           // JAXB (Java Architecture for XML Binding)

    // 通用
    NONE            // 不使用任何序列化框架注解
}
