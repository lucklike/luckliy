package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 支持使用SpEL表达式的
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/26 15:11
 */
public class SpELHttpExceptionHandle extends AbstractHttpExceptionHandle {

    @Override
    protected Object doExceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        ExceptionHandle exceptionHandleAnn = methodContext.getMergedAnnotationCheckParent(ExceptionHandle.class);
        String expression = exceptionHandleAnn.excHandleExp();
        if (!StringUtils.hasText(expression)) {
           return throwExceptionPrintLog(methodContext, throwable);
        }

        return methodContext.parseExpression(expression);
    }
}
