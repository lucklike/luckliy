package com.luckyframework.httpclient.proxy.handle;

import com.luckyframework.exception.LuckyRuntimeException;

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
