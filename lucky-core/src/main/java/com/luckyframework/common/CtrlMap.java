package com.luckyframework.common;


import com.luckyframework.exception.CtrlMapValueModifiedException;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 插入受控制的Map
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/20 23:33
 */
public class CtrlMap<K, V> implements Map<K, V> {

    private final Map<K, V> delegate;

    private final ModifiedVerifier<K> errVerifier;

    private final ModifiedVerifier<K> ignoreVerifier;

    public CtrlMap(Map<K, V> delegate, ModifiedVerifier<K> errVerifier, ModifiedVerifier<K> ignoreVerifier) {
        this.delegate = delegate;
        this.errVerifier = errVerifier;
        this.ignoreVerifier = ignoreVerifier;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return delegate.get(key);
    }

    @Override
    public V put(K key, V value) {
        if (canItBeModified(key)) {
            return delegate.put(key, value);
        }
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (canItBeModified((K) key)) {
            return delegate.remove(key);
        }
        return null;
    }

    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @NonNull
    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @NonNull
    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return delegate.entrySet().stream().map(CtrlEntry::new).collect(Collectors.toSet());
    }

    /**
     * 判断某个Key是否可以被修改
     *
     * @param k 待判断的KEY
     * @return 是否可以被修改
     */
    private boolean canItBeModified(K k) {
        if (!containsKey(k)) {
            return true;
        }
        if (!errVerifier.can(k)) {
            throw new CtrlMapValueModifiedException("Unable to modify a protected Key: '{}'", k);
        }
        return ignoreVerifier.can(k);
    }


    /**
     * 受控制的Entry
     */
    class CtrlEntry implements Entry<K, V> {

        private final Entry<K, V> entry;

        CtrlEntry(Entry<K, V> entry) {
            this.entry = entry;
        }

        @Override
        public K getKey() {
            return entry.getKey();
        }

        @Override
        public V getValue() {
            return entry.getValue();
        }

        @Override
        public V setValue(V value) {
            if (canItBeModified(getKey())) {
                return entry.setValue(value);
            }
            return value;
        }
    }

    /**
     * 修改检验器
     *
     * @param <T> 原始泛型
     */
    @FunctionalInterface
    public interface ModifiedVerifier<T> {

        /**
         * 判断否个元素是否可以被修改
         *
         * @param element 元素
         * @return 是否可以被修改
         */
        boolean can(T element);
    }
}
