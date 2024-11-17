package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 变量展开异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/16 03:28
 */
public class VarUnfoldException extends LuckyRuntimeException {
    public VarUnfoldException(String message) {
        super(message);
    }

    public VarUnfoldException(Throwable ex) {
        super(ex);
    }

    public VarUnfoldException(String message, Throwable ex) {
        super(message, ex);
    }

    public VarUnfoldException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public VarUnfoldException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
