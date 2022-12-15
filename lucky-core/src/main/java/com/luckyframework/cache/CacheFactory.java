package com.luckyframework.cache;

/**
 * 缓存工厂，负责构建一个缓存对象
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/13 10:35
 */
@FunctionalInterface
public interface CacheFactory {

    Cache<Object, Object> getCache();

}
