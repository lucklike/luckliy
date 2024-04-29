package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 用户主动抛出的异常
 * @author fukang
 * @version 1.0.0
 * @date 2024/4/29 13:21
 */
public class ActivelyThrownException extends LuckyRuntimeException {
    public ActivelyThrownException(String message) {
        super(message);
    }

    public ActivelyThrownException(Throwable ex) {
        super(ex);
    }

    public ActivelyThrownException(String message, Throwable ex) {
        super(message, ex);
    }

    public ActivelyThrownException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ActivelyThrownException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
