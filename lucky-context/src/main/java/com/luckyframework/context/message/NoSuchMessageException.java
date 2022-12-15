package com.luckyframework.context.message;

import java.util.Locale;

/**
 * 没有找到消息时会抛出该异常
 * @author FK7075
 * @version 1.0.0
 * @date 2022/11/22 09:17
 */
public class NoSuchMessageException extends RuntimeException {

    /**
     * Create a new exception.
     * @param code the code that could not be resolved for given locale
     * @param locale the locale that was used to search for the code within
     */
    public NoSuchMessageException(String code, Locale locale) {
        super("No message found under code '" + code + "' for locale '" + locale + "'.");
    }

    /**
     * Create a new exception.
     * @param code the code that could not be resolved for given locale
     */
    public NoSuchMessageException(String code) {
        super("No message found under code '" + code + "' for locale '" + Locale.getDefault() + "'.");
    }

}