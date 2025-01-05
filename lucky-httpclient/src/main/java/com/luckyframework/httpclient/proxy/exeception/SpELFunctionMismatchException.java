package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * SpEL函数不匹配异常
 */
public class SpELFunctionMismatchException extends LuckyRuntimeException {
    public SpELFunctionMismatchException(String message) {
        super(message);
    }

    public SpELFunctionMismatchException(Throwable ex) {
        super(ex);
    }

    public SpELFunctionMismatchException(String message, Throwable ex) {
        super(message, ex);
    }

    public SpELFunctionMismatchException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public SpELFunctionMismatchException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
