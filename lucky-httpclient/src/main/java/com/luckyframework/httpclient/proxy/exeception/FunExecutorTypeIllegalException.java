package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 函数执行器类型非法异常
 */
public class FunExecutorTypeIllegalException extends LuckyRuntimeException {

    public FunExecutorTypeIllegalException(String message) {
        super(message);
    }

    public FunExecutorTypeIllegalException(Throwable ex) {
        super(ex);
    }

    public FunExecutorTypeIllegalException(String message, Throwable ex) {
        super(message, ex);
    }

    public FunExecutorTypeIllegalException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public FunExecutorTypeIllegalException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
