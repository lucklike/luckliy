package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

public class ConvertException extends LuckyRuntimeException {
    public ConvertException(String message) {
        super(message);
    }

    public ConvertException(Throwable ex) {
        super(ex);
    }

    public ConvertException(String message, Throwable ex) {
        super(message, ex);
    }

    public ConvertException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ConvertException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
