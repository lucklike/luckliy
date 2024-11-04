package com.luckyframework.httpclient.proxy.fuse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LengthWindow<T> implements Window<T> {

    private final List<T> list;
    private final int maxSize;

    public LengthWindow(int length) {
        this.list = new ArrayList<>(length);
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
    public boolean isFull() {
        return list.size() == maxSize;
    }

    @Override
    public Collection<T> getElements() {
        return list;
    }

    @Override
    public void clear() {
        list.clear();
    }
}
