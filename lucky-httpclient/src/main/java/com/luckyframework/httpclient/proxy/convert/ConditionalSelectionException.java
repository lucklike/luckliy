package com.luckyframework.httpclient.proxy.convert;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 通用的基于SpEL表达式的响应转换器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/23 10:46
 */
public class ConditionalSelectionException extends LuckyRuntimeException {
    public ConditionalSelectionException(String message) {
        super(message);
    }

    public ConditionalSelectionException(Throwable ex) {
        super(ex);
    }

    public ConditionalSelectionException(String message, Throwable ex) {
        super(message, ex);
    }

    public ConditionalSelectionException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ConditionalSelectionException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
