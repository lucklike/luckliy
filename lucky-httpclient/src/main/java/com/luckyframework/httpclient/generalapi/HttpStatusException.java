package com.luckyframework.httpclient.generalapi;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * HTTP响应状态码异常
 */
public class HttpStatusException extends LuckyRuntimeException {
    public HttpStatusException(String message) {
        super(message);
    }

    public HttpStatusException(Throwable ex) {
        super(ex);
    }

    public HttpStatusException(String message, Throwable ex) {
        super(message, ex);
    }

    public HttpStatusException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public HttpStatusException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
