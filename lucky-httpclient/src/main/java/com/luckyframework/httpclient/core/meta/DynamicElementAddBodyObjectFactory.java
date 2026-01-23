package com.luckyframework.httpclient.core.meta;

/**
 * 支持动态的添加元素的工厂
 */
public interface DynamicElementAddBodyObjectFactory extends BodyObjectFactory {

    /**
     * 添加单个元素
     *
     * @param elementName  元素的名称
     * @param elementValue 元素值
     */
    void addElement(String elementName, Object elementValue);
}
