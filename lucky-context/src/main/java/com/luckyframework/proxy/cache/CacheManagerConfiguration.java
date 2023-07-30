package com.luckyframework.proxy.cache;

import com.luckyframework.annotations.Bean;
import com.luckyframework.annotations.Configuration;
import com.luckyframework.bean.aware.ApplicationContextAware;
import com.luckyframework.cache.CacheFactory;
import com.luckyframework.cache.CacheManager;
import com.luckyframework.cache.impl.ConcurrentMapCache;
import com.luckyframework.cache.impl.FIFOCache;
import com.luckyframework.cache.impl.LFUCache;
import com.luckyframework.cache.impl.LRUCache;
import com.luckyframework.cache.impl.ThreadLocalCacheWrapper;
import com.luckyframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存功能相关的配置类
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 20:27
 */
@Configuration(proxyBeanMethods = false)
public class CacheManagerConfiguration implements ApplicationContextAware {

    public static final String DEFAULT_CACHE_MANGER = "luckyDefaultCacheManger";

    public static final String LRU = "luckyLRUCache";
    public static final String FIFO = "luckyFIFOCache";
    public static final String LFU = "luckyLFUCache";
    public static final String CONCURRENT = "luckyConcurrentCache";
    public static final String TL_LRU = "luckyThreadLocalLRUCache";
    public static final String TL_LFU = "luckyThreadLocalLFUCache";
    public static final String TL_FIFO = "luckyThreadLocalFIFOCache";
    public static final String TL_CONCURRENT = "luckyThreadLocalConcurrentCache";

    private static final Integer DEFAULT_SIZE = 225;

    private final Map<String, CacheFactory> usingCacheFactoryMap = new HashMap<>();

    @Bean(DEFAULT_CACHE_MANGER)
    public CacheManager defaultManager(){

        // 注册内置的缓存工厂
        CacheManager cacheManager = new CacheManager(CONCURRENT, () -> new ConcurrentMapCache<>(DEFAULT_SIZE));
        cacheManager.registerCacheFactory(LRU, () -> new LRUCache<>(DEFAULT_SIZE));
        cacheManager.registerCacheFactory(LFU, () -> new LFUCache<>(DEFAULT_SIZE));
        cacheManager.registerCacheFactory(FIFO, () -> new FIFOCache<>(DEFAULT_SIZE));
        cacheManager.registerCacheFactory(TL_LRU, () -> ThreadLocalCacheWrapper.createLRUWrapper(DEFAULT_SIZE));
        cacheManager.registerCacheFactory(TL_LFU, () -> ThreadLocalCacheWrapper.createLFUWrapper(DEFAULT_SIZE));
        cacheManager.registerCacheFactory(TL_FIFO, () -> ThreadLocalCacheWrapper.createFIFOWrapper(DEFAULT_SIZE));
        cacheManager.registerCacheFactory(TL_CONCURRENT, () -> ThreadLocalCacheWrapper.createCMAPWrapper(DEFAULT_SIZE));

        // 注册用户声明的缓存工厂
        for (Map.Entry<String, CacheFactory> entry : usingCacheFactoryMap.entrySet()) {
            cacheManager.registerCacheFactory(entry.getKey(), entry.getValue());
        }

        return cacheManager;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        String[] cacheFactoryBeanNames = applicationContext.getBeanNamesForType(CacheFactory.class);
        for (String cacheFactoryBeanName : cacheFactoryBeanNames) {
            usingCacheFactoryMap.put(cacheFactoryBeanName, applicationContext.getBean(cacheFactoryBeanName, CacheFactory.class));
        }
    }
}
