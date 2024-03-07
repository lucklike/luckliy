package com.luckyframework.cache.finder;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 到期自动删除的Map
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/7 23:29
 */
public class ExpiringMap<K, V> implements Map<K, V> {

    private final Map<K, Node<V>> cacheMap = new ConcurrentHashMap<>(16);


    public ExpiringMap(int cleaningIntervalSeconds) {
        Assert.isTrue(cleaningIntervalSeconds > 0, "'cleaningIntervalSeconds' cannot be less than 0.");
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(this::clearExpired, 0, cleaningIntervalSeconds, TimeUnit.SECONDS);
    }

    public ExpiringMap() {
        this(10);
    }

    public void putFixedTimeRemove(K key, V value, long delayedDeletionMillis) {
        put(key, value, new Date().getTime() + delayedDeletionMillis);
    }

    public void put(K key, V value, Date expiredDate) {
        put(key, value, expiredDate.getTime());
    }

    public void put(K key, V value, long expiredMillis) {
        this.cacheMap.put(key, new Node<>(expiredMillis, value));
    }

    public V getNotExpired(K key) {
        Node<V> node = this.cacheMap.get(key);
        return node == null || node.isExpired() ? null : node.getData();
    }

    public boolean hasNotExpired(K key) {
        Node<V> node = this.cacheMap.get(key);
        return node != null && node.isExpired();
    }

    public void clearExpired() {
        System.out.println("清理");
        this.cacheMap.entrySet().removeIf(kNodeEntry -> kNodeEntry.getValue().isExpired());
    }

    public long notExpiredSize() {
        return this.cacheMap.values().stream().filter(n->!n.isExpired()).count();
    }


    //------------------------------------------------------
    //                    Map methods
    //------------------------------------------------------

    @Override
    public int size() {
        return this.cacheMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.cacheMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.cacheMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new IllegalArgumentException("This method is not supported.");
    }

    @Override
    public V get(Object key) {
        return getData(this.cacheMap.get(key));
    }

    @Override
    public V put(K key, V value) {
        return getData(this.cacheMap.put(key, new Node<>(-1, value)));
    }

    @Override
    public V remove(Object key) {
        return getData(this.cacheMap.remove(key));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        map.forEach(this::put);
    }

    @Override
    public void clear() {
        this.cacheMap.clear();
    }

    @Override
    @NonNull
    public Set<K> keySet() {
        return this.cacheMap.keySet();
    }

    @Override
    @NonNull
    public Collection<V> values() {
        return this.cacheMap.values().stream().map(Node::getData).collect(Collectors.toList());
    }

    @Override
    @NonNull
    public Set<Entry<K, V>> entrySet() {
        return this.cacheMap.entrySet().stream().map(entry -> new Entry<K, V>() {

            @Override
            public K getKey() {
                return entry.getKey();
            }

            @Override
            public V getValue() {
                return getData(entry.getValue());
            }

            @Override
            public V setValue(V value) {
                return getData(entry.setValue(new Node<>(-1, value)));
            }

        }).collect(Collectors.toSet());
    }

    private V getData(Node<V> node) {
        return node == null ? null : node.getData();
    }

    static class Node<V> {

        /**
         * 过期时间
         */
        private final long expiredMillis;

        private final V data;

        public Node(long expiredMillis, V data) {
            this.expiredMillis = expiredMillis;
            this.data = data;
        }

        public long getExpiredMillis() {
            return expiredMillis;
        }

        public V getData() {
            return data;
        }

        public boolean isExpired() {
            if (expiredMillis < 0) {
                return false;
            }
            return new Date().getTime() < expiredMillis;
        }
    }


    public static void main(String[] args) {
        ExpiringMap<String, String> expiringMap = new ExpiringMap<>(2);
        for (int i = 0; i < 100; i++) {
            expiringMap.putFixedTimeRemove("key"+i, "value"+i, i * 1000);
        }

        while (expiringMap.notExpiredSize() > 0) {
            System.out.print("\r"+expiringMap.notExpiredSize()+"/"+expiringMap.size());
        }

    }
}
