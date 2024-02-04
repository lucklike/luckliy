package com.luckyframework.spel;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/1/21 03:04
 */
public class SpelExpressionExecuteException extends RuntimeException {

    public SpelExpressionExecuteException() {
    }

    public SpelExpressionExecuteException(String message) {
        super(message);
    }

    public SpelExpressionExecuteException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpelExpressionExecuteException(Throwable cause) {
        super(cause);
    }

    public SpelExpressionExecuteException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
