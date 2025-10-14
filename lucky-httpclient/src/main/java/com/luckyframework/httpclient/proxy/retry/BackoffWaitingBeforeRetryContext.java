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
    public void doBeforeRetry(TaskResult<Object> taskResult) {
        Retryable retryableAnn = toAnnotation(Retryable.class);
        long waitMillis = parseExpression(retryableAnn.waitMillis(), long.class);
        double multiplier = parseExpression(retryableAnn.multiplier(), double.class);
        long maxWaitMillis = parseExpression(retryableAnn.maxWaitMillis(), long.class);
        long minWaitMillis = parseExpression(retryableAnn.minWaitMillis(), long.class);
        new BackoffWaitBeforeRetry(waitMillis, multiplier, maxWaitMillis, minWaitMillis).beforeRetry(taskResult);
    }
}
