package com.luckyframework.httpclient.proxy.fuse;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 熔断异常
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/4 02:39
 */
public class FuseException extends LuckyRuntimeException {
    public FuseException(String message) {
        super(message);
    }

    public FuseException(Throwable ex) {
        super(ex);
    }

    public FuseException(String message, Throwable ex) {
        super(message, ex);
    }

    public FuseException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public FuseException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
