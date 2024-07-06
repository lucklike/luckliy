package com.luckyframework.httpclient.proxy.unpack;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/11/24 13:50
 */
public class ContextValueUnpackException extends LuckyRuntimeException {
    public ContextValueUnpackException(String message) {
        super(message);
    }

    public ContextValueUnpackException(Throwable ex) {
        super(ex);
    }

    public ContextValueUnpackException(String message, Throwable ex) {
        super(message, ex);
    }

    public ContextValueUnpackException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ContextValueUnpackException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
