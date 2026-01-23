package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

public class ConvertMetaTypeGetException extends LuckyRuntimeException {
    public ConvertMetaTypeGetException(String message) {
        super(message);
    }

    public ConvertMetaTypeGetException(Throwable ex) {
        super(ex);
    }

    public ConvertMetaTypeGetException(String message, Throwable ex) {
        super(message, ex);
    }

    public ConvertMetaTypeGetException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ConvertMetaTypeGetException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
