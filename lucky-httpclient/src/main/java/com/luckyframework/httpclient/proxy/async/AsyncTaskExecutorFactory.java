package com.luckyframework.httpclient.proxy.async;

import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;

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
     * 根据【异步模型 + 线程池 + 并发数】来创建异步任务执行器
     *
     * @param executor    异步执行器
     * @param concurrency 并发数
     * @param model       异步模型
     * @return 异步任务执行器
     */
    public static AsyncTaskExecutor create(Executor executor, int concurrency, Model model) {
        if (model == Model.KOTLIN_COROUTINE) {
            return KotlinCoroutineAsyncTaskExecutor.createByExecutor(executor, concurrency);
        }
        return JavaThreadAsyncTaskExecutor.createByExecutor(executor, concurrency);
    }

    /**
     * 创建默认的异步任务执行器
     *
     * @param factory     代理工厂类
     * @param concurrency 并发数
     * @param model       异步模型
     * @return 异步任务执行器
     */
    public static AsyncTaskExecutor createDefault(HttpClientProxyObjectFactory factory, int concurrency, Model model) {
        if (model == Model.KOTLIN_COROUTINE) {
            return KotlinCoroutineAsyncTaskExecutor.createDefault(concurrency);
        }
        return JavaThreadAsyncTaskExecutor.createByExecutor(factory.getAsyncExecutor(), concurrency);
    }
}
