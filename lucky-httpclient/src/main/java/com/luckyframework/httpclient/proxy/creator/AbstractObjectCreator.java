package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.common.StringUtils;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对象创建对象的基本实现，该类基于缓存实现了单例、原型对象的创建方式
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/6 23:40
 */
public abstract class AbstractObjectCreator implements ObjectCreator {

    private final Map<String, Object> cache = new ConcurrentHashMap<>(32);

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
        Object createObject = doCreateObject(clazz, msg);
        if (createObject instanceof ObjectFactory) {
            return ((ObjectFactory) createObject).createObject(msg);
        }
        return createObject;
    }

    protected abstract Object doCreateObject(Class<?> clazz, String msg);
}
