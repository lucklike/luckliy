package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 添加SpEL变量
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/22 02:15
 */
public class AddSpELVariableException extends LuckyRuntimeException {
    public AddSpELVariableException(String message) {
        super(message);
    }

    public AddSpELVariableException(Throwable ex) {
        super(ex);
    }

    public AddSpELVariableException(String message, Throwable ex) {
        super(message, ex);
    }

    public AddSpELVariableException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public AddSpELVariableException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
