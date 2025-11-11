package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

public class HttpExecutorNotFountException extends LuckyRuntimeException {
    public HttpExecutorNotFountException(String message) {
        super(message);
    }

    public HttpExecutorNotFountException(Throwable ex) {
        super(ex);
    }

    public HttpExecutorNotFountException(String message, Throwable ex) {
        super(message, ex);
    }

    public HttpExecutorNotFountException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public HttpExecutorNotFountException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
