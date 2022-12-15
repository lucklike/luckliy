package com.luckyframework.async;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/13 12:33
 */
public class EnhanceFutureTaskNotFountException extends LuckyRuntimeException {

    public EnhanceFutureTaskNotFountException(String message) {
        super(message);
    }

    public EnhanceFutureTaskNotFountException(Throwable ex) {
        super(ex);
    }

    public EnhanceFutureTaskNotFountException(String message, Throwable ex) {
        super(message, ex);
    }

    public EnhanceFutureTaskNotFountException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public EnhanceFutureTaskNotFountException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }

}
