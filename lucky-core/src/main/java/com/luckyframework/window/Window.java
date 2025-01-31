package com.luckyframework.window;

import java.util.Collection;

public interface Window<T> {

    /**
     * 添加元素
     *
     * @param element 元素
     */
    void addElement(T element);

    /**
     * 向前滑动
     *
     * @param unit 滑动单位
     */
    void slideForward(long unit);

    /**
     * 窗口是否已经装满
     *
     * @return 是否已经装满
     */
    boolean isFull();

    /**
     * 返回窗体中元素的个数
     *
     * @return 窗体中元素的个数
     */
    int size();

    /**
     * 获取窗口中的所有元素
     *
     * @return 窗口中的所有元素
     */
    Collection<T> getElements();

    /**
     * 清空所有元素
     */
    void clear();

}
