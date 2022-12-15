package com.luckyframework.cache.impl;

import com.luckyframework.cache.Cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于{@link LinkedHashMap}实现的FIFO缓存，先进先出的数据缓存器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/10 16:44
 */
public class FIFOCache<K, V> extends LinkedHashMap<K,V> implements Cache<K,V> {


    private final int MAX_CACHE_SIZE;

    /**
     * 构造器
     * @param cacheSize 缓存大小 默认的负债因子为(0.75f)
     */
    public FIFOCache(int cacheSize){
        super((int)Math.ceil(cacheSize / 0.75f) + 1, 0.75f, false);
        MAX_CACHE_SIZE = cacheSize;
    }

    /**
     * 构造器
     * @param cacheSize 缓存大小
     * @param loadFactor 负债因子
     */
    public FIFOCache(int cacheSize, float loadFactor){
        super((int)Math.ceil(cacheSize / loadFactor) + 1, loadFactor, false);
        MAX_CACHE_SIZE = cacheSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > MAX_CACHE_SIZE;
    }

    @Override
    public synchronized V get(Object key) {
        return super.get(key);
    }

    @Override
    public synchronized V put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public synchronized void putAll(Map<? extends K, ? extends V> m) {
        super.putAll(m);
    }

    @Override
    public synchronized V remove(Object key) {
        return super.remove(key);
    }

    @Override
    public synchronized int size() {
        return super.size();
    }

    @Override
    public synchronized boolean equals(Object o) {
        if(o instanceof FIFOCache){
            return super.equals(o);
        }
        return false;
    }
}
