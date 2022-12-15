package com.luckyframework.async;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/13 13:13
 */
public class FutureGetException extends LuckyRuntimeException {
    public FutureGetException(String message) {
        super(message);
    }

    public FutureGetException(Throwable ex) {
        super(ex);
    }

    public FutureGetException(String message, Throwable ex) {
        super(message, ex);
    }

    public FutureGetException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public FutureGetException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
