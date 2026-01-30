package com.luckyframework.httpclient.proxy.context;

import java.lang.reflect.Type;

/**
 * 转换元数据
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/1/31 02:56
 */
public class ConvertMetaData {

    /**
     * 默认的转换元数据
     */
    public static final ConvertMetaData DEFAULT = of(Object.class, "");


    /**
     * 转化元类型
     */
    private Type metaType;

    /**
     * 内容类型
     */
    private String contentType;

    public static ConvertMetaData of(Type metaType, String contentType) {
        ConvertMetaData metaData = new ConvertMetaData();
        metaData.setContentType(contentType);
        metaData.setMetaType(metaType);
        return metaData;
    }

    /**
     * 设置转化元类型
     *
     * @param metaType 转化元类型
     */
    public void setMetaType(Type metaType) {
        this.metaType = metaType;
    }

    /**
     * 设置内容类型
     *
     * @param contentType 内容类型
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 获取转化元类型
     *
     * @return 转化元类型
     */
    public Type getMetaType() {
        return metaType;
    }

    /**
     * 获取内容类型
     *
     * @return 内容类型
     */
    public String getContentType() {
        return contentType;
    }
}
