package com.luckyframework.retry;

/**
 * 重试决策者，用于决策是否需要进行重试
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/20 12:58
 */
public interface RetryDecider<R> {

    /**
     * 根据任务的运行结果来判断是否需要进行重试
     *
     * @param taskResult 任务运行结果
     * @return 是否需要重试
     */
    boolean needRetry(TaskResult<R> taskResult);
}
