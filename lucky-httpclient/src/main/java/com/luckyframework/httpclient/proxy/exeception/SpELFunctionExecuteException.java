package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * SpEL函数执行异常
 */
public class SpELFunctionExecuteException extends LuckyRuntimeException {
    public SpELFunctionExecuteException(String message) {
        super(message);
    }

    public SpELFunctionExecuteException(Throwable ex) {
        super(ex);
    }

    public SpELFunctionExecuteException(String message, Throwable ex) {
        super(message, ex);
    }

    public SpELFunctionExecuteException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public SpELFunctionExecuteException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
