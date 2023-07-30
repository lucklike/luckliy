package com.luckyframework.diff;

import java.util.List;

/**
 * 差异处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/20 00:26
 */
@FunctionalInterface
public interface DiffHandler<T> {

    /**
     * 处理存在差异的对象
     *
     * @param diffList 存在差异的对象集合
     */
    void diffHandle(List<T> diffList);
}
