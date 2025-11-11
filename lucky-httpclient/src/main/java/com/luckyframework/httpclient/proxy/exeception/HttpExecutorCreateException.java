package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 异步执行器创建异常
 */
public class HttpExecutorCreateException extends LuckyRuntimeException {
    public HttpExecutorCreateException(String message) {
        super(message);
    }

    public HttpExecutorCreateException(Throwable ex) {
        super(ex);
    }

    public HttpExecutorCreateException(String message, Throwable ex) {
        super(message, ex);
    }

    public HttpExecutorCreateException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public HttpExecutorCreateException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
