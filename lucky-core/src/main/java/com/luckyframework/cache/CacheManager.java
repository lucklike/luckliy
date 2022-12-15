package com.luckyframework.cache;

import com.luckyframework.cache.impl.LRUCache;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存管理器
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/12 16:23
 */
public class CacheManager {

    /** 缓存工厂集合*/
    private final Map<String, CacheFactory> cacheFactoryMap = new HashMap<>(64);
    /** 缓存空间集合*/
    private final Map<String, Cache<Object, Object>> cacheSpace = new ConcurrentHashMap<>(225);

    /** 默认的缓存空间*/
    private final String defaultCacheSpace;
    /** 默认的缓存工厂*/
    private final String defaultCacheFactory;

    public CacheManager(@NonNull String defCacheFactoryName, @NonNull CacheFactory defCacheFactory){
        this.defaultCacheFactory = defCacheFactoryName;
        this.defaultCacheSpace = "DEF-CACHE-SPACE:" + defCacheFactoryName.toUpperCase();
        registerCacheFactory(defCacheFactoryName, defCacheFactory);
    }

    public String getDefaultCacheSpace() {
        return defaultCacheSpace;
    }

    public String getDefaultCacheFactory() {
        return defaultCacheFactory;
    }

    //------------------------------------------------------------------
    //                       Cache Factory Methods
    //------------------------------------------------------------------

    /**
     * 注册一个缓存工厂，如果缓存工厂名称已经存在则会抛出{@link IllegalArgumentException}异常
     * @param factoryName   缓存工厂名称
     * @param cacheFactory  缓存工厂
     */
    public void registerCacheFactory(String factoryName, CacheFactory cacheFactory){
        if(hasCacheFactory(factoryName)){
            throw new IllegalArgumentException("Failed to register cache factory, cache factory with name '"+factoryName+"' already exists!");
        }
        cacheFactoryMap.put(factoryName, cacheFactory);
    }

    /**
     * 是否存在该名称的缓存工厂
     * @param factoryName 缓存工厂名称
     * @return 该名称是否已经被注册了
     */
    public boolean hasCacheFactory(String factoryName){
        return cacheFactoryMap.containsKey(factoryName);
    }

    @Nullable
    public CacheFactory getCacheFactory(String factoryName){
        return cacheFactoryMap.get(factoryName);
    }

    public CacheFactory removeCacheFactory(String factoryName){
        if(defaultCacheFactory.equals(factoryName)){
            throw new IllegalArgumentException("The default cache factory cannot be deleted.");
        }
        return cacheFactoryMap.remove(factoryName);
    }

    public CacheFactory updateCacheFactory(String factoryName, CacheFactory cacheFactory){
        if(hasCacheFactory(factoryName)){
            return cacheFactoryMap.put(factoryName, cacheFactory);
        }
        return null;
    }

    public Set<String> getCacheFactoryNames(){
        return cacheFactoryMap.keySet();
    }


    //------------------------------------------------------------------
    //                       Cache Methods
    //------------------------------------------------------------------

    public boolean hasCached(String cacheName){
        return cacheSpace.containsKey(cacheName);
    }

    public Cache<Object, Object> ifNotExistsCreated(String cacheName, String factoryName){
        Cache<Object, Object> cache = cacheSpace.get(cacheName);
        if(cache == null){
            CacheFactory cacheFactory = getCacheFactory(factoryName);
            if(cacheFactory == null){
                throw new IllegalArgumentException("Failed to create cache object: No cache factory with name '"+factoryName+"' exists!");
            }
            cache = cacheFactory.getCache();
            cacheSpace.put(cacheName, cache);
        }
        return cache;
    }

    public Cache<Object, Object> getDefaultCache(){
        return ifNotExistsCreated(defaultCacheSpace, defaultCacheFactory);
    }

    public void clear(String cacheName){
        Cache<Object, Object> cache = cacheSpace.get(cacheName);
        if(cache != null){
            cache.clear();
        }
    }

    public void clearDefault(){
        clear(defaultCacheSpace);
    }

    public void drop(String cacheName){
        cacheSpace.remove(cacheName);
    }

    public Object remove(String cacheName, Object key){
        Cache<Object, Object> cache = cacheSpace.get(cacheName);
        if(cache != null){
            return cache.remove(key);
        }
        return null;
    }

    public Object remove(Object key){
        return remove(defaultCacheSpace, key);
    }

    public Object put(String cacheName, String cacheFactoryName, Object key, Object value){
        Cache<Object, Object> cache = ifNotExistsCreated(cacheName, cacheFactoryName);
        return cache.put(key, value);
    }

    public Object putDefaultSpace( Object key, Object value){
        return put(defaultCacheSpace, defaultCacheFactory, key, value);
    }

    public Object putIfAbsent(String cacheName, String cacheFactoryName, Object key, Object value){
        Cache<Object, Object> cache = ifNotExistsCreated(cacheName, cacheFactoryName);
        return cache.putIfAbsent(key, value);
    }

    public Object putDefaultSpaceIfAbsent(Object key, Object value){
        return putIfAbsent(defaultCacheSpace, defaultCacheFactory, key, value);
    }

    public Object get(String key){
        return get(defaultCacheSpace, key);
    }

    public Object get(String cacheName, String key){
        Cache<Object, Object> cache = cacheSpace.get(cacheName);
        if(cache != null){
            return cache.get(key);
        }
        return null;
    }

    public static void main(String[] args) {
        CacheManager manager = new CacheManager("default", () -> new LRUCache<>(50));
        manager.putDefaultSpace("one", "ONE");
        manager.putDefaultSpace("two", "TWO");

        System.out.println(manager.get("one"));
    }

}
