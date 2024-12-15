package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.exception.LuckyRuntimeException;

public class PackGetterException extends LuckyRuntimeException {
    public PackGetterException(String message) {
        super(message);
    }

    public PackGetterException(Throwable ex) {
        super(ex);
    }

    public PackGetterException(String message, Throwable ex) {
        super(message, ex);
    }

    public PackGetterException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public PackGetterException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
