package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.ExceptionUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.retry.RetryDeciderContext;
import com.luckyframework.httpclient.proxy.spel.AddTempRespAndThrowVarSetter;
import com.luckyframework.retry.TaskResult;

import java.util.Arrays;

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

    @Override
    public boolean needRetry(TaskResult<Response> taskResult) {
        Throwable throwable = taskResult.getThrowable();
        Response response = taskResult.getResult();
        return exceptionCheck(throwable) || httpCodeCheck(response) || retryExpressionCheck(response, throwable);
    }

    /**
     * 异常检查，检查当前异常是否满足重试条件
     *
     * @param throwable 当前发生的异常实例
     * @return 当前异常是否满足重试条件
     */
    private boolean exceptionCheck(Throwable throwable) {
        // 异常对象为null时说明没有出现异常
        if (throwable == null) {
            return false;
        }

        // 指定排除的异常
        if (ExceptionUtils.contained(Arrays.asList(exclude), throwable.getClass())) {
            return false;
        }

        // 指定需要重试的异常
        return ExceptionUtils.isAssignableFrom(Arrays.asList(retryFor), throwable.getClass());
    }

    /**
     * HTTP响应码校验，检查当前响应的code码是否满足重试条件
     *
     * @param response 响应对象
     * @return 当前响应的code码是否满足重试条件
     */
    private boolean httpCodeCheck(Response response) {
        Integer code = response.getStatus();

        // 获取异常状态码
        Integer[] exceptionStatus = ConversionUtils.conversion(this.exceptionStatus, Integer[].class);
        if (ContainerUtils.inArrays(exceptionStatus, code)) {
            return true;
        }

        // 获取正常情况的状态码
        Integer[] normalStatus = ConversionUtils.conversion(this.normalStatus, Integer[].class);
        return ContainerUtils.isNotEmptyArray(normalStatus) && ContainerUtils.notInArrays(normalStatus, code);
    }

    /**
     * 重试表达式检验，检验当前情况是否满足重试表达式
     *
     * @param response  响应对象
     * @param throwable 当前发生的异常实例
     * @return 当前情况是否满足重试表达式
     */
    private boolean retryExpressionCheck(Response response, Throwable throwable) {
        if (!StringUtils.hasText(retryExpression)) {
            return false;
        }
        return parseExpression(retryExpression, boolean.class, new AddTempRespAndThrowVarSetter(response, this.getContext(), throwable));
    }


}
