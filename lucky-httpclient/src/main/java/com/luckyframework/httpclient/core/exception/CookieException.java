package com.luckyframework.httpclient.core.exception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * Cookie异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/3/10 00:51
 */
public class CookieException extends LuckyRuntimeException {
    public CookieException(String message) {
        super(message);
    }

    public CookieException(Throwable ex) {
        super(ex);
    }

    public CookieException(String message, Throwable ex) {
        super(message, ex);
    }

    public CookieException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public CookieException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
