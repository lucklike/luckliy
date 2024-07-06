package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 无需返回结果的HTTP异常处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/27 01:13
 */
public interface NotReturnExceptionHandle extends HttpExceptionHandle {

    @Override
    default Object exceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        doExceptionHandler(methodContext, request, throwable);
        return null;
    }

    void doExceptionHandler(MethodContext methodContext, Request request, Throwable throwable);
}
