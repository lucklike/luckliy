package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.ExceptionUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.SpELUtils;
import com.luckyframework.httpclient.proxy.annotations.Retryable;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.retry.TaskResult;

import java.util.Arrays;

/**
 * 异常重试策略
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 17:14
 */
@SuppressWarnings("unchecked")
public class HttpExceptionRetryDeciderContent extends RetryDeciderContent<Object> {


    @Override
    public boolean needRetry(TaskResult<Object> taskResult) {
        Throwable throwable = taskResult.getThrowable();
        Object response = taskResult.getResult();
        return exceptionCheck(throwable) || httpCodeCheck(response) || retryExpressionCheck(throwable, response);
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
        // 如果是HttpExecutorException则需要转化为更为本质的异常实例
        throwable = ExceptionUtils.getCauseThrowable(throwable, HttpExecutorException.class);

        // 指定排除的异常
        Class<? extends Throwable>[] excludes = (Class<? extends Throwable>[]) getAnnotationAttribute(Retryable.ATTRIBUTE_EXCLUDE);
        if (ExceptionUtils.contained(Arrays.asList(excludes), throwable.getClass())) {
            return false;
        }

        // 指定需要重试的异常
        Class<? extends Throwable>[] retryFor = (Class<? extends Throwable>[]) getAnnotationAttribute(Retryable.ATTRIBUTE_RETRY_FOR);
        return ExceptionUtils.isAssignableFrom(Arrays.asList(retryFor), throwable.getClass());
    }

    /**
     * HTTP响应码校验，检查当前响应的code码是否满足重试条件
     *
     * @param response 响应对象
     * @return 当前响应的code码是否满足重试条件
     */
    private boolean httpCodeCheck(Object response) {
        Integer code = null;
        if (response instanceof Response) {
            code = ((Response) response).getStatus();
        } else if (response instanceof ResponseMetaData) {
            code = ((ResponseMetaData) response).getStatus();
        }
        if (code == null) {
            return true;
        }

        // 获取异常状态码
        Integer[] exceptionStatus = getAnnotationAttribute(Retryable.ATTRIBUTE_EXCEPTION_STATUS, Integer[].class);
        if (ContainerUtils.inArrays(exceptionStatus, code)) {
            return true;
        }

        // 获取正常情况的状态码
        Integer[] normalStatus = getAnnotationAttribute(Retryable.ATTRIBUTE_NORMAL_STATUS, Integer[].class);
        return ContainerUtils.isNotEmptyArray(normalStatus) && ContainerUtils.notInArrays(normalStatus, code);
    }

    /**
     * 重试表达式检验，检验当前情况是否满足重试表达式
     *
     * @param throwable 当前发生的异常实例
     * @param response  当前响应对象实例
     * @return 当前情况是否满足重试表达式
     */
    private boolean retryExpressionCheck(Throwable throwable, Object response) {
        String retryExpression = getAnnotationAttribute(Retryable.ATTRIBUTE_RETRY_EXPRESSION, String.class);
        if (!StringUtils.hasText(retryExpression)) {
            return false;
        }

        MethodContext methodContext = getContext();
        SpELUtils.ExtraSpELArgs extraSpELArgs
                = SpELUtils.createSpELArgs()
                .setExpression(retryExpression)
                .setReturnType(boolean.class)
                .extractSpELEnv()
                .extractException(throwable)
                .extractMethodContext(methodContext)
                .extractAnnotationContext(this);
        if (response instanceof Response) {
            Response resp = (Response) response;
            extraSpELArgs.extractResponse(resp)
                    .extractRequest(resp.getRequest());
        } else if (response instanceof VoidResponse) {
            VoidResponse voidResp = (VoidResponse) response;
            extraSpELArgs.extractVoidResponse(voidResp)
                    .extractRequest(voidResp.getRequest());
        }
        return SpELUtils.parseExpression(
                SpELUtils.getContextParamWrapper(methodContext, extraSpELArgs)
        );
    }
}
