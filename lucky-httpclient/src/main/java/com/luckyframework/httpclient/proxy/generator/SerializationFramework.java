package com.luckyframework.httpclient.proxy.generator;

/**
 * 序列化框架类型枚举
 * 每个枚举附带一个探针类与依赖坐标：选了某个框架但工程里没引对应依赖时，
 * 生成的代码会直接编译失败，提前探测并给出提示比让用户去猜报错更有用
 */
public enum SerializationFramework {
    // JSON框架
    JACKSON("com.fasterxml.jackson.annotation.JsonProperty", "com.fasterxml.jackson.core:jackson-annotations"),        // Jackson JSON
    GSON("com.google.gson.annotations.SerializedName", "com.google.code.gson:gson"),           // Gson
    FASTJSON("com.alibaba.fastjson.annotation.JSONField", "com.alibaba:fastjson"),       // Fastjson
    FASTJSON2("com.alibaba.fastjson2.annotation.JSONField", "com.alibaba.fastjson2:fastjson2"),      // Fastjson2

    // XML框架
    JACKSON_XML("com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty", "com.fasterxml.jackson.dataformat:jackson-dataformat-xml"),    // Jackson XML
    XSTREAM("com.thoughtworks.xstream.annotations.XStreamAlias", "com.thoughtworks.xstream:xstream"),        // XStream
    JAXB("javax.xml.bind.annotation.XmlElement", "javax.xml.bind:jaxb-api (must be added explicitly on JDK 11+)"),           // JAXB (Java Architecture for XML Binding)

    // 通用
    NONE(null, null);            // 不使用任何序列化框架注解

    /**
     * 用于探测依赖是否存在的注解类全限定名
     */
    private final String probeClassName;

    /**
     * 缺失时提示用户引入的依赖坐标
     */
    private final String dependencyCoordinate;

    SerializationFramework(String probeClassName, String dependencyCoordinate) {
        this.probeClassName = probeClassName;
        this.dependencyCoordinate = dependencyCoordinate;
    }

    public String getProbeClassName() {
        return probeClassName;
    }

    public String getDependencyCoordinate() {
        return dependencyCoordinate;
    }

    /**
     * 判断当前类路径中是否存在该框架的注解类
     *
     * @return true表示依赖已就绪，NONE 恒为true
     */
    public boolean isDependencyPresent() {
        if (probeClassName == null) {
            return true;
        }
        try {
            Class.forName(probeClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
