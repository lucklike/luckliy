package com.luckyframework.httpclient.proxy.url;

import com.luckyframework.exception.LuckyRuntimeException;

public class UrlGetException extends LuckyRuntimeException {


    public UrlGetException(String message) {
        super(message);
    }

    public UrlGetException(Throwable ex) {
        super(ex);
    }

    public UrlGetException(String message, Throwable ex) {
        super(message, ex);
    }

    public UrlGetException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public UrlGetException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
