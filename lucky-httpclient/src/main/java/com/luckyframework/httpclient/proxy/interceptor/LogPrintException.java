package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 日志打印异常
 */
public class LogPrintException extends LuckyRuntimeException {
    public LogPrintException(String message) {
        super(message);
    }

    public LogPrintException(Throwable ex) {
        super(ex);
    }

    public LogPrintException(String message, Throwable ex) {
        super(message, ex);
    }

    public LogPrintException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public LogPrintException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
