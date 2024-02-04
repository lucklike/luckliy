package com.luckyframework.retry;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 重试等待异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/20 14:08
 */
public class RetryWaiteException extends LuckyRuntimeException {
    public RetryWaiteException(String message) {
        super(message);
    }

    public RetryWaiteException(Throwable ex) {
        super(ex);
    }

    public RetryWaiteException(String message, Throwable ex) {
        super(message, ex);
    }

    public RetryWaiteException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public RetryWaiteException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
