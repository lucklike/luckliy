package com.luckyframework.exception;

import java.lang.reflect.InvocationTargetException;

/**
 * 将InvocationTargetException转化为RuntimeException
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/5 17:59
 */
public class LuckyInvocationTargetException extends LuckyRuntimeException {

    public LuckyInvocationTargetException(InvocationTargetException ex) {
        super(ex);
    }

    @Override
    public synchronized Throwable getCause() {
        return ((InvocationTargetException) super.getCause()).getTargetException();
    }
}
