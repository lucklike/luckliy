package com.luckyframework.cache.finder;

import com.luckyframework.common.Console;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Date;
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
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private final Map<K, Node<V>> cacheMap = new ConcurrentHashMap<>(16);


    public ExpiringMap(int cleaningIntervalSeconds, int initialDelaySeconds) {
        Assert.isTrue(cleaningIntervalSeconds > 0, "'cleaningIntervalSeconds' cannot be less than 0.");
        Assert.isTrue(initialDelaySeconds >= 0, "'initialDelaySeconds' cannot be less than 0.");
        executor.scheduleAtFixedRate(this::clearExpired, initialDelaySeconds, cleaningIntervalSeconds, TimeUnit.SECONDS);
    }

    public ExpiringMap() {
        this(5, 3);
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
        this.cacheMap.entrySet().removeIf(kNodeEntry -> kNodeEntry.getValue().isExpired());
    }

    public long notExpiredSize() {
        return this.cacheMap.values().stream().filter(n->!n.isExpired()).count();
    }

    public Set<K> notExpiredKeySet() {
        return this.cacheMap.entrySet().stream().filter(e -> !e.getValue().isExpired()).map(Entry::getKey).collect(Collectors.toSet());
    }

    public Set<V> notExpiredValues() {
        return  this.cacheMap.values().stream().filter(vNode -> !vNode.isExpired()).map(Node::getData).collect(Collectors.toSet());
    }

    public Set<Entry<K, V>> notExpiredEntrySet() {
        return this.cacheMap.entrySet().stream().filter(e -> !e.getValue().isExpired()).map(KVEntry::new).collect(Collectors.toSet());
    }

    public void close() {
        executor.shutdown();
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
        return this.cacheMap.entrySet().stream().map(KVEntry::new).collect(Collectors.toSet());
    }

    private V getData(Node<V> node) {
        return node == null ? null : node.getData();
    }

    static class Node<V> {

        /**
         * 过期时间
         */
        private long expiredMillis;

        private V data;

        public Node(long expiredMillis, V data) {
            this.expiredMillis = expiredMillis;
            this.data = data;
        }

        public void setExpiredMillis(long expiredMillis) {
            this.expiredMillis = expiredMillis;
        }

        public void setData(V data) {
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
            return new Date().getTime() > expiredMillis;
        }
    }

    static class KVEntry<K, V> implements Entry<K, V> {

        private final Entry<K, Node<V>> nodeEntry;

        KVEntry(Entry<K, Node<V>> nodeEntry) {
            this.nodeEntry = nodeEntry;
        }

        @Override
        public K getKey() {
            return nodeEntry.getKey();
        }

        @Override
        public V getValue() {
            return nodeEntry.getValue().getData();
        }

        @Override
        public V setValue(V value) {
            Node<V> node = nodeEntry.getValue();
            V oldData = node.getData();
            node.setData(value);
            nodeEntry.setValue(node);
            return oldData;
        }
    }


    public static void main(String[] args) throws InterruptedException {
        ExpiringMap<String, String> expiringMap = new ExpiringMap<>(5, 0);
        for (int i = 0; i < 100; i++) {
            expiringMap.putFixedTimeRemove("key"+i, "value"+i, i * 1000);
        }
        while (expiringMap.notExpiredSize() > 0) {
            Console.print("\r{}/{}", expiringMap.notExpiredSize(), expiringMap.size());
        }
        expiringMap.close();
    }
}
