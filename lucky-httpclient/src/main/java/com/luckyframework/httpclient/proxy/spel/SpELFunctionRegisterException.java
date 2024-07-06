package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/14 03:51
 */
public class SpELFunctionRegisterException extends LuckyRuntimeException {
    public SpELFunctionRegisterException(String message) {
        super(message);
    }

    public SpELFunctionRegisterException(Throwable ex) {
        super(ex);
    }

    public SpELFunctionRegisterException(String message, Throwable ex) {
        super(message, ex);
    }

    public SpELFunctionRegisterException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public SpELFunctionRegisterException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
