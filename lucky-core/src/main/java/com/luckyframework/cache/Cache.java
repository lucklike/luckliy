package com.luckyframework.cache;

import java.util.function.Supplier;

/**
 * 缓存的顶级接口
 * @author fk7075
 * @version 1.0.0
 * @date 2021/8/27 上午10:58
 */
public interface Cache<K, V> {

    V get(K k);

    V put(K k, V v);

    boolean containsKey(K k);

    int size();

    V remove(K k);

    void clear();

    default V putIfAbsent(K k, V v){
        V v1 = get(k);
        if(v1 == null){
            v1 = put(k, v);
        }
        return v1;
    }

    default void put(K k, V v, boolean condition){
        if(condition){
            put(k, v);
        }
    }

    default void put(K k, Supplier<V> supplier, boolean condition){
        if(condition){
            put(k, supplier.get());
        }
    }

    default void remove(K k, boolean condition){
        if(condition){
            remove(k);
        }
    }
}
