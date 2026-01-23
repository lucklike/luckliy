package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

public class FunctionReturnTypeNonMatchException extends LuckyRuntimeException {
    public FunctionReturnTypeNonMatchException(String message) {
        super(message);
    }

    public FunctionReturnTypeNonMatchException(Throwable ex) {
        super(ex);
    }

    public FunctionReturnTypeNonMatchException(String message, Throwable ex) {
        super(message, ex);
    }

    public FunctionReturnTypeNonMatchException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public FunctionReturnTypeNonMatchException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
