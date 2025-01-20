package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 异步执行器创建异常
 */
public class AsyncExecutorCreateException extends LuckyRuntimeException {
    public AsyncExecutorCreateException(String message) {
        super(message);
    }

    public AsyncExecutorCreateException(Throwable ex) {
        super(ex);
    }

    public AsyncExecutorCreateException(String message, Throwable ex) {
        super(message, ex);
    }

    public AsyncExecutorCreateException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public AsyncExecutorCreateException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
