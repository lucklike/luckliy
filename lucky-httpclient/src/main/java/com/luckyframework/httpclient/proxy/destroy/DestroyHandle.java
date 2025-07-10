package com.luckyframework.httpclient.proxy.destroy;

/**
 * 执行销毁逻辑的处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/1/18 00:57
 */
public interface DestroyHandle {

    /**
     * 销毁逻辑
     *
     * @param context 销毁上下文
     */
    void destroy(DestroyContext context) throws Throwable;
}
