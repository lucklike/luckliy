package com.luckyframework.httpclient.proxy.configapi.parse;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.retry.ExceptionModel;
import com.luckyframework.httpclient.proxy.retry.RetryDeciderContext;
import com.luckyframework.retry.TaskResult;

/**
 * 基于配置的重试决策者
 */
public class ConfigurationRetryDeciderContext  extends RetryDeciderContext<Response> {

    private final String condition;
    private final String conditionFunc;
    private final int[] normalStatus;
    private final int[] exceptionStatus;
    private final Class<? extends Throwable>[] exceptionClasses;
    private final Class<? extends Throwable>[] excludeClasses;
    private final ExceptionModel exCheckModel;
    private final ExceptionModel exExcludeModel;

    public ConfigurationRetryDeciderContext(RetryConfiguration retryConfig) {
        this.condition = retryConfig.getCondition();
        this.conditionFunc = retryConfig.getConditionFunc();
        this.normalStatus = retryConfig.getNormalStatus();
        this.exceptionStatus = retryConfig.getExceptionStatus();
        this.exceptionClasses = retryConfig.getExceptionClasses();
        this.excludeClasses = retryConfig.getExcludeClasses();
        this.exCheckModel = retryConfig.getExCheckModel();
        this.exExcludeModel = retryConfig.getExExcludeModel();
    }

    @Override
    protected boolean doNeedRetry(TaskResult<Response> taskResult) {
        boolean isRetryEx = exceptionCheck(taskResult, exceptionClasses, excludeClasses, exCheckModel, exExcludeModel);
        if (isRetryEx) {
            return true;
        }
        if (taskResult.hasException()) {
            return false;
        }
        return retryExpressionCheck(taskResult, condition, conditionFunc)
                || httpStatusCheck(taskResult, normalStatus, exceptionStatus);

    }
}
