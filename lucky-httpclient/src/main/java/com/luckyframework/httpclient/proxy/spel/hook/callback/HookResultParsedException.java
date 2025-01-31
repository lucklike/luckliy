package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.exception.LuckyRuntimeException;

public class HookResultParsedException extends LuckyRuntimeException {


    public HookResultParsedException(String message) {
        super(message);
    }

    public HookResultParsedException(Throwable ex) {
        super(ex);
    }

    public HookResultParsedException(String message, Throwable ex) {
        super(message, ex);
    }

    public HookResultParsedException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public HookResultParsedException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
