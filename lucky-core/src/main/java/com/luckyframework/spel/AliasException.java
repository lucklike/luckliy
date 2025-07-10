package com.luckyframework.spel;

import com.luckyframework.exception.LuckyRuntimeException;

public class AliasException extends LuckyRuntimeException {
    public AliasException(String message) {
        super(message);
    }

    public AliasException(Throwable ex) {
        super(ex);
    }

    public AliasException(String message, Throwable ex) {
        super(message, ex);
    }

    public AliasException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public AliasException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
