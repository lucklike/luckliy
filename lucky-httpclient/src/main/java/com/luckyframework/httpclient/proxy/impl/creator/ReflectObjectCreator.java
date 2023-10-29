package com.luckyframework.httpclient.proxy.impl.creator;

import com.luckyframework.httpclient.proxy.ObjectCreator;
import com.luckyframework.reflect.ClassUtils;

/**
 * 基于反射机制的对象创建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/31 02:38
 */
public class ReflectObjectCreator implements ObjectCreator {

    @Override
    public <T> T newObject(Class<T> aClass, String createMessage) {
        return ClassUtils.newObject(aClass);
    }
}
