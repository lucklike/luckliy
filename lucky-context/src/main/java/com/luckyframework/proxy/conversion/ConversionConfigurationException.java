package com.luckyframework.proxy.conversion;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/10/5 01:38
 */
public class ConversionConfigurationException extends LuckyRuntimeException {
    public ConversionConfigurationException(String message) {
        super(message);
    }

    public ConversionConfigurationException(Throwable ex) {
        super(ex);
    }

    public ConversionConfigurationException(String message, Throwable ex) {
        super(message, ex);
    }

    public ConversionConfigurationException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ConversionConfigurationException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
