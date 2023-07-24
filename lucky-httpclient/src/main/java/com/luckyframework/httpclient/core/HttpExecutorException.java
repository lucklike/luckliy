package com.luckyframework.httpclient.core;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 05:09
 */
public class HttpExecutorException extends RuntimeException {
    public HttpExecutorException() {
    }

    public HttpExecutorException(String message) {
        super(message);
    }

    public HttpExecutorException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpExecutorException(Throwable cause) {
        super(cause);
    }

    public HttpExecutorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
