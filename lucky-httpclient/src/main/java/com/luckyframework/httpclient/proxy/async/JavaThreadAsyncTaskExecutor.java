package com.luckyframework.httpclient.proxy.async;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * 基于Java线程模型实现的异步任务执行器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2025/3/6 23:30
 */
public class JavaThreadAsyncTaskExecutor implements AsyncTaskExecutor {

    private final Executor executor;
    private final Semaphore concurrencySemaphore;

    public static JavaThreadAsyncTaskExecutor createByExecutor(Executor executor) {
        return new JavaThreadAsyncTaskExecutor(executor, -1);
    }

    public static JavaThreadAsyncTaskExecutor createByExecutor(Executor executor, int concurrency) {
        return new JavaThreadAsyncTaskExecutor(executor, concurrency);
    }

    private JavaThreadAsyncTaskExecutor(@NonNull Executor executor, int concurrency) {
        Assert.notNull(executor, "executor must not be null");
        this.executor = executor;
        if (concurrency > 0) {
            this.concurrencySemaphore = new Semaphore(concurrency);
        } else {
            this.concurrencySemaphore = null;
        }

    }

    @Override
    public void execute(Runnable command) {
        if (concurrencySemaphore == null) {
            executor.execute(command);
        } else {
            executor.execute(() -> {
                try {
                    concurrencySemaphore.acquire();
                    command.run();
                } catch (InterruptedException e) {
                    throw new AsyncTaskExecutorException(e);
                } finally {
                    concurrencySemaphore.release();
                }
            });
        }

    }

    @Override
    public <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        if (concurrencySemaphore == null) {
            return CompletableFuture.supplyAsync(supplier, executor);
        } else {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    concurrencySemaphore.acquire();
                    return supplier.get();
                } catch (InterruptedException e) {
                    throw new AsyncTaskExecutorException(e);
                } finally {
                    concurrencySemaphore.release();
                }
            }, executor);
        }

    }

    @Override
    public Executor getExecutor() {
        return this.executor;
    }
}
