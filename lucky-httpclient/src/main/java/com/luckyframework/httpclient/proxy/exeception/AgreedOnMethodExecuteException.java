package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 约定方法执行异常
 */
public class AgreedOnMethodExecuteException extends LuckyRuntimeException {
    public AgreedOnMethodExecuteException(String message) {
        super(message);
    }

    public AgreedOnMethodExecuteException(Throwable ex) {
        super(ex);
    }

    public AgreedOnMethodExecuteException(String message, Throwable ex) {
        super(message, ex);
    }

    public AgreedOnMethodExecuteException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public AgreedOnMethodExecuteException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
