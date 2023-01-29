package com.luckyframework.cache.finder;

/**
 * 结果查找器
 * @author fukang
 * @version 1.0.0
 * @date 2023/1/12 14:19
 */
@FunctionalInterface
public interface ResultQuery<K, V> {

    V find(K key);

}
