package com.luckyframework.httpclient.proxy.stream;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * Stream异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/10 02:46
 */
public class StreamException extends LuckyRuntimeException {
    public StreamException(String message) {
        super(message);
    }

    public StreamException(Throwable ex) {
        super(ex);
    }

    public StreamException(String message, Throwable ex) {
        super(message, ex);
    }

    public StreamException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public StreamException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
