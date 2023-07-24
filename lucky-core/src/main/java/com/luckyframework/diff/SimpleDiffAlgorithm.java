package com.luckyframework.diff;

/**
 * 简单的比较算法
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/6/4 06:59
 */
public class SimpleDiffAlgorithm<T> implements DiffAlgorithm<T> {
    @Override
    public boolean hasDiff(T obj1, T obj2) {
        if (obj1 == obj2) {
            return false;
        }
        if (obj1 == null || obj2 == null) {
            return true;
        }
        return !obj1.equals(obj2);
    }
}
