package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 异步执行器未找到异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/6/26 02:16
 */
public class AsyncExecutorNotFountException extends LuckyRuntimeException {
    public AsyncExecutorNotFountException(String message) {
        super(message);
    }

    public AsyncExecutorNotFountException(Throwable ex) {
        super(ex);
    }

    public AsyncExecutorNotFountException(String message, Throwable ex) {
        super(message, ex);
    }

    public AsyncExecutorNotFountException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public AsyncExecutorNotFountException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
