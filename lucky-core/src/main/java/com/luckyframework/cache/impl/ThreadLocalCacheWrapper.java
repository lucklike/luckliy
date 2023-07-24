package com.luckyframework.cache.impl;

import com.luckyframework.cache.Cache;
import org.springframework.util.Assert;

/**
 * 基于ThreadLocal的缓存包装器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/4/1 18:42
 */
public class ThreadLocalCacheWrapper<K, V> implements Cache<K, V> {

    private final ThreadLocal<Cache<K, V>> cacheWrapper;

    public ThreadLocalCacheWrapper(Cache<K, V> cache) {
        Assert.notNull(cache, "cache is null");
        cacheWrapper = new ThreadLocal<>();
        cacheWrapper.set(cache);
    }

    public static <K, V> ThreadLocalCacheWrapper<K, V> createLRUWrapper(int cacheSize){
        return new ThreadLocalCacheWrapper<>(new LRUCache<>(cacheSize));
    }

    public static <K, V> ThreadLocalCacheWrapper<K, V> createLFUWrapper(int cacheSize){
        return new ThreadLocalCacheWrapper<>(new LFUCache<>(cacheSize));
    }

    public static <K, V> ThreadLocalCacheWrapper<K, V> createFIFOWrapper(int cacheSize){
        return new ThreadLocalCacheWrapper<>(new FIFOCache<>(cacheSize));
    }

    public static <K, V> ThreadLocalCacheWrapper<K, V> createCMAPWrapper(int cacheSize){
        return new ThreadLocalCacheWrapper<>(new ConcurrentMapCache<>(cacheSize));
    }


    @Override
    public V get(K k) {
        return getCache().get(k);
    }

    @Override
    public V put(K k, V v) {
        return getCache().put(k, v);
    }

    @Override
    public boolean containsKey(K k) {
        return getCache().containsKey(k);
    }

    @Override
    public int size() {
        return getCache().size();
    }

    @Override
    public V remove(K k) {
        return getCache().remove(k);
    }

    @Override
    public void clear() {
        getCache().clear();
        this.cacheWrapper.remove();
    }

    private Cache<K, V> getCache(){
        return cacheWrapper.get();
    }
}
