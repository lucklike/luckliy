package com.luckyframework.httpclient.generalapi.token;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * Token 缓存异常
 */
public class TokenCacheException extends LuckyRuntimeException {

    public TokenCacheException(String message) {
        super(message);
    }

    public TokenCacheException(Throwable ex) {
        super(ex);
    }

    public TokenCacheException(String message, Throwable ex) {
        super(message, ex);
    }

    public TokenCacheException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public TokenCacheException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
