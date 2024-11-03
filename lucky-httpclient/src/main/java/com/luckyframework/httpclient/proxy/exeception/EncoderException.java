package com.luckyframework.httpclient.proxy.exeception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 编码异常
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/3 22:30
 */
public class EncoderException extends LuckyRuntimeException {
    public EncoderException(String message) {
        super(message);
    }

    public EncoderException(Throwable ex) {
        super(ex);
    }

    public EncoderException(String message, Throwable ex) {
        super(message, ex);
    }

    public EncoderException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public EncoderException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
