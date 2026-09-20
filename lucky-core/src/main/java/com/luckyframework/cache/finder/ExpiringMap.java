package com.luckyframework.cache.finder;

import com.luckyframework.threadpool.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 到期自动删除的Map
 *
 * <p>每个条目可以携带各自的过期时间(绝对毫秒时间戳)，过期时间小于0表示永不过期。
 * 过期条目的删除采用"惰性删除 + 周期清理"的方式：
 * <ul>
 *     <li>惰性删除：读取(get/containsKey/getNotExpired等)时若发现条目已过期则立即删除</li>
 *     <li>周期清理：内部维护一份按过期时间有序的索引，清理时只处理已经到期的时间段，
 *     不需要对Map进行全量扫描；该任务由所有实例共享的守护线程调度器执行</li>
 * </ul>
 *
 * <p>{@link #size()}、{@link #isEmpty()}、{@link #keySet()}、{@link #values()}、
 * {@link #entrySet()}等视图均不包含已过期的条目。
 *
 * <p>调用{@link #close()}后会取消周期清理任务(幂等)，Map仍可正常使用，
 * 但过期条目只会在读取时被惰性删除，或手动调用{@link #clearExpired()}时被清理。
 *
 * @author fukang
 * @version 1.1.0
 * @date 2024/3/7 23:29
 */
public class ExpiringMap<K, V> implements Map<K, V>, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ExpiringMap.class);

    /**
     * 所有实例共享的清理调度器：单个守护线程，不会阻止JVM退出
     */
    private static final ScheduledExecutorService CLEANER_SCHEDULER = createCleanerScheduler();

    /**
     * 数据存储
     */
    private final Map<K, Node<V>> cacheMap = new ConcurrentHashMap<>(16);

    /**
     * 过期时间索引：expireTime -> keys。按过期时间有序，清理时只需处理已到期的时间段，
     * 复杂度与实际过期条目数相关，而不是与Map总量相关
     */
    private final ConcurrentSkipListMap<Long, Set<K>> expireIndex = new ConcurrentSkipListMap<>();

    /**
     * 当前实例的周期清理任务
     */
    private final ScheduledFuture<?> cleaningFuture;

    public ExpiringMap(int cleaningIntervalSeconds, int initialDelaySeconds) {
        Assert.isTrue(cleaningIntervalSeconds > 0, "'cleaningIntervalSeconds' must be greater than 0.");
        Assert.isTrue(initialDelaySeconds >= 0, "'initialDelaySeconds' cannot be less than 0.");
        CleaningTask cleaningTask = new CleaningTask(this);
        this.cleaningFuture = CLEANER_SCHEDULER.scheduleWithFixedDelay(cleaningTask, initialDelaySeconds, cleaningIntervalSeconds, TimeUnit.SECONDS);
        cleaningTask.setFuture(this.cleaningFuture);
    }

    public ExpiringMap() {
        this(5, 3);
    }

    /**
     * 存入缓存，在延迟delayedDeletionMillis毫秒后过期
     */
    public void putFixedTimeRemove(K key, V value, long delayedDeletionMillis) {
        put(key, value, System.currentTimeMillis() + delayedDeletionMillis);
    }

    /**
     * 存入缓存，在expiredDate时刻过期
     */
    public void put(K key, V value, Date expiredDate) {
        put(key, value, expiredDate.getTime());
    }

    /**
     * 存入缓存，在expiredMillis时刻过期，小于0表示永不过期
     */
    public void put(K key, V value, long expiredMillis) {
        putNode(key, new Node<>(expiredMillis, value));
    }

    /**
     * 获取未过期的值，key不存在或者已过期时返回null，已过期的条目会被立即删除
     */
    public V getNotExpired(K key) {
        return getData(getLiveNode(key));
    }

    /**
     * key存在且未过期时返回true，已过期的条目会被立即删除
     */
    public boolean hasNotExpired(K key) {
        return getLiveNode(key) != null;
    }

    /**
     * 清理所有已过期的条目
     */
    public void clearExpired() {
        long now = System.currentTimeMillis();
        while (true) {
            Map.Entry<Long, Set<K>> bucket = this.expireIndex.firstEntry();
            if (bucket == null || bucket.getKey() > now) {
                return;
            }
            // 先原子摘除该时间桶，之后写入相同过期时间的条目会进入新的时间桶，不会被本次清理误伤
            if (!this.expireIndex.remove(bucket.getKey(), bucket.getValue())) {
                continue;
            }
            for (K key : new ArrayList<>(bucket.getValue())) {
                Node<V> node = this.cacheMap.get(key);
                // 条件删除：只删除过期时间与该时间桶一致(防止误删被重新put刷新了过期时间的节点)且确实已经过期的节点
                if (node != null && node.getExpiredMillis() == bucket.getKey() && node.isExpired(now)) {
                    this.cacheMap.remove(key, node);
                }
            }
        }
    }

    /**
     * 未过期条目的数量
     */
    public long notExpiredSize() {
        return this.cacheMap.values().stream().filter(node -> !node.isExpired()).count();
    }

    /**
     * 未过期条目的key集合
     */
    public Set<K> notExpiredKeySet() {
        return this.cacheMap.entrySet().stream().filter(e -> !e.getValue().isExpired()).map(Entry::getKey).collect(Collectors.toSet());
    }

    /**
     * 未过期条目的value集合
     */
    public Set<V> notExpiredValues() {
        return this.cacheMap.values().stream().filter(node -> !node.isExpired()).map(Node::getData).collect(Collectors.toSet());
    }

    /**
     * 未过期条目的Entry集合
     */
    public Set<Entry<K, V>> notExpiredEntrySet() {
        return this.cacheMap.entrySet().stream().filter(e -> !e.getValue().isExpired()).map(KVEntry::new).collect(Collectors.toSet());
    }

    /**
     * 关闭周期清理任务(幂等)，关闭后Map仍可正常使用，但不再自动清理过期条目
     */
    @Override
    public void close() {
        this.cleaningFuture.cancel(false);
    }

    //------------------------------------------------------
    //                    Map methods
    //------------------------------------------------------

    @Override
    public int size() {
        return (int) notExpiredSize();
    }

    @Override
    public boolean isEmpty() {
        return notExpiredSize() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        return getLiveNode(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new IllegalArgumentException("This method is not supported.");
    }

    @Override
    public V get(Object key) {
        return getData(getLiveNode(key));
    }

    /**
     * 存入一个永不过期的缓存
     */
    @Override
    public V put(K key, V value) {
        return getData(putNode(key, new Node<>(-1, value)));
    }

    @Override
    public V remove(Object key) {
        Node<V> oldNode = this.cacheMap.remove(key);
        if (oldNode != null && oldNode.hasExpiry()) {
            removeIndex(oldNode.getExpiredMillis(), key);
        }
        return getData(oldNode);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        map.forEach(this::put);
    }

    @Override
    public void clear() {
        this.cacheMap.clear();
        this.expireIndex.clear();
    }

    @Override
    @NonNull
    public Set<K> keySet() {
        return notExpiredKeySet();
    }

    @Override
    @NonNull
    public Collection<V> values() {
        return notExpiredValues();
    }

    @Override
    @NonNull
    public Set<Entry<K, V>> entrySet() {
        return notExpiredEntrySet();
    }

    /**
     * 获取未过期的节点，过期的节点会被立即删除(惰性删除)
     */
    private Node<V> getLiveNode(Object key) {
        Node<V> node = this.cacheMap.get(key);
        if (node == null || !node.isExpired()) {
            return node;
        }
        // 条件删除，防止误删被重新put刷新了过期时间的节点
        this.cacheMap.remove(key, node);
        return null;
    }

    /**
     * 存放节点，并同步维护过期时间索引
     */
    private Node<V> putNode(K key, Node<V> newNode) {
        Node<V> oldNode = this.cacheMap.put(key, newNode);
        if (oldNode != null && oldNode.hasExpiry() && oldNode.getExpiredMillis() != newNode.getExpiredMillis()) {
            removeIndex(oldNode.getExpiredMillis(), key);
        }
        if (newNode.hasExpiry()) {
            addIndex(newNode.getExpiredMillis(), key);
        }
        return oldNode;
    }

    private void addIndex(long expiredMillis, K key) {
        this.expireIndex.computeIfAbsent(expiredMillis, time -> ConcurrentHashMap.newKeySet()).add(key);
    }

    private void removeIndex(long expiredMillis, Object key) {
        Set<K> keys = this.expireIndex.get(expiredMillis);
        if (keys != null) {
            keys.remove(key);
        }
    }

    private V getData(Node<V> node) {
        return node == null ? null : node.getData();
    }

    private static ScheduledExecutorService createCleanerScheduler() {
        NamedThreadFactory threadFactory = new NamedThreadFactory("lucky-expiring-map-cleaner-");
        threadFactory.setDaemon(true);
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    /**
     * 周期清理任务，只持有Map的弱引用：Map被GC之后任务会自动取消，
     * 避免共享的静态调度器一直被已废弃的Map实例引用
     */
    private static final class CleaningTask implements Runnable {

        private final WeakReference<ExpiringMap<?, ?>> mapReference;

        private final AtomicReference<ScheduledFuture<?>> futureReference = new AtomicReference<>();

        CleaningTask(ExpiringMap<?, ?> expiringMap) {
            this.mapReference = new WeakReference<>(expiringMap);
        }

        void setFuture(ScheduledFuture<?> future) {
            this.futureReference.set(future);
        }

        @Override
        public void run() {
            ExpiringMap<?, ?> expiringMap = this.mapReference.get();
            if (expiringMap == null) {
                ScheduledFuture<?> future = this.futureReference.get();
                if (future != null) {
                    future.cancel(false);
                }
                return;
            }
            try {
                expiringMap.clearExpired();
            } catch (Throwable t) {
                // 捕获所有异常，防止周期任务被静默取消
                logger.error("An exception occurred during the cleanup of ExpiringMap.", t);
            }
        }
    }

    static class Node<V> {

        /**
         * 过期时间, 小于0表示永不过期
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

        public boolean hasExpiry() {
            return expiredMillis >= 0;
        }

        public boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        public boolean isExpired(long currentMillis) {
            return hasExpiry() && currentMillis > expiredMillis;
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
}
