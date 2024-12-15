package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 函数执行器类型非法异常
 */
public class FunctionExecutorTypeIllegalException extends LuckyRuntimeException {

    public FunctionExecutorTypeIllegalException(String message) {
        super(message);
    }

    public FunctionExecutorTypeIllegalException(Throwable ex) {
        super(ex);
    }

    public FunctionExecutorTypeIllegalException(String message, Throwable ex) {
        super(message, ex);
    }

    public FunctionExecutorTypeIllegalException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public FunctionExecutorTypeIllegalException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
