package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.retry.CallableRetryTaskNamedAdapter;
import com.luckyframework.retry.RetryUtils;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;
import java.util.function.Function;

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
    private final Function<MethodContext, RunBeforeRetryContext> beforeRetryContentFunction;
    private final Function<MethodContext, RetryDeciderContext> retryDeciderContentFunction;
    private final Annotation retryAnnotation;

    public static final RetryActuator DONT_RETRY = new RetryActuator(false, "", 0, null, null, null);

    private RetryActuator(boolean needRetry,
                          String taskName,
                          int retryCount,
                          Function<MethodContext, RunBeforeRetryContext> beforeRetryContentFunction,
                          Function<MethodContext, RetryDeciderContext> retryDeciderContentFunction,
                          Annotation retryAnnotation
    ) {
        this.needRetry = needRetry;
        this.taskName = taskName;
        this.retryCount = retryCount;
        this.beforeRetryContentFunction = beforeRetryContentFunction;
        this.retryDeciderContentFunction = retryDeciderContentFunction;
        this.retryAnnotation = retryAnnotation;
    }

    public RetryActuator(String taskName,
                         int retryCount,
                         Function<MethodContext, RunBeforeRetryContext> beforeRetryContentFunction,
                         Function<MethodContext, RetryDeciderContext> retryDeciderContentFunction,
                         Annotation retryAnnotation
    ) {
        this(true, taskName, retryCount, beforeRetryContentFunction, retryDeciderContentFunction, retryAnnotation);
    }

    public int getRetryCount() {
        return retryCount;
    }

    public RunBeforeRetryContext<?> getBeforeRetryContext(MethodContext methodContext) {
        RunBeforeRetryContext beforeRetryContext = beforeRetryContentFunction.apply(methodContext);
        beforeRetryContext.setContext(methodContext);
        beforeRetryContext.setAnnotation(retryAnnotation);
        beforeRetryContext.setContextVar();
        return beforeRetryContext;
    }

    public RetryDeciderContext<Response> getRetryDeciderContext(MethodContext methodContext) {
        RetryDeciderContext<Response> retryDeciderContext = retryDeciderContentFunction.apply(methodContext);
        retryDeciderContext.setContext(methodContext);
        retryDeciderContext.setAnnotation(retryAnnotation);
        retryDeciderContext.setContextVar();
        return retryDeciderContext;
    }

    public boolean isNeedRetry() {
        return needRetry;
    }

    public Annotation getRetryAnnotation() {
        return retryAnnotation;
    }

    public String getTaskName() {
        return taskName;
    }

    public Response retryExecute(Callable<Response> task, MethodContext methodContext) throws Exception {
        if (!isNeedRetry()) {
            return task.call();
        }

        RunBeforeRetryContext<?> beforeRetryContext = getBeforeRetryContext(methodContext);
        RetryDeciderContext<Response> retryDeciderContext = getRetryDeciderContext(methodContext);
        return RetryUtils.callReturn(createNamedCallabe(retryDeciderContext, task), this.retryCount, beforeRetryContext, retryDeciderContext);
    }

    private CallableRetryTaskNamedAdapter<Response> createNamedCallabe(AnnotationContext annotationContext, Callable<Response> task) {
        String name = annotationContext.parseExpression(this.taskName, String.class);
        return CallableRetryTaskNamedAdapter.<Response>create(name, task);
    }

}
