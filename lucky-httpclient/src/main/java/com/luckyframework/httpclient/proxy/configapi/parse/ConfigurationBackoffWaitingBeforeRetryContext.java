package com.luckyframework.httpclient.proxy.configapi.parse;

import com.luckyframework.httpclient.proxy.retry.RunBeforeRetryContext;
import com.luckyframework.retry.BackoffWaitBeforeRetry;
import com.luckyframework.retry.TaskResult;

/**
 * 基于配置的重试等待器
 */
public class ConfigurationBackoffWaitingBeforeRetryContext extends RunBeforeRetryContext<Object> {

    private final BackoffWaitBeforeRetry backoffWaitBeforeRetry;

    public ConfigurationBackoffWaitingBeforeRetryContext(RetryConfiguration retryConfig) {
        this.backoffWaitBeforeRetry = new BackoffWaitBeforeRetry(retryConfig.getWaitMillis(), retryConfig.getMultiplier(), retryConfig.getMaxWaitMillis(), retryConfig.getMinWaitMillis());
    }

    @Override
    protected void doBeforeRetry(TaskResult<Object> taskResult) {
        backoffWaitBeforeRetry.beforeRetry(taskResult);
    }
}
