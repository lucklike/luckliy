package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.reflect.ClassUtils;

/**
 * 基于反射机制的对象创建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/31 02:38
 */
public class ReflectObjectCreator extends AbstractObjectCreator {

    @Override
    protected <T> T doCreateObject(Class<T> clazz, String msg) {
        return ClassUtils.newObject(clazz);
    }
}
