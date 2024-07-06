package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 重定向异常
 */
public class RedirectException extends LuckyRuntimeException {
    public RedirectException(String message) {
        super(message);
    }

    public RedirectException(Throwable ex) {
        super(ex);
    }

    public RedirectException(String message, Throwable ex) {
        super(message, ex);
    }

    public RedirectException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public RedirectException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
