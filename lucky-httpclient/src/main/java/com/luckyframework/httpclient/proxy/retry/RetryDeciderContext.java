package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.ExceptionUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.spel.AddTempRespAndThrowVarSetter;
import com.luckyframework.retry.RetryDecider;
import com.luckyframework.retry.TaskResult;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 重试决策抽象类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 15:13
 */
public abstract class RetryDeciderContext<T> extends RetryContext implements RetryDecider<T> {

    /**
     * 重试任务结果
     */
    public static final String RETRY_TASK_RESULT_KEY = "_retryTaskResult_";

    /**
     * 约定决定是否重试的方法后缀
     */
    public static final String AGREED_RETRY_METHOD_SUFFIX = "NeedRetry";

    @Override
    public boolean needRetry(TaskResult<T> taskResult) {
        getContextVar().addRootVariable(RETRY_TASK_RESULT_KEY, taskResult);
        return doNeedRetry(taskResult);
    }

    /**
     * 重试表达式检验，检验当前情况是否满足重试表达式
     *
     * @param taskResult      当前任务执行结果
     * @param retryExpression 重试表达式
     * @return 当前情况是否满足重试表达式
     */
    protected boolean retryExpressionCheck(TaskResult<Response> taskResult, String retryExpression) {
        MethodContext context = this.getContext();

        // 存在重试表达式时使用表达式
        if (StringUtils.hasText(retryExpression)) {
            return parseExpression(retryExpression, boolean.class, new AddTempRespAndThrowVarSetter(taskResult.getResult(), context, taskResult.getThrowable()));
        }

        // 不存在重试表达式时，查找是否存在约定的重试方法xxxNeedRetry
        Method agreedOnNeedRetryMethod = getAgreedOnNeedRetryMethod(context);
        if (agreedOnNeedRetryMethod != null) {
            return (boolean) context.invokeMethod(null, agreedOnNeedRetryMethod);
        }
        return false;
    }

    /**
     * 获取约定的NeedRetry方法
     *
     * @param context 方法上下文
     * @return 约定的Mock方法
     */
    @Nullable
    protected Method getAgreedOnNeedRetryMethod(MethodContext context) {
        String agreedOnNeedRetryExpression = context.getCurrentAnnotatedElement().getName() + AGREED_RETRY_METHOD_SUFFIX;
        Method agreedOnMockMethod = context.getVar(agreedOnNeedRetryExpression, Method.class);
        if (agreedOnMockMethod != null && (boolean.class == agreedOnMockMethod.getReturnType() || Boolean.class == agreedOnMockMethod.getReturnType())) {
            return agreedOnMockMethod;
        }
        return null;
    }

    /**
     * 异常检查，检查当前异常是否满足重试条件
     *
     * @param taskResult 当前任务执行结果
     * @param retryFor   指定需要重试的异常
     * @param exclude    指定排除的异常，出现这类异常时不需要重试
     * @return 当前异常是否满足重试条件
     */
    protected boolean exceptionCheck(TaskResult<Response> taskResult, Class<? extends Throwable>[] retryFor, Class<? extends Throwable>[] exclude) {

        // 获取异常信息
        Throwable throwable = taskResult.getThrowable();

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
     * @param taskResult      当前任务执行结果
     * @param normalStatus    正常的状态码
     * @param exceptionStatus 异常的状态码
     * @return 当前响应的code码是否满足重试条件
     */
    protected boolean httpStatusCheck(TaskResult<Response> taskResult, int[] normalStatus, int[] exceptionStatus) {
        // 获取状态码信息
        Response response = taskResult.getResult();
        Integer status = response.getStatus();

        // 获取异常状态码
        Integer[] _exceptionStatus = ConversionUtils.conversion(exceptionStatus, Integer[].class);
        if (ContainerUtils.inArrays(_exceptionStatus, status)) {
            return true;
        }

        // 获取正常情况的状态码
        Integer[] _normalStatus = ConversionUtils.conversion(normalStatus, Integer[].class);
        return ContainerUtils.isNotEmptyArray(_normalStatus) && ContainerUtils.notInArrays(_normalStatus, status);
    }


    /**
     * 决定是否需要进行重试的方法，该方法由子类进行实现
     *
     * @param taskResult 当前任务结果
     * @return 评估当前结果是否需要重试
     */
    protected abstract boolean doNeedRetry(TaskResult<T> taskResult);
}
