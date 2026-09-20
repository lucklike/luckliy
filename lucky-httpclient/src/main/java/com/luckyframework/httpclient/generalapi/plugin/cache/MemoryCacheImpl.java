package com.luckyframework.httpclient.generalapi.plugin.cache;

import com.luckyframework.cache.finder.ExpiringMap;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * {@link ICache}的进程内缓存实现，也是{@link MemoryCache @MemoryCache}注解默认使用的缓存实现
 *
 * <p>底层使用{@link ExpiringMap}存储缓存数据，采用"惰性删除 + 周期清理"的方式回收过期条目：
 * <pre>
 *     惰性删除：读取时若命中的条目已过期，则会立即将其移除并返回{@code null}
 *     周期清理：后台清理线程会周期性地清理已经过期的条目
 * </pre>
 *
 * <p>该实现只提供进程内的缓存能力，无法在分布式环境下共享缓存数据；
 * 缓存对象使用{@link com.luckyframework.httpclient.proxy.creator.Scope#SINGLETON}生成时，
 * 同一个Class只会存在一个实例，此时缓存数据在整个应用内共享
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
public class MemoryCacheImpl implements ICache {

    /**
     * 缓存数据存储器
     */
    private final ExpiringMap<String, Object> cache = new ExpiringMap<>();

    /**
     * 获取缓存数据，未命中或者已过期时返回{@code null}
     *
     * @param mc  当前API方法对应的方法上下文
     * @param key 缓存key
     * @return 缓存数据
     */
    @Override
    public Object get(MethodContext mc, String key) {
        return cache.getNotExpired(key);
    }

    /**
     * 写入缓存，写入的条目永不过期
     *
     * @param mc    当前API方法对应的方法上下文
     * @param key   缓存key
     * @param value 缓存数据
     */
    @Override
    public void put(MethodContext mc, String key, Object value) {
        cache.put(key, value);
    }

    /**
     * 写入缓存，并指定过期时间
     *
     * @param mc      当前API方法对应的方法上下文
     * @param key     缓存key
     * @param value   缓存数据
     * @param expires 过期时间，单位：毫秒，小于0时表示永不过期
     */
    @Override
    public void put(MethodContext mc, String key, Object value, long expires) {
        if (expires < 0) {
            put(mc, key, value);
        } else {
            cache.putFixedTimeRemove(key, value, expires);
        }
    }

    /**
     * 移除缓存
     *
     * @param mc  当前API方法对应的方法上下文
     * @param key 缓存key
     */
    @Override
    public void remove(MethodContext mc, String key) {
        cache.remove(key);
    }
}
