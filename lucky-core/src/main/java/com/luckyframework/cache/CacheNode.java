package com.luckyframework.cache;

/**
 * 缓存节点,一个双向链表
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/10 15:32
 */
public class CacheNode<K, V> {

    /** 缓存的KEY值*/
    protected volatile K key;
    /** 缓存的VALUE*/
    protected volatile V value;

    /** 上一个节点*/
    private volatile CacheNode<K, V> prev;
    /** 下一个节点*/
    private volatile CacheNode<K, V> next;

    public CacheNode(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public CacheNode<K, V> getPrev() {
        return prev;
    }

    public void setPrev(CacheNode<K, V> prev) {
        this.prev = prev;
    }

    public CacheNode<K, V> getNext() {
        return next;
    }

    public void setNext(CacheNode<K, V> next) {
        this.next = next;
    }

}
