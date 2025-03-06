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

    public static AsyncTaskExecutor create(Executor executor, Model model) {
        if (model == Model.KOTLIN_COROUTINE) {
            return new KotlinCoroutineAsyncTaskExecutor(executor);
        }
        return new JavaAsyncTaskExecutor(executor);
    }
}
