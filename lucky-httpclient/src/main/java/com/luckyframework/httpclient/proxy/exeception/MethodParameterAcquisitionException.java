package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 方法参数获取异常
 */
public class MethodParameterAcquisitionException extends LuckyRuntimeException {
    public MethodParameterAcquisitionException(String message) {
        super(message);
    }

    public MethodParameterAcquisitionException(Throwable ex) {
        super(ex);
    }

    public MethodParameterAcquisitionException(String message, Throwable ex) {
        super(message, ex);
    }

    public MethodParameterAcquisitionException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public MethodParameterAcquisitionException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
