package com.luckyframework.exception;

import org.springframework.lang.Nullable;

/**
 * @author fk
 * @version 1.0
 * @date 2021/3/12 0012 16:48
 */
public class BeansException extends NestedRuntimeException {


    /**
     * Create a new BeansException with the specified message.
     * @param msg the detail message
     */
    public BeansException(String msg) {
        super(msg);
    }

    /**
     * Create a new BeansException with the specified message
     * and root cause.
     * @param msg the detail message
     * @param cause the root cause
     */
    public BeansException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
