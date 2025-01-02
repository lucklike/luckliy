package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.retry.RunBeforeRetryContext;
import com.luckyframework.retry.BackoffWaitBeforeRetry;
import com.luckyframework.retry.TaskResult;

/**
 * 支持时间补偿的重试等待时间策略
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 17:27
 */
public class ConfigApiBackoffWaitingBeforeRetryContext extends RunBeforeRetryContext<Object> {

    private long waitMillis = 1000L;
    private double multiplier = 0D;
    private long maxWaitMillis = 10000L;
    private long minWaitMillis = 500L;

    public void setWaitMillis(long waitMillis) {
        this.waitMillis = waitMillis;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public void setMaxWaitMillis(long maxWaitMillis) {
        this.maxWaitMillis = maxWaitMillis;
    }

    public void setMinWaitMillis(long minWaitMillis) {
        this.minWaitMillis = minWaitMillis;
    }

    @Override
    public void doBeforeRetry(TaskResult<Object> taskResult) {
        new BackoffWaitBeforeRetry(waitMillis, multiplier, maxWaitMillis, minWaitMillis).beforeRetry(taskResult);
    }
}
