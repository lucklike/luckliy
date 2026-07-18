package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

public class SpELFunctionPrepareException extends LuckyRuntimeException {
    public SpELFunctionPrepareException(String message) {
        super(message);
    }

    public SpELFunctionPrepareException(Throwable ex) {
        super(ex);
    }

    public SpELFunctionPrepareException(String message, Throwable ex) {
        super(message, ex);
    }

    public SpELFunctionPrepareException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public SpELFunctionPrepareException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
