package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.exception.LuckyRuntimeException;

public class GenerateObjectException extends LuckyRuntimeException {


    public GenerateObjectException(String message) {
        super(message);
    }

    public GenerateObjectException(Throwable ex) {
        super(ex);
    }

    public GenerateObjectException(String message, Throwable ex) {
        super(message, ex);
    }

    public GenerateObjectException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public GenerateObjectException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
