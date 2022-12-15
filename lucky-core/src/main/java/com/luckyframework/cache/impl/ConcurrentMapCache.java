package com.luckyframework.cache.impl;

import com.luckyframework.cache.Cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 20:20
 */
public class ConcurrentMapCache<K, V> implements Cache<K, V> {

    private final ConcurrentHashMap<K, V> concurrentHashMap;

    public ConcurrentMapCache(int initialCapacity){
        concurrentHashMap = new ConcurrentHashMap<>(initialCapacity);
    }

    public ConcurrentMapCache(){
        concurrentHashMap = new ConcurrentHashMap<>();
    }


    @Override
    public V get(K k) {
        return concurrentHashMap.get(k);
    }

    @Override
    public V put(K k, V v) {
        return concurrentHashMap.put(k, v);
    }

    @Override
    public boolean containsKey(K k) {
        return concurrentHashMap.containsKey(k);
    }

    @Override
    public int size() {
        return concurrentHashMap.size();
    }

    @Override
    public V remove(K k) {
        return concurrentHashMap.remove(k);
    }

    @Override
    public void clear() {
        concurrentHashMap.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof ConcurrentMapCache){
            return super.equals(obj);
        }
      return false;
    }
}
