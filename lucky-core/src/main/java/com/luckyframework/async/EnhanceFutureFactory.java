package com.luckyframework.async;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * 用来生成{@link EnhanceFuture}的工厂
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/16 02:43
 */
public class EnhanceFutureFactory {

    /**
     * 用于执行异步任务的线程池
     */
    private final Executor taskExecutor;
    /**
     * 用于异步处理结果的线程池
     */
    private final Executor asyncResultProcessExecutor;

    public EnhanceFutureFactory(@NonNull Executor taskExecutor, @Nullable Executor asyncResultProcessExecutor) {
        this.taskExecutor = taskExecutor;
        this.asyncResultProcessExecutor = asyncResultProcessExecutor == null ? taskExecutor : asyncResultProcessExecutor;
    }

    public EnhanceFutureFactory(@NonNull Executor taskExecutor) {
        this(taskExecutor, null);
    }

    public EnhanceFutureFactory() {
        this(new SimpleAsyncTaskExecutor("enhance-future-"));
    }

    public <T> EnhanceFuture<T> create() {
        return new EnhanceFuture<>(taskExecutor, asyncResultProcessExecutor);
    }

    /**
     * 优雅关机
     */
    public void shutdown() {
        if (taskExecutor instanceof ExecutorService) {
            ((ExecutorService) taskExecutor).shutdown();
        }
        if (asyncResultProcessExecutor instanceof ExecutorService) {
            if (!((ExecutorService) asyncResultProcessExecutor).isShutdown()) {
                ((ExecutorService) asyncResultProcessExecutor).shutdown();
            }
        }
    }

    /**
     * 强制关机
     */
    public void shutdownNow() {
        if (taskExecutor instanceof ExecutorService) {
            ((ExecutorService) taskExecutor).shutdownNow();
        }
        if (asyncResultProcessExecutor instanceof ExecutorService) {
            if (!((ExecutorService) asyncResultProcessExecutor).isShutdown()) {
                ((ExecutorService) asyncResultProcessExecutor).shutdownNow();
            }
        }
    }

}
