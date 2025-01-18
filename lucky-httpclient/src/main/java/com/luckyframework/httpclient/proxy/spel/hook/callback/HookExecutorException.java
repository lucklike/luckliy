package com.luckyframework.httpclient.proxy.spel.hook.callback;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * Hook执行异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/18 03:06
 */
public class HookExecutorException extends LuckyRuntimeException {
    public HookExecutorException(String message) {
        super(message);
    }

    public HookExecutorException(Throwable ex) {
        super(ex);
    }

    public HookExecutorException(String message, Throwable ex) {
        super(message, ex);
    }

    public HookExecutorException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public HookExecutorException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
