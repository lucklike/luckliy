package com.luckyframework.httpclient.proxy.typeparser.support;

import com.luckyframework.httpclient.proxy.async.AsyncTaskExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 记录型异步任务执行器
 * <pre>
 *     1.记录每一次{@link #supplyAsync(Supplier)}返回的Future，可用于断言任务的取消状态
 *     2.统计提交的任务数量
 *     3.跟踪在途任务数量，可用于断言任务是否已结束
 *     4.支持手动模式：提交的任务不会立即执行，由测试通过{@link #drain()}手动驱动，
 *       以便确定性地构造"任务尚未执行/执行中"的中间态
 * </pre>
 * 行为说明：{@link #supplyAsync(Supplier)}的完成语义与{@link CompletableFuture#supplyAsync(Supplier)}对齐，
 * 即任务异常以{@link CompletionException}包装后暴露给whenComplete回调。
 */
public class RecordingAsyncTaskExecutor implements AsyncTaskExecutor {

    private final ExecutorService delegate;
    private final List<CompletableFuture<?>> supplyFutures = Collections.synchronizedList(new ArrayList<>());
    private final List<Runnable> pendingTasks = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger submittedCount = new AtomicInteger();
    private final AtomicInteger inFlightCount = new AtomicInteger();
    private final Object idleMonitor = new Object();
    private volatile boolean manualMode;

    public RecordingAsyncTaskExecutor() {
        this.delegate = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "recording-async-executor");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 开启/关闭手动模式
     */
    public void setManualMode(boolean manualMode) {
        this.manualMode = manualMode;
    }

    /**
     * 已提交的任务总数（execute + supplyAsync）
     */
    public int submittedCount() {
        return submittedCount.get();
    }

    /**
     * 最近一次supplyAsync返回的Future
     */
    public CompletableFuture<?> lastSupplyFuture() {
        synchronized (supplyFutures) {
            return supplyFutures.isEmpty() ? null : supplyFutures.get(supplyFutures.size() - 1);
        }
    }

    /**
     * 等待所有在途任务执行结束
     *
     * @param timeoutMillis 超时时间
     * @return 超时时间内全部结束返回true
     */
    public boolean awaitIdle(long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        synchronized (idleMonitor) {
            while (inFlightCount.get() > 0) {
                long remain = deadline - System.currentTimeMillis();
                if (remain <= 0) {
                    return false;
                }
                try {
                    idleMonitor.wait(remain);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 手动模式下同步执行全部待执行任务，返回执行的任务数
     */
    public int drain() {
        List<Runnable> tasks;
        synchronized (pendingTasks) {
            tasks = new ArrayList<>(pendingTasks);
            pendingTasks.clear();
        }
        for (Runnable task : tasks) {
            task.run();
        }
        return tasks.size();
    }

    /**
     * 丢弃全部待执行任务（不执行）
     */
    public void discardPending() {
        pendingTasks.clear();
    }

    @Override
    public void execute(Runnable command) {
        submittedCount.incrementAndGet();
        Runnable wrapped = wrap(command);
        if (manualMode) {
            pendingTasks.add(wrapped);
        } else {
            delegate.execute(wrapped);
        }
    }

    @Override
    public <R> CompletableFuture<R> supplyAsync(Supplier<R> supplier) {
        submittedCount.incrementAndGet();
        CompletableFuture<R> future = new CompletableFuture<>();
        supplyFutures.add(future);
        Runnable task = wrap(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable e) {
                future.completeExceptionally(e instanceof CompletionException ? e : new CompletionException(e));
            }
        });
        if (manualMode) {
            pendingTasks.add(task);
        } else {
            delegate.execute(task);
        }
        return future;
    }

    @Override
    public Executor getExecutor() {
        return delegate;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public void shutdownNow() {
        delegate.shutdownNow();
    }

    private Runnable wrap(Runnable task) {
        return () -> {
            inFlightCount.incrementAndGet();
            try {
                task.run();
            } finally {
                inFlightCount.decrementAndGet();
                synchronized (idleMonitor) {
                    idleMonitor.notifyAll();
                }
            }
        };
    }
}
