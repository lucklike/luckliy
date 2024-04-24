package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.httpclient.proxy.context.AnnotationContext;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.retry.CallableRetryTaskNamedAdapter;
import com.luckyframework.retry.RetryUtils;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

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
    private final Supplier<RunBeforeRetryContext> beforeRetryContentSupplier;
    private final Supplier<RetryDeciderContent> retryDeciderContentSupplier;
    private final Annotation retryAnnotation;

    public static final RetryActuator DONT_RETRY = new RetryActuator(false, "", 0, null, null, null);

    private RetryActuator(boolean needRetry,
                          String taskName,
                          int retryCount,
                          Supplier<RunBeforeRetryContext> beforeRetryContentSupplier,
                          Supplier<RetryDeciderContent> retryDeciderContentSupplier,
                          Annotation retryAnnotation
    ) {
        this.needRetry = needRetry;
        this.taskName = taskName;
        this.retryCount = retryCount;
        this.beforeRetryContentSupplier = beforeRetryContentSupplier;
        this.retryDeciderContentSupplier = retryDeciderContentSupplier;
        this.retryAnnotation = retryAnnotation;
    }

    public RetryActuator(String taskName,
                         int retryCount,
                         Supplier<RunBeforeRetryContext> beforeRetryContentSupplier,
                         Supplier<RetryDeciderContent> retryDeciderContentSupplier,
                         Annotation retryAnnotation
    ) {
        this(true, taskName, retryCount, beforeRetryContentSupplier, retryDeciderContentSupplier, retryAnnotation);
    }

    public int getRetryCount() {
        return retryCount;
    }

    public RunBeforeRetryContext<?> getBeforeRetryContent() {
        return beforeRetryContentSupplier.get();
    }

    public RetryDeciderContent<?> getRetryDeciderContent() {
        return retryDeciderContentSupplier.get();
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

    public Object retryExecute(Callable<?> task, MethodContext methodContext) throws Exception {
        if (!isNeedRetry()) {
            return task.call();
        }

        RunBeforeRetryContext<?> beforeRetryContent = getBeforeRetryContent();
        RetryDeciderContent<?> retryDeciderContent = getRetryDeciderContent();
        beforeRetryContent.setContext(methodContext);
        beforeRetryContent.setAnnotation(retryAnnotation);
        retryDeciderContent.setContext(methodContext);
        retryDeciderContent.setAnnotation(retryAnnotation);
        beforeRetryContent.setContextVar();
        retryDeciderContent.setContextVar();
        return RetryUtils.callReturn(createNamedCallabe(retryDeciderContent, task), this.retryCount, beforeRetryContent, retryDeciderContent);
    }

    private CallableRetryTaskNamedAdapter createNamedCallabe(AnnotationContext annotationContext, Callable<?> task) {
        String name = annotationContext.parseExpression(this.taskName, String.class);
        return CallableRetryTaskNamedAdapter.create(name, task);
    }

}
