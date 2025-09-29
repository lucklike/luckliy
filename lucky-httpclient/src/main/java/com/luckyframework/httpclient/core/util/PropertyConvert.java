package com.luckyframework.httpclient.core.util;

/**
 * 属性转换回调
 */
public interface PropertyConvert {

    /**
     * 属性转换逻辑
     *
     * @param sourceProperty 原属性信息
     * @param targetProperty 目标属性信息
     */
    void convert(PropertyInfo sourceProperty, PropertyInfo targetProperty);
}
