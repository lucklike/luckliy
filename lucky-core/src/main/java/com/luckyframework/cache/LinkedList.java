package com.luckyframework.cache;

import java.util.Iterator;


/**
 * 节点value列表
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/10 15:40
 */
public class LinkedList<K, V> implements Iterable<CacheNode<K, V>> {

    private int size;

    private CacheNode<K, V> first;
    private CacheNode<K, V> last;

    public LinkedList() {
        this.first = null;
        this.last = null;
        this.size = 0;
    }

    public boolean containsKey(K k){
        CacheNode<K, V> tempNode = first;
        while (tempNode.getNext() != null){
            if(tempNode.getKey().equals(k)){
                return true;
            }
            tempNode = tempNode.getNext();
        }
        return false;
    }

    public void addFirst(CacheNode<K, V> node) {
        CacheNode<K, V> oldFirst = first;
        first = node;
        first.setPrev(null);
        if (oldFirst == null) {
            last = first;
        } else {
            first.setNext(oldFirst);
            oldFirst.setPrev(first);
        }
        size++;
    }

    public void addLast(CacheNode<K, V> node) {
        // 记录老的last节点
        CacheNode<K, V> oldLast = last;

        // 将last节点直接指向新节点
        last = node;

        // 链表初始化 如果last为空，那么代表链表还没有新元素，则将first指向last进行
        if (oldLast == null) {
            first = last;
        } else {
            // 如果链表已经进行了初始化，那么设置last的上一个节点是老的last节点
            last.setPrev(oldLast);

            // 将老的last节点的下一个节点设置为最新last节点
            oldLast.setNext(last);
        }

        size++;
    }

    /**
     * 删除任意节点
     * @param node 待删除的节点
     * @return 待删除的节点
     */
    public CacheNode<K, V> remove(CacheNode<K, V> node) {
        // 分情况删除
        // 情况1：前驱节点不存在 node.prev == null 为头节点，需要处理头节点
        // 情况2：前驱节点存在   node.prev ！= null 可能为尾节点，可能为中间节点
        // 情况3：后继节点不存在 node.next == null 确定为尾节点
        // 情况4：后继节点存在 node.next != null 可能为头结点，可能为中间节点
        CacheNode<K, V> prev = node.getPrev();
        CacheNode<K, V> next = node.getNext();
        if (prev == null) {
            first = next;
        } else {
            prev.setNext(next);
            node.setPrev(null);
        }

        if (next == null) {
            last = prev;
        } else {
            next.setPrev(prev);
            node.setNext(null);
        }
        size--;
        return node;
    }

    /**
     * 将节点移动到第一个节点
     * @param node 待移动节点
     * @return 待移动节点
     */
    public CacheNode<K, V> moveToFirst(CacheNode<K, V> node) {
        if (node == null) {
            throw new IllegalArgumentException();
        }

        // 先删除掉这个节点
        node = remove(node);

        // 再将该节点添加到第一个上面
        addFirst(node);
        return node;
    }

    /**
     * 删除最后一个节点
     * @return 删除前的最后一个节点
     */
    public CacheNode<K, V> removeFirst() {
        if (first == null) {
            return null;
        }

        CacheNode<K, V> tmpFirst = first;
        CacheNode<K, V> next = tmpFirst.getNext();
        first = next;
        if (next == null) {
            last = null;
        } else {
            next.setPrev(null);
        }

        size--;

        return tmpFirst;
    }

    public CacheNode<K, V> removeLast() {
        CacheNode<K, V> tmpLast = last;
        CacheNode<K, V> prev = tmpLast.getPrev();
        last = prev;
        if (prev == null) {
            first = null;
        } else {
            prev.setNext(null);
        }

        size--;

        return tmpLast;
    }

    public int getSize() {
        return size;
    }

    @Override
    public Iterator<CacheNode<K, V>> iterator() {
        return new LinkedListNodeIterator();
    }

    public void clear() {
        CacheNode<K, V> cur = first;
        while (cur != null) {
            CacheNode<K, V> tmp = cur.getNext();
            cur.setKey(null);
            cur.setValue(null);
            cur.setPrev(null);
            cur.setNext(null);
            cur = tmp;
        }
        size = 0;
    }

    public CacheNode<K, V> getLast() {
        return last;
    }

    public CacheNode<K,V> getFirst() {
        return first;
    }

    private class LinkedListNodeIterator implements Iterator<CacheNode<K, V>> {
        private CacheNode<K, V> cur;

        public LinkedListNodeIterator() {
            this.cur = first;
        }

        @Override
        public boolean hasNext() {
            return cur != null;
        }

        @Override
        public CacheNode<K, V> next() {
            CacheNode<K, V> tmp = cur;
            cur = cur.getNext();
            return tmp;
        }
    }
}
