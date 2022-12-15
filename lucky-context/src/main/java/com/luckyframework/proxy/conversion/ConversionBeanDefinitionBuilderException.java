package com.luckyframework.proxy.conversion;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/30 02:19
 */
public class ConversionBeanDefinitionBuilderException extends LuckyRuntimeException {
    public ConversionBeanDefinitionBuilderException(String message) {
        super(message);
    }

    public ConversionBeanDefinitionBuilderException(Throwable ex) {
        super(ex);
    }

    public ConversionBeanDefinitionBuilderException(String message, Throwable ex) {
        super(message, ex);
    }

    public ConversionBeanDefinitionBuilderException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ConversionBeanDefinitionBuilderException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
