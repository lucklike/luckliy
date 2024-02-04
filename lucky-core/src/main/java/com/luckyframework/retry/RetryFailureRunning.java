package com.luckyframework.retry;

/**
 * 重试失败之后执行
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/20 14:18
 */
@FunctionalInterface
public interface RetryFailureRunning<R> {

    R failureRunning(TaskResult<R> taskResult);
}
