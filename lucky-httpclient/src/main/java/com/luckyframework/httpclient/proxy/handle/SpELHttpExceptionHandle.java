package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandle;
import com.luckyframework.httpclient.proxy.annotations.ExceptionHandleMeta;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;

/**
 * 支持使用SpEL表达式的
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/12/26 15:11
 */
public class SpELHttpExceptionHandle implements HttpExceptionHandle {

    private static final Logger log = LoggerFactory.getLogger(SpELHttpExceptionHandle.class);

    @Override
    public Object exceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        Annotation exHandAnn = methodContext.getMergedAnnotationCheckParent(ExceptionHandleMeta.class);
        String expression = methodContext.toAnnotation(exHandAnn, ExceptionHandle.class).excHandleExp();
        if (!StringUtils.hasText(expression)) {
            throw new HttpExecutorException(throwable).printException(log);
        }

        return methodContext.parseExpression(expression);
    }
}
