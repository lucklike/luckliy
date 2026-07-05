package com.luckyframework.common;

import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private boolean enableAddFirst = true;
    private boolean enableAddLast = true;

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
        if (!enableAddFirst) {
            throw new UnsupportedOperationException("addFirst() method has been disabled");
        }
        if (ContainerUtils.isNotEmptyMap(map)) {
            mutableMapList.add(0, map);
        }
    }

    public void addLast(@NonNull Map<K, V> map) {
        if (!enableAddLast) {
            throw new UnsupportedOperationException("addLast() method has been disabled");
        }
        if (ContainerUtils.isNotEmptyMap(map)) {
            mutableMapList.add(map);
        }
    }

    public void disabledAddFirst() {
        enableAddFirst = false;
    }

    public void disabledAddLast() {
        enableAddLast = false;
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
        V firstValue = null;
        int firstIndex = -1;

        // 找到第一个存在的 key 及其位置
        for (int i = 0; i < mutableMapList.size(); i++) {
            Map<K, V> map = mutableMapList.get(i);
            if (map.containsKey(key)) {
                firstValue = map.get(key);
                firstIndex = i;
                break;
            }
        }

        // 如果不存在，返回 null
        if (firstValue == null) {
            return null;
        }

        // 如果不需要合并，直接返回
        if (!needMergeType(firstValue)) {
            return firstValue;
        }

        // 需要合并，继续从下一个 Map 开始找相同类型的值
        List<V> valuesToMerge = new ArrayList<>();
        valuesToMerge.add(firstValue);

        for (int i = firstIndex + 1; i < mutableMapList.size(); i++) {
            Map<K, V> map = mutableMapList.get(i);
            if (map.containsKey(key)) {
                V currentValue = map.get(key);
                // 检查类型是否相同
                if (currentValue != null && Objects.equals(currentValue.getClass(), firstValue.getClass())) {
                    valuesToMerge.add(currentValue);
                }
            }
        }

        // 合并所有收集到的值
        return mergeValues(valuesToMerge);
    }

    /**
     * 合并多个值
     *
     * @param values 需要合并的值列表
     * @return 合并后的结果
     */
    @SuppressWarnings("unchecked")
    private V mergeValues(List<V> values) {
        if (values.isEmpty()) {
            return null;
        }

        if (values.size() == 1) {
            return values.get(0);
        }

        V first = values.get(0);

        // 合并 Map
        if (first instanceof Map) {
            Map<Object, Object> result = new HashMap<>();
            for (V value : values) {
                result.putAll((Map<?, ?>) value);
            }
            return (V) result;
        }

        // 合并 Collection
        if (first instanceof Collection) {
            Collection<Object> result = new ArrayList<>();
            for (V value : values) {
                result.addAll((Collection<?>) value);
            }
            return (V) result;
        }

        // 合并数组
        if (first.getClass().isArray()) {
            // 获取数组组件类型
            Class<?> componentType = first.getClass().getComponentType();

            // 计算总长度
            int totalLength = 0;
            for (V value : values) {
                totalLength += java.lang.reflect.Array.getLength(value);
            }

            // 创建新数组
            Object result = java.lang.reflect.Array.newInstance(componentType, totalLength);

            // 复制数组元素
            int destPos = 0;
            for (V value : values) {
                int length = java.lang.reflect.Array.getLength(value);
                System.arraycopy(value, 0, result, destPos, length);
                destPos += length;
            }
            return (V) result;
        }

        // 其他类型，返回第一个值
        return first;
    }

    /**
     * 是否为需要合并的类型
     *
     * @param value 值
     * @return 是否为需要合并的类型
     */
    private boolean needMergeType(Object value) {
        return (value instanceof Map)
                || (value instanceof Collection)
                || (value.getClass().isArray());
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
