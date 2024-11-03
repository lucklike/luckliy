package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 参数解析异常
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/3 22:38
 */
public class ParameterResolverException extends LuckyRuntimeException {
    public ParameterResolverException(String message) {
        super(message);
    }

    public ParameterResolverException(Throwable ex) {
        super(ex);
    }

    public ParameterResolverException(String message, Throwable ex) {
        super(message, ex);
    }

    public ParameterResolverException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ParameterResolverException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
