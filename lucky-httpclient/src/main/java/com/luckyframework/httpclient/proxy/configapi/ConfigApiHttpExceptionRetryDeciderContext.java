package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.retry.ExCheckModel;
import com.luckyframework.httpclient.proxy.retry.RetryDeciderContext;
import com.luckyframework.retry.TaskResult;

/**
 * 异常重试策略
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 17:14
 */
public class ConfigApiHttpExceptionRetryDeciderContext extends RetryDeciderContext<Response> {

    private Class<? extends Throwable>[] retryFor;
    private Class<? extends Throwable>[] exclude;
    private int[] exceptionStatus = {};
    private int[] normalStatus = {};
    private String retryExpression = "";
    private String retryFuncName = "";
    private ExCheckModel exCheckModel;


    public void setRetryFor(Class<? extends Throwable>[] retryFor) {
        this.retryFor = retryFor;
    }

    public void setExclude(Class<? extends Throwable>[] exclude) {
        this.exclude = exclude;
    }

    public void setExceptionStatus(int[] exceptionStatus) {
        this.exceptionStatus = exceptionStatus;
    }

    public void setNormalStatus(int[] normalStatus) {
        this.normalStatus = normalStatus;
    }

    public void setRetryExpression(String retryExpression) {
        this.retryExpression = retryExpression;
    }

    public void setRetryFuncName(String retryFuncName) {
        this.retryFuncName = retryFuncName;
    }

    public void setExCheckModel(ExCheckModel exCheckModel) {
        this.exCheckModel = exCheckModel;
    }

    @Override
    public boolean doNeedRetry(TaskResult<Response> taskResult) {
        boolean isRetryEx = exceptionCheck(taskResult, retryFor, exclude, exCheckModel);
        if (isRetryEx) {
            return true;
        }
        if (taskResult.hasException()) {
            return false;
        }
        return retryExpressionCheck(taskResult, retryExpression, retryFuncName) ||
                httpStatusCheck(taskResult, normalStatus, exceptionStatus);
    }
}
