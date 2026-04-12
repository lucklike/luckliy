package com.luckyframework.async;

import com.luckyframework.spel.LazyValue;
import com.luckyframework.threadpool.ThreadPoolFactory;
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
    private final LazyValue<Executor> taskExecutor;
    /**
     * 用于异步处理结果的线程池
     */
    private final LazyValue<Executor> asyncResultProcessExecutor;

    public EnhanceFutureFactory(@NonNull LazyValue<Executor> taskExecutor, @Nullable LazyValue<Executor> asyncResultProcessExecutor) {
        this.taskExecutor = taskExecutor;
        this.asyncResultProcessExecutor = asyncResultProcessExecutor == null ? taskExecutor : asyncResultProcessExecutor;
    }

    public EnhanceFutureFactory(@NonNull LazyValue<Executor> taskExecutor) {
        this(taskExecutor, null);
    }

    public EnhanceFutureFactory() {
        this(LazyValue.of(() -> ThreadPoolFactory.createIOIntensiveThreadPool("enhance-future-", 0.3)));
    }

    public <T> EnhanceFuture<T> create() {
        return new EnhanceFuture<>(taskExecutor, asyncResultProcessExecutor);
    }

    public Executor getTaskExecutor() {
        return taskExecutor.getValue();
    }

    public Executor getAsyncResultProcessExecutor() {
        return asyncResultProcessExecutor.getValue();
    }

    /**
     * 优雅关机
     */
    public void shutdown() {
        Executor taskExecutor = getTaskExecutor();
        if (taskExecutor instanceof ExecutorService) {
            if (!((ExecutorService) taskExecutor).isShutdown()) {
                ((ExecutorService) taskExecutor).shutdown();
            }
        }

        Executor asyncResultProcessExecutor = getAsyncResultProcessExecutor();
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
        Executor taskExecutor = getTaskExecutor();
        if (taskExecutor instanceof ExecutorService) {
            if (!((ExecutorService) taskExecutor).isShutdown()) {
                ((ExecutorService) taskExecutor).shutdownNow();
            }
        }

        Executor asyncResultProcessExecutor = getAsyncResultProcessExecutor();
        if (asyncResultProcessExecutor instanceof ExecutorService) {
            if (!((ExecutorService) asyncResultProcessExecutor).isShutdown()) {
                ((ExecutorService) asyncResultProcessExecutor).shutdownNow();
            }
        }
    }

}
