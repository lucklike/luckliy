package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.retry.RunBeforeRetry;
import com.luckyframework.retry.TaskResult;

/**
 * 携带上下文的重试执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 16:15
 */
public abstract class RunBeforeRetryContext<T> extends RetryContext implements RunBeforeRetry<T> {

    @Override
    public void beforeRetry(TaskResult<T> taskResult) {
        getContext().useHook(Lifecycle.RETRY);
        doBeforeRetry(taskResult);
    }

    protected abstract void doBeforeRetry(TaskResult<T> taskResult);
}
