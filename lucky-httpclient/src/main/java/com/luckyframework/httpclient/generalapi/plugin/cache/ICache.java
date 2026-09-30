package com.luckyframework.httpclient.generalapi.plugin.cache;

import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 缓存插件顶层接口，定义了缓存插件所需的缓存读写功能
 *
 * <p>可以通过{@link CachePluginMeta#cache()}指定缓存实现类，
 * 或者通过{@link CachePluginMeta#cacheGenerate()}指定缓存对象生成器，
 * 也可以直接使用{@link MemoryCache @MemoryCache}注解来使用默认的进程内缓存实现
 *
 * <p>所有方法均会传入当前API方法对应的方法上下文{@link MethodContext}，
 * 实现方可以基于上下文信息来实现更灵活的缓存策略
 *
 * @author fukang
 * @version 3.0.1
 * @date 2025/6/13 17:21
 */
public interface ICache {

    /**
     * 获取缓存数据
     *
     * @param mc  当前API方法对应的方法上下文
     * @param key 缓存key
     * @return 缓存数据，key不存在或者缓存已过期时返回{@code null}
     */
    Object get(MethodContext mc, String key);

    /**
     * 写入缓存，写入的条目永不过期
     *
     * @param mc    当前API方法对应的方法上下文
     * @param key   缓存key
     * @param value 缓存数据
     */
    void put(MethodContext mc, String key, Object value);

    /**
     * 写入缓存，并指定过期时间
     *
     * @param mc      当前API方法对应的方法上下文
     * @param key     缓存key
     * @param value   缓存数据
     * @param expires 过期时间，单位：毫秒，小于0时表示永不过期
     */
    void put(MethodContext mc, String key, Object value, long expires);

    /**
     * 移除缓存
     *
     * @param mc  当前API方法对应的方法上下文
     * @param key 缓存key
     */
    void remove(MethodContext mc, String key);

}
