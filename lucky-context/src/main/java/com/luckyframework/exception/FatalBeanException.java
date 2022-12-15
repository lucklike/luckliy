package com.luckyframework.exception;

import org.springframework.lang.Nullable;

/**
 * Thrown on an unrecoverable problem encountered in the
 * beans packages or sub-packages, e.g. bad class or field.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class FatalBeanException extends BeansException {

    /**
     * Create a new FatalBeanException with the specified message.
     * @param msg the detail message
     */
    public FatalBeanException(String msg) {
        super(msg);
    }

    /**
     * Create a new FatalBeanException with the specified message
     * and root cause.
     * @param msg the detail message
     * @param cause the root cause
     */
    public FatalBeanException(String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }

}
