package com.luckyframework.httpclient.core.exception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * Http请求执行异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/16 05:09
 */
public class HttpExecutorException extends LuckyRuntimeException {

    public HttpExecutorException(String message) {
        super(message);
    }

    public HttpExecutorException(Throwable ex) {
        super(ex);
    }

    public HttpExecutorException(String message, Throwable ex) {
        super(message, ex);
    }

    public HttpExecutorException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public HttpExecutorException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
