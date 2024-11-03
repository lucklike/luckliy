package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * Lucky代理方法执行异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/3 22:06
 */
public class LuckyProxyMethodExecuteException extends LuckyRuntimeException {
    public LuckyProxyMethodExecuteException(String message) {
        super(message);
    }

    public LuckyProxyMethodExecuteException(Throwable ex) {
        super(ex);
    }

    public LuckyProxyMethodExecuteException(String message, Throwable ex) {
        super(message, ex);
    }

    public LuckyProxyMethodExecuteException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public LuckyProxyMethodExecuteException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
