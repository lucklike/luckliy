package com.luckyframework.httpclient.proxy.plugin;

import com.luckyframework.exception.LuckyRuntimeException;

/**
 * 代理插件异常
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/6/18 01:43
 */
public class MockProxyPluginException extends LuckyRuntimeException {
    public MockProxyPluginException(String message) {
        super(message);
    }

    public MockProxyPluginException(Throwable ex) {
        super(ex);
    }

    public MockProxyPluginException(String message, Throwable ex) {
        super(message, ex);
    }

    public MockProxyPluginException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public MockProxyPluginException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
