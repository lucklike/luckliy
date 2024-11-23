package com.luckyframework.common;


/**
 * 修改检验器
 *
 * @param <T> 原始泛型
 */
@FunctionalInterface
public interface ModifiedVerifier<T> {

    /**
     * 判断否个元素是否可以被修改
     *
     * @param element 元素
     * @return 是否可以被修改
     */
    boolean can(T element);
}