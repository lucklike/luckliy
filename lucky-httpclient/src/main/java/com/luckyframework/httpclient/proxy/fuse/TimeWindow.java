package com.luckyframework.httpclient.proxy.fuse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TimeWindow<T> implements Window<T> {

    private final List<TimeNode<T>> list;
    private final long timeInterval;
    private long minTime = 0L;
    private long maxTime = 0L;

    public TimeWindow(long timeInterval) {
        this.list = Collections.synchronizedList(new ArrayList<>());
        this.timeInterval = timeInterval;
    }

    @Override
    public void addElement(T element) {
        long time = System.currentTimeMillis();
        if (isFull()) {
            minTime = maxTime - timeInterval;
            list.removeIf(td -> td.getTime() < minTime);
        }
        list.add(new TimeNode<>(time, element));
        maxTime = time;
        if (minTime == 0L) {
            minTime = time;
        }
    }

    @Override
    public boolean isFull() {
        return maxTime - minTime >= timeInterval;
    }

    @Override
    public int size() {
        return list.size();
    }


    @Override
    public Collection<T> getElements() {
        return list.stream().map(TimeNode::getElement).collect(Collectors.toList());
    }

    @Override
    public void clear() {
        list.clear();
    }

    static class TimeNode<T> {
        private final long time;
        private final T element;

        TimeNode(long time, T element) {
            this.time = time;
            this.element = element;
        }

        public long getTime() {
            return time;
        }

        public T getElement() {
            return element;
        }
    }
}
