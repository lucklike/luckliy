package com.luckyframework.httpclient.core.exception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 参数转换异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/3 22:33
 */
public class ParameterConvertException extends LuckyRuntimeException {
    public ParameterConvertException(String message) {
        super(message);
    }

    public ParameterConvertException(Throwable ex) {
        super(ex);
    }

    public ParameterConvertException(String message, Throwable ex) {
        super(message, ex);
    }

    public ParameterConvertException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ParameterConvertException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
