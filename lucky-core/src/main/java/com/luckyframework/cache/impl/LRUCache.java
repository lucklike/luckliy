package com.luckyframework.cache.impl;

import com.luckyframework.cache.Cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于{@link LinkedHashMap}实现的LRU缓存 最近最少使用缓存算法
 * @param <K>
 * @param <V>
 */
public class LRUCache<K,V> extends LinkedHashMap<K,V> implements Cache<K,V> {

    private final int MAX_CACHE_SIZE;

    /**
     * 构造器
     * @param cacheSize 缓存大小 默认的负债因子为(0.75f)
     */
    public LRUCache(int cacheSize){
        super((int)Math.ceil(cacheSize / 0.75f) + 1, 0.75f, true);
        MAX_CACHE_SIZE = cacheSize;
    }

    /**
     * 构造器
     * @param cacheSize 缓存大小
     * @param loadFactor 负债因子
     */
    public LRUCache(int cacheSize, float loadFactor){
        super((int)Math.ceil(cacheSize / loadFactor) + 1, loadFactor, true);
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
    public String toString() {
        return "LRUCache" + super.toString();
    }

    @Override
    public synchronized boolean equals(Object o) {
        if(o instanceof LRUCache){
            return super.equals(o);
        }
        return false;
    }
}
