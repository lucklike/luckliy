package com.luckyframework.httpclient.core.util;

/**
 * 属性过滤器
 */
@FunctionalInterface
public interface PropertyFilter {

    /**
     * 是否需要转换
     *
     * @param sourceProperty 原属性信息
     * @param targetProperty 目标属性信息
     * @return 是否需要转换
     */
    boolean needConvert(PropertyInfo sourceProperty, PropertyInfo targetProperty);
}
