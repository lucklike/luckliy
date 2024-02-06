package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;

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
     * @param clazz 实例Class
     * @param msg    对象的创建信息
     * @param scope  对象的作用域
     * @return 对象实例
     */
    Object newObject(Class<?> clazz, String msg, Scope scope);

    default Object newObject(ObjectGenerate generate) {
        return newObject(generate.clazz(), generate.msg(), generate.scope());
    }
}
