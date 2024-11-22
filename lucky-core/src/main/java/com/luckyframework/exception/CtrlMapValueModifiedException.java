package com.luckyframework.exception;

/**
 * 受控Map值修改异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/11/20 23:55
 */
public class CtrlMapValueModifiedException extends LuckyRuntimeException {
    public CtrlMapValueModifiedException(String message) {
        super(message);
    }

    public CtrlMapValueModifiedException(Throwable ex) {
        super(ex);
    }

    public CtrlMapValueModifiedException(String message, Throwable ex) {
        super(message, ex);
    }

    public CtrlMapValueModifiedException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public CtrlMapValueModifiedException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
