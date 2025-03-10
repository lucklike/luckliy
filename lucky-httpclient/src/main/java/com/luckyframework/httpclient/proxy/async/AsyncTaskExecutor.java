package com.luckyframework.httpclient.proxy.async;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 异步任务执行器接口
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/6 23:30
 */
public interface AsyncTaskExecutor extends ExecutorHolder {

    /**
     * 异步执行任务，不返回结果
     *
     * @param command 要执行的任务
     */
    void execute(Runnable command);


    /**
     * 异步执行任务，并返回执行结果
     *
     * @param supplier 要执行的任务
     * @param <R>      任务结果类型
     * @return 任务执行结果的CompletableFuture包装类
     */
    <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier);
}
