package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.exception.LuckyRuntimeException;

public class ExceptionHandleCreateException extends LuckyRuntimeException {
    public ExceptionHandleCreateException(String message) {
        super(message);
    }

    public ExceptionHandleCreateException(Throwable ex) {
        super(ex);
    }

    public ExceptionHandleCreateException(String message, Throwable ex) {
        super(message, ex);
    }

    public ExceptionHandleCreateException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ExceptionHandleCreateException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
