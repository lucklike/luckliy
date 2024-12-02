package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.exception.LuckyRuntimeException;

public class CallbackMethodExecuteException extends LuckyRuntimeException {
    public CallbackMethodExecuteException(String message) {
        super(message);
    }

    public CallbackMethodExecuteException(Throwable ex) {
        super(ex);
    }

    public CallbackMethodExecuteException(String message, Throwable ex) {
        super(message, ex);
    }

    public CallbackMethodExecuteException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public CallbackMethodExecuteException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
