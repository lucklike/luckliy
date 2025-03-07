package com.luckyframework.httpclient.proxy.async;

import java.util.concurrent.Executor;

/**
 * 异步任务执行器工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/6 23:47
 */
public class AsyncTaskExecutorFactory {

    /**
     * 根据异步模型来创建异步任务执行器
     *
     * @param executor 异步执行器
     * @param model    异步模型
     * @return 异步任务执行器
     */
    public static AsyncTaskExecutor create(Executor executor, Model model) {
        if (model == Model.KOTLIN_COROUTINE) {
            return new KotlinCoroutineAsyncTaskExecutor(executor);
        }
        return new JavaThreadAsyncTaskExecutor(executor);
    }
}
