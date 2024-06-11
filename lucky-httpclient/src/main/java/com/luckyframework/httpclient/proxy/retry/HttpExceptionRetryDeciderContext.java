package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.ExceptionUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.ResponseMetaData;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.annotations.Retryable;
import com.luckyframework.retry.TaskResult;

import java.util.Arrays;

import static com.luckyframework.httpclient.proxy.ParameterNameConstant.*;
import static com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager.getResponseBody;

/**
 * 异常重试策略
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 17:14
 */
public class HttpExceptionRetryDeciderContext extends RetryDeciderContext<Object> {


    @Override
    public boolean needRetry(TaskResult<Object> taskResult) {
        Throwable throwable = taskResult.getThrowable();
        Object response = taskResult.getResult();
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
        // 如果是HttpExecutorException则需要转化为更为本质的异常实例
        throwable = ExceptionUtils.getCauseThrowable(throwable, HttpExecutorException.class);

        Retryable retryableAnn = toAnnotation(Retryable.class);

        // 指定排除的异常
        if (ExceptionUtils.contained(Arrays.asList(retryableAnn.exclude()), throwable.getClass())) {
            return false;
        }

        // 指定需要重试的异常
        return ExceptionUtils.isAssignableFrom(Arrays.asList(retryableAnn.retryFor()), throwable.getClass());
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

        Retryable retryableAnn = toAnnotation(Retryable.class);

        // 获取异常状态码
        Integer[] exceptionStatus = ConversionUtils.conversion(retryableAnn.exceptionStatus(), Integer[].class);
        if (ContainerUtils.inArrays(exceptionStatus, code)) {
            return true;
        }

        // 获取正常情况的状态码
        Integer[] normalStatus = ConversionUtils.conversion(retryableAnn.normalStatus(), Integer[].class);
        return ContainerUtils.isNotEmptyArray(normalStatus) && ContainerUtils.notInArrays(normalStatus, code);
    }

    /**
     * 重试表达式检验，检验当前情况是否满足重试表达式
     *
     * @param response  响应对象
     * @param throwable 当前发生的异常实例
     * @return 当前情况是否满足重试表达式
     */
    private boolean retryExpressionCheck(Object response, Throwable throwable) {
        String retryExpression = toAnnotation(Retryable.class).retryExpression();
        if (!StringUtils.hasText(retryExpression)) {
            return false;
        }
        return parseExpression(retryExpression, boolean.class, mpw -> {
            if (throwable != null) {
                mpw.addRootVariable(THROWABLE, throwable);
            }
            if (response instanceof Response) {
                Response resp = (Response) response;
                mpw.addRootVariable(RESPONSE, resp);
                mpw.addRootVariable(RESPONSE_STATUS, resp.getStatus());
                mpw.addRootVariable(CONTENT_LENGTH, resp.getContentLength());
                mpw.addRootVariable(CONTENT_TYPE, resp.getContentType());
                mpw.addRootVariable(RESPONSE_BODY, getResponseBody(resp, getConvertMetaType()));
                mpw.addRootVariable(RESPONSE_HEADER, resp.getSimpleHeaders());
                mpw.addRootVariable(RESPONSE_COOKIE, resp.getSimpleCookies());
            } else if (response instanceof ResponseMetaData) {
                VoidResponse voidResp = new VoidResponse((ResponseMetaData) response);
                mpw.addRootVariable(VOID_RESPONSE, voidResp);
                mpw.addRootVariable(RESPONSE_STATUS, voidResp.getStatus());
                mpw.addRootVariable(CONTENT_LENGTH, voidResp.getContentLength());
                mpw.addRootVariable(CONTENT_TYPE, voidResp.getContentType());
                mpw.addRootVariable(RESPONSE_HEADER, voidResp.getSimpleHeaders());
                mpw.addRootVariable(RESPONSE_COOKIE, voidResp.getSimpleCookies());
            }
        });
    }


}