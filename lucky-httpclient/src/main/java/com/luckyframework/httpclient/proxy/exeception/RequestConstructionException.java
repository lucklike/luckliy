package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 请求构造异常
 */
public class RequestConstructionException extends LuckyRuntimeException {
    public RequestConstructionException(String message) {
        super(message);
    }

    public RequestConstructionException(Throwable ex) {
        super(ex);
    }

    public RequestConstructionException(String message, Throwable ex) {
        super(message, ex);
    }

    public RequestConstructionException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public RequestConstructionException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
