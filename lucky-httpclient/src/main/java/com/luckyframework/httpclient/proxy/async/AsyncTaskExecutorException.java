package com.luckyframework.httpclient.proxy.async;

import com.luckyframework.exception.LuckyRuntimeException;

public class AsyncTaskExecutorException extends LuckyRuntimeException {
    public AsyncTaskExecutorException(String message) {
        super(message);
    }

    public AsyncTaskExecutorException(Throwable ex) {
        super(ex);
    }

    public AsyncTaskExecutorException(String message, Throwable ex) {
        super(message, ex);
    }

    public AsyncTaskExecutorException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public AsyncTaskExecutorException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
