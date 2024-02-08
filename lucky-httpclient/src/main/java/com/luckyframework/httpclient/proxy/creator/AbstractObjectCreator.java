package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
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

    /**
     * 单例缓存
     */
    private final Map<String, Object> singletonCache = new ConcurrentHashMap<>(16);

    private final Map<Method, Object> methodObjectCache = new ConcurrentHashMap<>(16);

    private final Map<Class<?>, Object> classObjectCache = new ConcurrentHashMap<>(16);

    private final Map<MethodContext, Object> methodContextObjectCache = new ConcurrentHashMap<>(16);

    @Override
    public Object newObject(Class<?> clazz, String msg, Context context, Scope scope) {
        switch (scope) {
            case SINGLETON: {
                String objectKey = StringUtils.format("[{}]-{}", msg, clazz);
                return singletonCache.computeIfAbsent(objectKey, _k -> createObject(clazz, msg));
            }
            case METHOD: {
                Method objectKey = context.lookupContext(MethodContext.class).getCurrentAnnotatedElement();
                return methodObjectCache.computeIfAbsent(objectKey, _k -> createObject(clazz, msg));
            }
            case CLASS: {
                Class<?> objectKey = context.lookupContext(MethodContext.class).getClassContext().getCurrentAnnotatedElement();
                return classObjectCache.computeIfAbsent(objectKey, _k -> createObject(clazz, msg));
            }
            case METHOD_CONTEXT: {
                MethodContext objectKey = context.lookupContext(MethodContext.class);
                return methodContextObjectCache.computeIfAbsent(objectKey, _k -> createObject(clazz, msg));
            }
            default: {
                return createObject(clazz, msg);
            }
        }
    }

    public void clearCache(Scope scope) {
        switch (scope) {
            case SINGLETON: {
                singletonCache.clear();
            }
            case METHOD: {
                methodObjectCache.clear();
            }
            case CLASS: {
                classObjectCache.clear();
            }
            case METHOD_CONTEXT: {
                methodContextObjectCache.clear();
            }
        }
    }

    public void removeMethodContextElement(MethodContext context) {
        methodContextObjectCache.remove(context);
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
