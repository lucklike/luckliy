package com.luckyframework.httpclient.exception;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 条件不满足异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/12 03:02
 */
public class ConditionNotSatisfiedException extends LuckyRuntimeException {

    public ConditionNotSatisfiedException(String message) {
        super(message);
    }

    public ConditionNotSatisfiedException(Throwable ex) {
        super(ex);
    }

    public ConditionNotSatisfiedException(String message, Throwable ex) {
        super(message, ex);
    }

    public ConditionNotSatisfiedException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ConditionNotSatisfiedException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
