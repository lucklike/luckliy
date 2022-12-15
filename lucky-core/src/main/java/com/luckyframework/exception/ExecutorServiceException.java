package com.luckyframework.exception;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/27 16:37
 */
public class ExecutorServiceException extends LuckyRuntimeException {
    public ExecutorServiceException(String message) {
        super(message);
    }

    public ExecutorServiceException(Throwable ex) {
        super(ex);
    }

    public ExecutorServiceException(String message, Throwable ex) {
        super(message, ex);
    }

    public ExecutorServiceException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ExecutorServiceException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
