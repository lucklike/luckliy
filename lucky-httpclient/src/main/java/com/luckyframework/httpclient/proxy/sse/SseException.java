package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.exception.LuckyRuntimeException;

public class SseException extends LuckyRuntimeException {
    public SseException(String message) {
        super(message);
    }

    public SseException(Throwable ex) {
        super(ex);
    }

    public SseException(String message, Throwable ex) {
        super(message, ex);
    }

    public SseException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public SseException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
