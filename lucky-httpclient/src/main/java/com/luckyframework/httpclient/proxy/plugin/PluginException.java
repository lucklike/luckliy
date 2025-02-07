package com.luckyframework.httpclient.proxy.plugin;

import com.luckyframework.exception.LuckyRuntimeException;

public class PluginException extends LuckyRuntimeException {
    public PluginException(String message) {
        super(message);
    }

    public PluginException(Throwable ex) {
        super(ex);
    }

    public PluginException(String message, Throwable ex) {
        super(message, ex);
    }

    public PluginException(String messageTemplate, Object... args) {
        super(messageTemplate, args);
    }

    public PluginException(Throwable ex, String messageTemplate, Object... args) {
        super(ex, messageTemplate, args);
    }
}
