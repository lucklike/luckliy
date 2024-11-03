package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 降级异常
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/4 01:26
 */
public class FallbackException extends LuckyRuntimeException {
    public FallbackException(String message) {
        super(message);
    }

    public FallbackException(Throwable ex) {
        super(ex);
    }

    public FallbackException(String message, Throwable ex) {
        super(message, ex);
    }

    public FallbackException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public FallbackException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
