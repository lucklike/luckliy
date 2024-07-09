package com.luckyframework.httpclient.proxy.sse;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * SSE异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/10 02:46
 */
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
