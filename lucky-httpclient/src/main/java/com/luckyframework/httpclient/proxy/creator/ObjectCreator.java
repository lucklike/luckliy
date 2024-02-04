package com.luckyframework.httpclient.proxy.creator;

/**
 * 对象创建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/31 02:34
 */
@FunctionalInterface
public interface ObjectCreator {

    /**
     * 用于创建一个对象实例
     *
     * @param aClass        实例Class
     * @param createMessage 对象的创建信息
     * @return 对象实例
     */
    <T> T newObject(Class<T> aClass, String createMessage);
}
