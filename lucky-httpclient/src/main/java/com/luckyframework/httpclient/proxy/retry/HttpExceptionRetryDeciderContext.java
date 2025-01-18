package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.Retryable;
import com.luckyframework.retry.TaskResult;

/**
 * 异常重试策略
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 17:14
 */
public class HttpExceptionRetryDeciderContext extends RetryDeciderContext<Response> {


    @Override
    public boolean doNeedRetry(TaskResult<Response> taskResult) {
        Retryable retryAnn = toAnnotation(Retryable.class);
        return retryExpressionCheck(taskResult, retryAnn.condition(), retryAnn.conditionFunc())
                || exceptionCheck(taskResult, retryAnn.retryFor(), retryAnn.exclude())
                || httpStatusCheck(taskResult, retryAnn.normalStatus(), retryAnn.exceptionStatus());
    }

}
