package com.luckyframework.common;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 对Key大小写不明感的Map
 *
 * @param <V>
 */
public class KeyCaseSensitivityMap<V> implements Map<String, V> {

    private final Map<String, V> upperCaseKeyMap = new LinkedHashMap<>();

    public KeyCaseSensitivityMap(Map<String, V> map) {
        map.forEach((k, v) -> upperCaseKeyMap.put(String.valueOf(k).toUpperCase(), v));
    }


    @Override
    public int size() {
        return upperCaseKeyMap.size();
    }

    @Override
    public boolean isEmpty() {
        return upperCaseKeyMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return upperCaseKeyMap.containsKey(key.toString().toUpperCase());
    }

    @Override
    public boolean containsValue(Object value) {
        return upperCaseKeyMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return upperCaseKeyMap.get(key.toString().toUpperCase());
    }

    @Override
    public V put(String key, V value) {
        return upperCaseKeyMap.put(key.toUpperCase(), value);
    }

    @Override
    public V remove(Object key) {
        return upperCaseKeyMap.remove(key.toString().toUpperCase());
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {
        m.forEach((k, v) -> upperCaseKeyMap.put(k.toUpperCase(), v));
    }

    @Override
    public void clear() {
        upperCaseKeyMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return upperCaseKeyMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return upperCaseKeyMap.values();
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        return upperCaseKeyMap.entrySet();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return upperCaseKeyMap.getOrDefault(key.toString().toUpperCase(), defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super V> action) {
        upperCaseKeyMap.forEach((t, u) -> action.accept(t.toUpperCase(), u));
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super V, ? extends V> function) {
        upperCaseKeyMap.replaceAll((t, u) -> function.apply(t.toUpperCase(), u));
    }

    @Override
    public V putIfAbsent(String key, V value) {
        return upperCaseKeyMap.putIfAbsent(key.toUpperCase(), value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return upperCaseKeyMap.remove(key.toString().toUpperCase(), value);
    }

    @Override
    public boolean replace(String key, V oldValue, V newValue) {
        return upperCaseKeyMap.replace(key.toUpperCase(), oldValue, newValue);
    }

    @Override
    public V replace(String key, V value) {
        return upperCaseKeyMap.replace(key.toUpperCase(), value);
    }

    @Override
    public V computeIfAbsent(String key, Function<? super String, ? extends V> mappingFunction) {
        return upperCaseKeyMap.computeIfAbsent(key.toUpperCase(), mappingFunction);
    }

    @Override
    public V computeIfPresent(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return upperCaseKeyMap.computeIfPresent(key.toUpperCase(), remappingFunction);
    }

    @Override
    public V compute(String key, BiFunction<? super String, ? super V, ? extends V> remappingFunction) {
        return upperCaseKeyMap.compute(key.toUpperCase(), (t, u) -> remappingFunction.apply(t.toUpperCase(), u));
    }

    @Override
    public V merge(String key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return upperCaseKeyMap.merge(key.toUpperCase(), value, remappingFunction);
    }
}
