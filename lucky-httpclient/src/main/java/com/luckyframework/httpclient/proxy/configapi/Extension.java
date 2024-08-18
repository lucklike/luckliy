package com.luckyframework.httpclient.proxy.configapi;

/**
 * 扩展配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/8/18 20:11
 */
public class Extension<T> {

    private ExtendHandleConfig<T> handle;
    private Object config;


    public ExtendHandleConfig<T> getHandle() {
        return handle;
    }

    public void setHandle(ExtendHandleConfig<T> handle) {
        this.handle = handle;
    }

    public Object getConfig() {
        return config;
    }

    public void setConfig(Object config) {
        this.config = config;
    }
}
