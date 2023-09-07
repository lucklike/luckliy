package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.HttpExceptionHandle;

/**
 * 默认的Http异常处理器，打印Request以及异常信息
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/29 23:04
 */
public class DefaultHttpExceptionHandle implements HttpExceptionHandle {

    @Override
    public void exceptionHandler(Request request, Exception exception) {
        if (exception instanceof HttpExecutorException) {
            throw (HttpExecutorException)exception;
        }
        String message = StringUtils.format("Response conversion failed after request execution. request is {}", request);
        throw new HttpExecutorException(message, exception);
    }
}
