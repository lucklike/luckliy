package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

public class WrapperMethodInvokeException extends LuckyRuntimeException {
    public WrapperMethodInvokeException(String message) {
        super(message);
    }

    public WrapperMethodInvokeException(Throwable ex) {
        super(ex);
    }

    public WrapperMethodInvokeException(String message, Throwable ex) {
        super(message, ex);
    }

    public WrapperMethodInvokeException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public WrapperMethodInvokeException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
