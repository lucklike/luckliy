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
    private final ClassInstanceCache singletonCache = new ClassInstanceCache();

    /**
     * 方法级别对象缓存
     */
    private final Map<Method, ClassInstanceCache> methodObjectCache = new ConcurrentHashMap<>(16);

    /**
     * 类级别对象缓存
     */
    private final Map<Class<?>, ClassInstanceCache> classObjectCache = new ConcurrentHashMap<>(16);

    /**
     * 方法上下文级别对象缓存
     */
    private final Map<MethodContext, ClassInstanceCache> methodContextObjectCache = new ConcurrentHashMap<>(16);

    @Override
    public Object newObject(Class<?> clazz, String msg, Context context, Scope scope) {
        switch (scope) {
            case SINGLETON: {
                return singletonCache.computeIfAbsent(clazz, msg, () -> this.createObject(clazz, msg));
            }
            case METHOD: {
                Method key = context.lookupContext(MethodContext.class).getCurrentAnnotatedElement();
                return computeIfAbsent(methodObjectCache, key, clazz, msg, () -> this.createObject(clazz, msg));
            }
            case CLASS: {
                Class<?> key = context.lookupContext(MethodContext.class).getClassContext().getCurrentAnnotatedElement();
                return computeIfAbsent(classObjectCache, key, clazz, msg, () -> this.createObject(clazz, msg));
            }
            case METHOD_CONTEXT: {
                MethodContext key = context.lookupContext(MethodContext.class);
                return computeIfAbsent(methodContextObjectCache, key, clazz, msg, () -> this.createObject(clazz, msg));
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

    private <K> Object computeIfAbsent(Map<K, ClassInstanceCache> cache, K key, Class<?> cacheKey, String msgKey, Supplier<Object> objectSupplier) {
        ClassInstanceCache instanceCache = cache.computeIfAbsent(key, _k -> new ClassInstanceCache());
        return instanceCache.computeIfAbsent(cacheKey, msgKey, objectSupplier);
    }

    protected abstract Object doCreateObject(Class<?> clazz, String msg);


    static class ClassInstanceCache {
        private final Map<Class<?>, Map<String, Object>> cacheMap = new ConcurrentHashMap<>(8);

        public Object computeIfAbsent(Class<?> cacheKey, String msgKey, Supplier<Object> objectSupplier) {
            Map<String, Object> objectMap = cacheMap.computeIfAbsent(cacheKey, _k -> new ConcurrentHashMap<>(4));
            return objectMap.computeIfAbsent(msgKey, _k -> objectSupplier.get());
        }

        public void clear() {
            cacheMap.clear();
        }
    }
}
