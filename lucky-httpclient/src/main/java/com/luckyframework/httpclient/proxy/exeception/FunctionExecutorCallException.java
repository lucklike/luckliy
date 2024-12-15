package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 函数执行器调用异常
 */
public class FunctionExecutorCallException extends LuckyRuntimeException {

    public FunctionExecutorCallException(String message) {
        super(message);
    }

    public FunctionExecutorCallException(Throwable ex) {
        super(ex);
    }

    public FunctionExecutorCallException(String message, Throwable ex) {
        super(message, ex);
    }

    public FunctionExecutorCallException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public FunctionExecutorCallException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
