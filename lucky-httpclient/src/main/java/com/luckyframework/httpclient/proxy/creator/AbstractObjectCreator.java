package com.luckyframework.httpclient.proxy.creator;

import com.luckyframework.httpclient.proxy.context.Context;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 对象创建对象的基本实现，实现了对象作用域的功能
 * <pre>
 * 对象作用域：
 *      1.SINGLETON      全局只会有一个对象
 *      2.PROTOTYPE      每一次创建都会产生一个新对象
 *      3.METHOD         在同一个代理方法内只会产生一个对象
 *      4.CLASS          每一个代理方法内只会产生一个对象
 *      5.METHOD_CONTEXT 每一次HTTP调用只会产生一个对象
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

    @SuppressWarnings("unchecked")
    @Override
    public <T> T newObject(Class<T> clazz, String msg, Context context, Scope scope, Consumer<T> consumer) {
        switch (scope) {
            case SINGLETON: {
                return singletonCache.computeIfAbsent(clazz, msg, () -> this.createObject(clazz, msg), consumer);
            }
            case METHOD: {
                Method key = context.lookupContext(MethodContext.class).getCurrentAnnotatedElement();
                return (T) computeIfAbsent(methodObjectCache, key, clazz, msg, () -> this.createObject(clazz, msg), consumer);
            }
            case CLASS: {
                Class<?> key = context.lookupContext(MethodContext.class).getClassContext().getCurrentAnnotatedElement();
                return (T)computeIfAbsent(classObjectCache, key, clazz, msg, () -> this.createObject(clazz, msg), consumer);
            }
            case METHOD_CONTEXT: {
                MethodContext key = context.lookupContext(MethodContext.class);
                return (T)computeIfAbsent(methodContextObjectCache, key, clazz, msg, () -> this.createObject(clazz, msg), consumer);
            }
            default: {
                T t = createObject(clazz, msg);
                consumer.accept(t);
                return t;
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

    protected <T> T createObject(Class<T> clazz, String msg) {
        Assert.notNull(clazz, "Failed to create the object with a null class.");
        return doCreateObject(clazz, msg);
    }

    private <K, T> Object computeIfAbsent(Map<K, ClassInstanceCache> cache, K key, Class<T> cacheKey, String msgKey, Supplier<T> objectSupplier, Consumer<T> consumer) {
        ClassInstanceCache instanceCache = cache.computeIfAbsent(key, _k -> new ClassInstanceCache());
        return instanceCache.computeIfAbsent(cacheKey, msgKey, objectSupplier, consumer);
    }

    protected abstract <T> T doCreateObject(Class<T> clazz, String msg);


    /**
     * 类实例缓存对象
     */
    @SuppressWarnings("unchecked")
    static class ClassInstanceCache {
        private final Map<Class<?>, Map<String, Object>> cacheMap = new ConcurrentHashMap<>(8);

        public <T> T computeIfAbsent(Class<T> cacheKey, String msgKey, Supplier<T> objectSupplier, Consumer<T> consumer) {
            Map<String, Object> objectMap = cacheMap.computeIfAbsent(cacheKey, _k -> new ConcurrentHashMap<>(4));
            return (T) objectMap.computeIfAbsent(msgKey, _k -> {
                T t = objectSupplier.get();
                consumer.accept(t);
                return t;
            });
        }

        public void clear() {
            cacheMap.clear();
        }
    }
}
