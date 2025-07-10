package com.luckyframework.httpclient.proxy.async;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * 异步执行器持有者
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/7 00:02
 */
public interface ExecutorHolder {

    /**
     * 获取异步执行器
     *
     * @return 异步执行器
     */
    Executor getExecutor();

    /**
     * 关闭线程池
     */
    default void shutdown() {
        Executor executor = getExecutor();
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }
}
