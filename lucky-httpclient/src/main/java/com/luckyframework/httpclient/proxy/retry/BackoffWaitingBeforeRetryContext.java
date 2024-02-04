package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.httpclient.proxy.annotations.Retryable;
import com.luckyframework.retry.BackoffWaitBeforeRetry;
import com.luckyframework.retry.TaskResult;

/**
 * 支持时间补偿的重试等待时间策略
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 17:27
 */
public class BackoffWaitingBeforeRetryContext extends RunBeforeRetryContext<Object> {

    @Override
    public void beforeRetry(TaskResult<Object> taskResult) {
        long waitMillis = getAnnotationAttribute(Retryable.ATTRIBUTE_WAIT_MILLIS, long.class);
        double multiplier = getAnnotationAttribute(Retryable.ATTRIBUTE_MULTIPLIER, double.class);
        long maxWaitMillis = getAnnotationAttribute(Retryable.ATTRIBUTE_MAX_WAIT_MILLIS, long.class);
        long minWaitMillis = getAnnotationAttribute(Retryable.ATTRIBUTE_MIN_WAIT_MILLIS, long.class);
        new BackoffWaitBeforeRetry(waitMillis, multiplier, maxWaitMillis, minWaitMillis).beforeRetry(taskResult);
    }
}
