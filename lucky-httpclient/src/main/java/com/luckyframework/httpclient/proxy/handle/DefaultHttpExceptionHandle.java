package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;
import com.luckyframework.httpclient.proxy.convert.ActivelyThrownException;

/**
 * 默认的Http异常处理器，打印Request以及异常信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/29 23:04
 */
public class DefaultHttpExceptionHandle implements HttpExceptionHandle {

    @Override
    public Object exceptionHandler(MethodContext methodContext, Request request, Throwable throwable) throws Throwable {
        return exceptionHandler(methodContext, throwable);
    }

    public static Object exceptionHandler(MethodContext methodContext, Throwable throwable) throws Throwable {
        if (throwable instanceof ActivelyThrownException && throwable.getCause() != null) {
            throw throwable.getCause();
        }
        throw throwable;
    }
}
