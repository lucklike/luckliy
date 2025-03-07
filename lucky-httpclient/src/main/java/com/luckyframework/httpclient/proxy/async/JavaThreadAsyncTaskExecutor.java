package com.luckyframework.httpclient.proxy.async;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * 基于Java线程模型实现的异步任务执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/6 23:30
 */
public class JavaThreadAsyncTaskExecutor implements AsyncTaskExecutor {

    /**
     * 执行器
     */
    private final Executor executor;

    public JavaThreadAsyncTaskExecutor(@NonNull Executor executor) {
        Assert.notNull(executor, "executor must not be null");
        this.executor = executor;
    }


    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    @Override
    public Executor getExecutor() {
        return this.executor;
    }
}
