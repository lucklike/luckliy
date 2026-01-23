package com.luckyframework.httpclient.proxy.context;

/**
 * {@link Context} Aware
 */
public interface ContextAware {

    /**
     * 设置{@link Context}
     *
     * @param context {@link Context}对象
     */
    void setContext(Context context);
}
