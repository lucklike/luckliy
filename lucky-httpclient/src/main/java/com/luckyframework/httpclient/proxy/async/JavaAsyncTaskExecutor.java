package com.luckyframework.httpclient.proxy.async;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class JavaAsyncTaskExecutor implements AsyncTaskExecutor {

    private final Executor executor;

    public JavaAsyncTaskExecutor(@NonNull Executor executor) {
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
}
