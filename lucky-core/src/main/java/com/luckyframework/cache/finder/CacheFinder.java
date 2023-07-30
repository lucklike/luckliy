package com.luckyframework.cache.finder;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存查找器，该查找器会维护一个过期时间，当缓存的K值在过期时间内会走缓存
 * 否则会执行{@link ResultQuery#find(Object)}方法进行查找
 * @author fukang
 * @version 1.0.0
 * @date 2023/1/12 14:17
 */
public class CacheFinder<K, V> {

    /** 缓存Map*/
    private final Map<K, CacheEntry> cacheMap = new HashMap<>();

    /** 缓存过期时间*/
    private Long cacheMillis;

    public CacheFinder(Long cacheMillis) {
        this.cacheMillis = cacheMillis;
    }

    public CacheFinder() {
        this(-1L);
    }

    public Long getCacheMillis() {
        return cacheMillis;
    }

    public void setCacheMillis(Long cacheMillis) {
        this.cacheMillis = cacheMillis;
    }

    @Nullable
    public V get(@NonNull K key, @NonNull ResultQuery<K, V> query){

        Assert.notNull(key, "key is null");
        Assert.notNull(query, "ResultQuery is null");

        synchronized (this.cacheMap){

            if(getCacheMillis() <= 0){
                return query.find(key);
            }

            CacheEntry cacheEntry = cacheMap.get(key);

            // 当前时间的毫秒数
            long currentTimeMillis = System.currentTimeMillis();

            // 没有命中缓存
            if(cacheEntry == null){
                V result = query.find(key);
                cacheMap.put(key, new CacheEntry(currentTimeMillis + currentTimeMillis, result));
                return result;
            }

            /* 命中缓存的情况 */

            Long expirationTime = cacheEntry.getExpirationTime();

            // 缓存未过期
            if(expirationTime >= currentTimeMillis){
                return cacheEntry.getData();
            }

            // 缓存已经过期，需要重新执行查询操作
            V result = query.find(key);
            cacheMap.put(key, new CacheEntry(currentTimeMillis + currentTimeMillis, result));
            return result;
        }
    }

    public void clear(){
        this.cacheMap.clear();
    }

    public V remove(@NonNull K key){
        CacheEntry removeEntry = cacheMap.remove(key);
        return removeEntry == null ? null : removeEntry.getData();
    }

    /**
     * 缓存元素，存储缓存结果和结果的过期时间
     */
    class CacheEntry{

        private Long expirationTime;
        private V data;

        public CacheEntry(Long expirationTime, V data) {
            this.expirationTime = expirationTime;
            this.data = data;
        }

        public CacheEntry(V data) {
            this(System.currentTimeMillis() + cacheMillis, data);
        }

        public Long getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(Long expirationTime) {
            this.expirationTime = expirationTime;
        }

        public V getData() {
            return data;
        }

        public void setData(V data) {
            this.data = data;
        }
    }

}
