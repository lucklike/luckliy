package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.retry.CallableRetryTaskNamedAdapter;
import com.luckyframework.retry.RetryUtils;
import com.luckyframework.retry.RunBeforeRetry;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;

/**
 * 重试执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/3 04:10
 */
@SuppressWarnings("all")
public class RetryActuator {
    private final boolean needRetry;
    private final String taskName;
    private final int retryCount;
    private final RunBeforeRetryContext beforeRetryContent;
    private final RetryDeciderContent retryDeciderContent;
    private final Annotation retryAnnotation;

    public static final RetryActuator DONT_RETRY = new RetryActuator(false, "", 0, null, null, null);

    private RetryActuator(boolean needRetry, String taskName, int retryCount, RunBeforeRetryContext beforeRetryContent, RetryDeciderContent retryDeciderContent, Annotation retryAnnotation) {
        this.needRetry = needRetry;
        this.taskName = taskName;
        this.retryCount = retryCount;
        this.beforeRetryContent = beforeRetryContent;
        this.retryDeciderContent = retryDeciderContent;
        this.retryAnnotation = retryAnnotation;
        initRetryAnnotation(this.retryAnnotation);
    }

    public RetryActuator(String taskName, int retryCount, RunBeforeRetryContext beforeRetryContent, RetryDeciderContent retryDeciderContent, Annotation retryAnnotation) {
        this(true, taskName, retryCount, beforeRetryContent, retryDeciderContent, retryAnnotation);
    }

    public int getRetryCount() {
        return retryCount;
    }

    public RunBeforeRetry<?> getBeforeRetryContent() {
        return beforeRetryContent;
    }

    public RetryDeciderContent<?> getRetryDeciderContent() {
        return retryDeciderContent;
    }

    public boolean isNeedRetry() {
        return needRetry;
    }

    public Annotation getRetryAnnotation() {
        return retryAnnotation;
    }

    private void initMethodContent(MethodContext methodContext) {
        if (this.beforeRetryContent != null) {
            this.beforeRetryContent.setContext(methodContext);
        }
        if (this.retryDeciderContent != null) {
            this.retryDeciderContent.setContext(methodContext);
        }
    }

    private void initRetryAnnotation(Annotation retryAnnotation) {
        if (this.beforeRetryContent != null) {
            this.beforeRetryContent.setAnnotation(retryAnnotation);
        }
        if (this.retryDeciderContent != null) {
            this.retryDeciderContent.setAnnotation(retryAnnotation);
        }
    }

    public String getTaskName() {
        return taskName;
    }

    public Object retryExecute(Callable<?> task, MethodContext methodContext) throws Exception {
        initMethodContent(methodContext);
        return isNeedRetry()
                ? RetryUtils.callReturn(createNamedCallabe(methodContext, task), this.retryCount, this.beforeRetryContent, this.retryDeciderContent)
                : task.call();
    }

    private CallableRetryTaskNamedAdapter createNamedCallabe(MethodContext methodContext, Callable<?> task) {
        String name = beforeRetryContent.parseExpression(this.taskName, String.class);
        return CallableRetryTaskNamedAdapter.create(name, task);
    }

}
