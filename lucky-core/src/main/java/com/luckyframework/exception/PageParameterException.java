package com.luckyframework.exception;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/6 12:00
 */
public class PageParameterException extends LuckyRuntimeException{

    public PageParameterException(String message) {
        super(message);
    }

    public PageParameterException(Throwable ex) {
        super(ex);
    }

    public PageParameterException(String message, Throwable ex) {
        super(message, ex);
    }

    public PageParameterException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public PageParameterException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
