package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.httpclient.proxy.spel.hook.Lifecycle;
import com.luckyframework.retry.RunBeforeRetry;
import com.luckyframework.retry.TaskResult;

import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$RETRY_COUNT$_;
import static com.luckyframework.httpclient.proxy.spel.OrdinaryVarName._$TASK_RESULT$_;

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
        getContextVar().addRootVariable(_$TASK_RESULT$_, taskResult);
        getContextVar().addRootVariable(_$RETRY_COUNT$_, taskResult.getRetryNum());
        getContext().useHook(Lifecycle.RETRY);
        doBeforeRetry(taskResult);
    }

    protected abstract void doBeforeRetry(TaskResult<T> taskResult);
}
