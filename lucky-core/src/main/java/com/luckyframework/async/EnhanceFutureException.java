package com.luckyframework.async;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/20 04:23
 */
public class EnhanceFutureException extends RuntimeException{
    public EnhanceFutureException() {
    }

    public EnhanceFutureException(String message) {
        super(message);
    }

    public EnhanceFutureException(String message, Throwable cause) {
        super(message, cause);
    }

    public EnhanceFutureException(Throwable cause) {
        super(cause);
    }

    public EnhanceFutureException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
