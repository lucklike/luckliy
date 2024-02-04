package com.luckyframework.retry;

/**
 * 重试之前运行的逻辑
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/20 12:53
 */
@FunctionalInterface
public interface RunBeforeRetry<R> {

    /**
     * 在重试之前运行一段逻辑
     *
     * @param taskResult 上次任务执行的结果
     */
    void beforeRetry(TaskResult<R> taskResult);
}
