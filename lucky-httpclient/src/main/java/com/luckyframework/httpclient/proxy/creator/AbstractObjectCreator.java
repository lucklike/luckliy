package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 对象创建对象的基本实现，实现了对象作用域的功能，并且提供了对{@link ObjectFactory}功能的支持
 * <pre>
 * 对象作用域：
 *      1.SINGLETON      全局只会有一个对象
 *      2.PROTOTYPE      每一次创建都会产生一个新对象
 *      3.METHOD         在同一个代理方法内只会产生一个对象
 *      4.CLASS          每一个代理方法内只会产生一个对象
 *      5.METHOD_CONTEXT 每一次HTTP调用只会产生一个对象
 *
 * 对象生成方式：
 *      1.子类的{@link #doCreateObject(Class, String)}方法返回的是{@link ObjectFactory}时则会调用{@link ObjectFactory#createObject(String)}来获取对象
 *      2.子类的{@link #doCreateObject(Class, String)}方法返回的是非{@link ObjectFactory}时，则会直接返回该对象
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/6 23:40
 */
public abstract class AbstractObjectCreator implements ObjectCreator {

    /**
     * 单例缓存
     */
    private final Map<Class<?>, Map<String, Object>> singletonCache = new ConcurrentHashMap<>(16);

    /**
     * 方法级别对象缓存
     */
    private final Map<Method, Map<String, Object>> methodObjectCache = new ConcurrentHashMap<>(16);

    /**
     * 类级别对象缓存
     */
    private final Map<Class<?>, Map<String, Object>> classObjectCache = new ConcurrentHashMap<>(16);

    /**
     * 方法上下文级别对象缓存
     */
    private final Map<MethodContext, Map<String, Object>> methodContextObjectCache = new ConcurrentHashMap<>(16);

    @Override
    public Object newObject(Class<?> clazz, String msg, Context context, Scope scope) {
        switch (scope) {
            case SINGLETON: {
                return cacheComputeIfAbsent(singletonCache, clazz, msg, () -> this.createObject(clazz, msg));
            }
            case METHOD: {
                Method cacheKey = context.lookupContext(MethodContext.class).getCurrentAnnotatedElement();
                return cacheComputeIfAbsent(methodObjectCache, cacheKey, msg, () -> this.createObject(clazz, msg));
            }
            case CLASS: {
                Class<?> cacheKey = context.lookupContext(MethodContext.class).getClassContext().getCurrentAnnotatedElement();
                return cacheComputeIfAbsent(classObjectCache, cacheKey, msg, () -> this.createObject(clazz, msg));
            }
            case METHOD_CONTEXT: {
                MethodContext cacheKey = context.lookupContext(MethodContext.class);
                return cacheComputeIfAbsent(methodContextObjectCache, cacheKey, msg, () -> this.createObject(clazz, msg));
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

    /**
     * 从二级缓存中获取值，没有则放入
     *
     * @param cacheMap       二级缓存
     * @param cacheKey       一级缓存Key
     * @param objectKey      二级缓存Key
     * @param objectSupplier 对象创建方式
     * @param <K>            一级缓存Key的类型
     * @param <_K>           二级缓存Key的类型
     * @param <V>            缓存元素类型
     * @return 缓存值
     */
    private <K, _K, V> V cacheComputeIfAbsent(Map<K, Map<_K, V>> cacheMap, K cacheKey, _K objectKey, Supplier<V> objectSupplier) {
        Map<_K, V> objectMap = cacheMap.computeIfAbsent(cacheKey, _k -> new ConcurrentHashMap<>(4));
        return objectMap.computeIfAbsent(objectKey, _k -> objectSupplier.get());
    }
}
