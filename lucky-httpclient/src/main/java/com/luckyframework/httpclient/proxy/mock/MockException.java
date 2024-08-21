package com.luckyframework.httpclient.proxy.mock;

import com.luckyframework.exception.LuckyRuntimeException;

public class MockException extends LuckyRuntimeException {
    public MockException(String message) {
        super(message);
    }

    public MockException(Throwable ex) {
        super(ex);
    }

    public MockException(String message, Throwable ex) {
        super(message, ex);
    }

    public MockException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public MockException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
