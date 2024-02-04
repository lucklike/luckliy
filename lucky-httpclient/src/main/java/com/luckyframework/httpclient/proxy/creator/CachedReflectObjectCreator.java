package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.reflect.ClassUtils;

/**
 * 带缓存的基于反射机制的对象创建器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/31 02:38
 */
public class CachedReflectObjectCreator extends AbstractCachedObjectCreator {

    @Override
    protected <T> T createObject(Class<T> aClass, String createMessage) {
        return ClassUtils.newObject(aClass);
    }
}
