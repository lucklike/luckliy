package com.luckyframework.diff;

/**
 * 比较算法
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/6/4 06:39
 */
@FunctionalInterface
public interface DiffAlgorithm<T> {

    /**
     * 判断两个实体是否有差异
     *
     * @param obj1 实体1
     * @param obj2 实体2
     * @return 是否有差异
     */
    boolean hasDiff(T obj1, T obj2);
}
