package com.luckyframework.diff;

import java.util.List;

/**
 * 更新差异处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/6/4 05:59
 */
@FunctionalInterface
public interface DiffInfoHandler<T> {

    void diffInfoHandle(List<Difference.DiffInfo<T>> updateDiffInfo);
}
