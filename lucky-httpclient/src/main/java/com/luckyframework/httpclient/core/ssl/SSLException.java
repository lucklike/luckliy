package com.luckyframework.httpclient.core.ssl;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/8/3 03:40
 */
public class SSLException extends LuckyRuntimeException {
    public SSLException(String message) {
        super(message);
    }

    public SSLException(Throwable ex) {
        super(ex);
    }

    public SSLException(String message, Throwable ex) {
        super(message, ex);
    }

    public SSLException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public SSLException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
