package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 配置解析异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/4 23:53
 */
public class ConfigurationParserException extends LuckyRuntimeException {
    public ConfigurationParserException(String message) {
        super(message);
    }

    public ConfigurationParserException(Throwable ex) {
        super(ex);
    }

    public ConfigurationParserException(String message, Throwable ex) {
        super(message, ex);
    }

    public ConfigurationParserException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public ConfigurationParserException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
