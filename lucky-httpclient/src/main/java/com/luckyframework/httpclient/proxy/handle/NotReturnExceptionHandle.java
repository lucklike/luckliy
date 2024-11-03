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
public abstract class NotReturnExceptionHandle extends AbstractHttpExceptionHandle {

    @Override
    protected Object doExceptionHandler(MethodContext methodContext, Request request, Throwable throwable) {
        handlerException(methodContext, request, throwable);
        return null;
    }

    protected abstract void handlerException(MethodContext methodContext, Request request, Throwable throwable);
}
