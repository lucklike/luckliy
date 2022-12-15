package com.luckyframework.cache.impl;

import com.luckyframework.cache.Cache;
import com.luckyframework.cache.CacheNode;
import com.luckyframework.cache.LinkedList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LFU缓存 最不经常使用缓存算法
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/10 10:10
 */
public class LFUCache<K, V> implements Cache<K, V> {

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<K, LFUCacheNode<K, V>> indexTable;
    private final Map<Integer, LFUCacheNodeList<K, V>> freqMap;
    private final int capacity;
    private LFUCacheNodeList<K, V> first;

    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.indexTable = new ConcurrentHashMap<>(capacity);
        this.freqMap = new ConcurrentHashMap<>(capacity);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof LFUCache){
            return indexTable.equals(((LFUCache)obj).indexTable);
        }
        return false;

    }

    private void evictCacheNode() {
        // 直接从最小计数链表中删除第一个
        LFUCacheNodeList<K, V> list = first;
        CacheNode<K, V> node = list.removeFirst();

        // 如果删除完成后该计数链表没有缓存节点，则将计数节点删除
        // 这里没有更新first，容量满时触发删除节点导致first更新时，说明有计数为1的节点要加入到frequency表
        if (list.size() == 0) {
            first = list.getNext();
            list.setNext(null);
            freqMap.remove(list.getFrequency());
        }
        // 将索引表中的缓存节点删除
        indexTable.remove(node.getKey());
    }

    private void doPromote(K k, V v, LFUCacheNode<K, V> node) {
        // 从frequency表中获取链表，并从链表中删除数据
        int frequency = node.getFrequency();
        LFUCacheNodeList<K, V> list = freqMap.get(frequency);
        list.remove(node);

        // 节点计数更新
        node.setFrequency(node.getFrequency() + 1);
        // 从下一个frequency表中获取下一个计数列表
        LFUCacheNodeList<K, V> nextList = freqMap.get(node.getFrequency());
        // 将节点放入到下一个节点列表
        if (nextList == null) {
            nextList = new LFUCacheNodeList<>(node.getFrequency());
        }
        nextList.addLast(node);
        freqMap.put(node.getFrequency(), nextList);
        node.setValue(v);

        // 前一个frequency表中的链表已经没有数据，那么我们就更新first指针
        if (list.size() == 0) {
            list.setPrev(null);
            list.setNext(null);
            freqMap.remove(frequency);
            // 更新first指针
            // 更新条件： 如果要删除的list==first，则更新first为next
            if (list == first) {
                first = nextList;
            }
        }

        indexTable.put(k, node);
    }


    @Override
    public V get(K k) {
        lock.lock();
        try {
            // 从索引表获取缓存节点
            // 如果缓存节点存在那么就提升缓存节点的计数，并将节点添加到下一个计数链表中
            LFUCacheNode<K, V> node = indexTable.get(k);
            if (node != null) {
                doPromote(k, node.getValue(), node);
                indexTable.put(k, node);
                return node.getValue();
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V put(K k, V v) {
        // 基本参数检查
        if (k == null) {
            throw new IllegalArgumentException("The key of the LFU Cache cannot be null.");
        }

        lock.lock();
        try {
            // 尝试从缓存索引表中查找key是否存在
            LFUCacheNode<K, V> node = indexTable.get(k);
            // 不存在则检查缓存容量是否超过了最大容量
            if (node == null) {
                // 如果当前缓存节点大小超过了容量，则执行删除
                if (size() == capacity) {
                    evictCacheNode();
                }

                // 新建一个缓存节点并将缓存节点添加到LFU双向链表中
                node = new LFUCacheNode<>(k, v);
                LFUCacheNodeList<K, V> list = freqMap.get(node.getFrequency());
                if (list == null) {
                    list = new LFUCacheNodeList<>(node.getFrequency());
                    if (first == null || first.getFrequency() != 1) {
                        first = list;
                    }
                }

                // 将缓存节点添加到frequency表中
                freqMap.put(node.getFrequency(), list);
                node = list.addLast(node);
                indexTable.put(k, node);
            } else {
                // 根据计数获取列表
                // 从当前列表中删除
                // 如果list的数量为空，从map中删除列表
                // 如果list的数量不为空，则不处从map中删除列表
                doPromote(k, v, node);
            }
            return node.getValue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsKey(K k) {
        return indexTable.containsKey(k);
    }

    @Override
    public V remove(K k) {
        if(!containsKey(k)){
            return null;
        }
        lock.lock();
        try {
            LFUCacheNode<K, V> removeEntry = indexTable.remove(k);
            for (Map.Entry<Integer, LFUCacheNodeList<K, V>> entry : freqMap.entrySet()) {
                LFUCacheNodeList<K, V> list = entry.getValue();
                if(list.containsKey(k)){
                    list.remove(removeEntry);
                }
            }
            return removeEntry.getValue();
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            indexTable.clear();
            for (Map.Entry<Integer, LFUCacheNodeList<K, V>> entry : freqMap.entrySet()) {
                LFUCacheNodeList<K, V> list = entry.getValue();
                CacheNode<K, V> node;
                do {
                    node = list.removeFirst();
                } while (node != null);
            }
            freqMap.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return indexTable.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("LFUCache{");
        indexTable.values().forEach(node -> sb.append(node.getKey()).append("=").append(node.getValue()).append(", "));
        String toString = sb.toString();
        toString = toString.endsWith(", ") ? toString.substring(0, toString.length()-2) : toString;
        return toString + "}";
    }

    /**
     * LFU缓存节点
     * @param <K>
     * @param <V>
     */
    static class LFUCacheNode<K, V> extends CacheNode<K, V> {

        /** 使用评率*/
        private volatile int frequency;

        public LFUCacheNode(K key, V value) {
            this(key, value, 1);
        }

        public LFUCacheNode(K key, V value, int frequency) {
            super(key, value);
            this.frequency = frequency;
        }

        public int getFrequency() {
            return frequency;
        }

        public void setFrequency(int frequency) {
            this.frequency = frequency;
        }

        @Override
        public String toString() {
            return "LFUCacheNode{" +
                    "frequency=" + frequency +
                    ", key=" + key +
                    ", value=" + value +
                    '}';
        }
    }

    static class LFUCacheNodeList<K, V> {

        private final int frequency;

        private final LinkedList<K, V> list;

        private volatile LFUCacheNodeList<K, V> prev;

        private volatile LFUCacheNodeList<K, V> next;

        public LFUCacheNodeList(int frequency) {
            this.frequency = frequency;
            this.list = new LinkedList<>();
        }

        public LFUCacheNode<K, V> addLast(LFUCacheNode<K, V> node) {
            list.addLast(node);
            return node;
        }

        public int getFrequency() {
            return frequency;
        }

        public LFUCacheNodeList<K, V> getPrev() {
            return prev;
        }

        public void setPrev(LFUCacheNodeList<K, V> prev) {
            this.prev = prev;
        }

        public LFUCacheNodeList<K, V> getNext() {
            return next;
        }

        public void setNext(LFUCacheNodeList<K, V> next) {
            this.next = next;
        }

        public int size() {
            return list.getSize();
        }

        public void remove(LFUCacheNode<K, V> node) {
            list.remove(node);
        }

        public boolean containsKey(K k){
            return list.containsKey(k);
        }

        public CacheNode<K, V> removeFirst() {
            return list.removeFirst();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();

            builder.append("LFUCacheNodeList{" + "frequency=").append(frequency).append('}');
            for (Object node : list) {
                builder.append(node);
            }

            return builder.toString();
        }
    }

}
