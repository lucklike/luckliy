package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 拦截器注册异常
 */
public class InterceptorRegisterException extends LuckyRuntimeException {

    public InterceptorRegisterException(String message) {
        super(message);
    }

    public InterceptorRegisterException(Throwable ex) {
        super(ex);
    }

    public InterceptorRegisterException(String message, Throwable ex) {
        super(message, ex);
    }

    public InterceptorRegisterException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public InterceptorRegisterException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
