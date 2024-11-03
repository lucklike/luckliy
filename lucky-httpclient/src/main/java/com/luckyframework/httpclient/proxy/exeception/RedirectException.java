package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 重试异常
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/3 22:37
 */
public class RedirectException extends LuckyRuntimeException {
    public RedirectException(String message) {
        super(message);
    }

    public RedirectException(Throwable ex) {
        super(ex);
    }

    public RedirectException(String message, Throwable ex) {
        super(message, ex);
    }

    public RedirectException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public RedirectException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
