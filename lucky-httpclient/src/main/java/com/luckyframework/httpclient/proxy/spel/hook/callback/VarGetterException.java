package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.exception.LuckyRuntimeException;

public class VarGetterException extends LuckyRuntimeException {
    public VarGetterException(String message) {
        super(message);
    }

    public VarGetterException(Throwable ex) {
        super(ex);
    }

    public VarGetterException(String message, Throwable ex) {
        super(message, ex);
    }

    public VarGetterException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public VarGetterException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
