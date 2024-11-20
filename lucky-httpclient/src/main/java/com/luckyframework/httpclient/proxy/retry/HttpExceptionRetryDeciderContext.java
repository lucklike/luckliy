package com.luckyframework.httpclient.proxy.retry;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.common.ExceptionUtils;
import com.luckyframework.common.StringUtils;
import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.annotations.Retryable;
import com.luckyframework.retry.TaskResult;
import com.luckyframework.spel.LazyValue;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.luckyframework.httpclient.proxy.spel.DefaultSpELVarManager.getResponseBody;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CONTENT_LENGTH_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_CONTENT_TYPE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_BYTE_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_COOKIE_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_HEADER_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_STATUS_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_STREAM_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_RESPONSE_STRING_BODY_$;
import static com.luckyframework.httpclient.proxy.spel.InternalParamName.$_THROWABLE_$;

/**
 * 异常重试策略
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/23 17:14
 */
public class HttpExceptionRetryDeciderContext extends RetryDeciderContext<Response> {


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
    private boolean httpCodeCheck(Response response) {
        Integer code = response.getStatus();
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
    private boolean retryExpressionCheck(Response response, Throwable throwable) {
        String retryExpression = toAnnotation(Retryable.class).retryExpression();
        if (!StringUtils.hasText(retryExpression)) {
            return false;
        }
        return parseExpression(retryExpression, boolean.class, mpw -> {

            Map<String, Object> extendMap = new ConcurrentHashMap<>(11);
            if (throwable != null) {
                extendMap.put($_THROWABLE_$, LazyValue.of(throwable));
            }
            extendMap.put($_RESPONSE_$, LazyValue.of(response));
            extendMap.put($_RESPONSE_STATUS_$, LazyValue.of(response::getStatus));
            extendMap.put($_CONTENT_LENGTH_$, LazyValue.of(response::getContentLength));
            extendMap.put($_CONTENT_TYPE_$, LazyValue.of(response::getContentType));
            extendMap.put($_RESPONSE_HEADER_$, LazyValue.of(response::getSimpleHeaders));
            extendMap.put($_RESPONSE_COOKIE_$, LazyValue.of(response::getSimpleCookies));
            extendMap.put($_RESPONSE_STREAM_BODY_$, LazyValue.rtc(response::getInputStream));
            extendMap.put($_RESPONSE_STRING_BODY_$, LazyValue.of(response::getStringResult));
            extendMap.put($_RESPONSE_BYTE_BODY_$, LazyValue.of(response::getResult));
            extendMap.put($_RESPONSE_BODY_$, LazyValue.of(() -> getResponseBody(response, getConvertMetaType())));
            mpw.getRootObject().addFirst(extendMap);
        });
    }


}
