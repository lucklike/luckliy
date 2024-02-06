package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.cache.Cache;
import com.luckyframework.cache.impl.ConcurrentMapCache;
import com.luckyframework.common.StringUtils;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.util.Assert;

/**
 * 默认的对象创建器实现
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/6 23:40
 */
public abstract class AbstractObjectCreator implements ObjectCreator {

    private final Cache<String, Object> cache = new ConcurrentMapCache<>(32);

    @Override
    public Object newObject(Class<?> clazz, String msg, Scope scope) {
        if (scope == Scope.SINGLETON) {
            String objectKey = StringUtils.format("[{}]-{}", msg, clazz);
            return cache.computeIfAbsent(objectKey, _k -> createObject(clazz, msg));
        }
        return createObject(clazz, msg);
    }


    protected Object createObject(Class<?> clazz, String msg) {
        Assert.notNull(clazz, "Failed to create the object with a null class.");
        if (ObjectFactory.class.isAssignableFrom(clazz)) {
            ObjectFactory factory = (ObjectFactory) ClassUtils.newObject(clazz);
            return factory.createObject(msg);
        }
        return doCreateObject(clazz, msg);
    }

    protected abstract Object doCreateObject(Class<?> clazz, String msg);
}
