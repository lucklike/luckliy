package com.luckyframework.httpclient.proxy.configapi;

import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/7/1 01:22
 */
public class ConfigContextApi {
    private final ConfigApi api;
    private final MethodContext context;

    public ConfigContextApi(ConfigApi api, MethodContext context) {
        this.api = api;
        this.context = context;
    }

    public ConfigApi getApi() {
        return api;
    }

    public MethodContext getContext() {
        return context;
    }
}
