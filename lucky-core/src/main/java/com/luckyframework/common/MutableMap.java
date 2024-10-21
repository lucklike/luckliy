package com.luckyframework.common;

import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 由多个Map集合构成的Map
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/10/19 14:57
 */
public class MutableMap<K, V> implements Map<K, V> {

    private final List<Map<K, V>> mutableMapList = new ArrayList<>();

    public MutableMap(boolean isInit) {
        if (isInit) {
            mutableMapList.add(new HashMap<>());
        }
    }

    public MutableMap() {
        this(true);
    }

    public MutableMap(@NonNull Map<K, V> map) {
        this.mutableMapList.add(map);
    }

    public MutableMap(List<Map<K, V>> mutableMapList) {
        this.mutableMapList.addAll(mutableMapList);
    }

    public void addFirst(@NonNull Map<K, V> map) {
        if (ContainerUtils.isNotEmptyMap(map)) {
            mutableMapList.add(0, map);
        }
    }

    public void addLast(@NonNull Map<K, V> map) {
        if (ContainerUtils.isNotEmptyMap(map)) {
            mutableMapList.add(map);
        }
    }

    //------------------------------------------------------------------------------------
    //                              Map Methods
    //------------------------------------------------------------------------------------

    @Override
    public int size() {
        return keySet().size();
    }

    @Override
    public boolean isEmpty() {
        for (Map<K, V> map : mutableMapList) {
            if (!map.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        for (Map<K, V> map : mutableMapList) {
            if (map.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        for (Map<K, V> map : mutableMapList) {
            if (map.containsValue(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        for (Map<K, V> map : mutableMapList) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        check();
        return mutableMapList.get(0).put(key, value);
    }

    @Override
    public V remove(Object key) {
        V v = null;
        for (Map<K, V> map : mutableMapList) {
            if (v == null) {
                v = map.remove(key);
            } else {
                map.remove(key);
            }
        }
        return v;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        check();
        mutableMapList.get(0).putAll(m);
    }

    @Override
    public void clear() {
        mutableMapList.clear();
    }

    @NonNull
    @Override
    public Set<K> keySet() {
        Set<K> keySet = new HashSet<>();
        for (Map<K, V> map : mutableMapList) {
            keySet.addAll(map.keySet());
        }
        return keySet;
    }

    @NonNull
    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (K k : keySet()) {
            values.add(get(k));
        }
        return values;
    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> entrySet = new HashSet<>();
        Set<K> keySet = new HashSet<>();

        for (Map<K, V> map : mutableMapList) {
            for (Entry<K, V> kvEntry : map.entrySet()) {
                if (!keySet.contains(kvEntry.getKey())) {
                    keySet.add(kvEntry.getKey());
                    entrySet.add(kvEntry);
                }
            }
        }
        return entrySet;
    }

    private void check() {
        if (mutableMapList.isEmpty()) {
            throw new IllegalStateException("MutableMapList is empty");
        }
    }

}
