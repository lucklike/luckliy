package com.luckyframework.httpclient.proxy.fuse;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LengthWindow<T> implements Window<T> {

    private final List<T> list;
    private final int maxSize;

    public LengthWindow(int length) {
        this.list = Collections.synchronizedList(new LinkedList<>());
        this.maxSize = length;
    }

    @Override
    public void addElement(T element) {
        // 如果满了则移除队尾元素
        if (isFull()) {
            list.remove(maxSize - 1);
        }
        list.add(element);
    }

    @Override
    public void slideForward(long unit) {
        if (unit <= 0) {
            return;
        }
        if (unit >= maxSize) {
            clear();
            return;
        }
        this.list.subList(0, (int) unit).clear();
    }


    @Override
    public boolean isFull() {
        return size() == maxSize;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Collection<T> getElements() {
        return list;
    }

    @Override
    public void clear() {
        list.clear();
    }

    /**
     * 获取窗口容量
     *
     * @return 窗口容量
     */
    public int capacity() {
        return maxSize;
    }
}
