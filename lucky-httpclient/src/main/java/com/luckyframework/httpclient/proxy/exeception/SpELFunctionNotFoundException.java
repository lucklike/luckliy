package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * SpEL函数未找到
 *
 */
public class SpELFunctionNotFoundException extends LuckyRuntimeException {
    public SpELFunctionNotFoundException(String message) {
        super(message);
    }

    public SpELFunctionNotFoundException(Throwable ex) {
        super(ex);
    }

    public SpELFunctionNotFoundException(String message, Throwable ex) {
        super(message, ex);
    }

    public SpELFunctionNotFoundException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public SpELFunctionNotFoundException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
