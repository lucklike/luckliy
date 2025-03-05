package com.luckyframework.httpclient.proxy.handle;

/**
 * 结果处理器
 *
 * @param <T> 结果类型
 * @author fukang
 * @version 3.0.1
 * @date 2025/03/05 16:16
 */
@FunctionalInterface
public interface ResultHandler<T> {

    /**
     * 处理结果
     *
     * @param resultContext 结果上下文
     */
    void handleResult(ResultContext<T> resultContext) throws Throwable;
}