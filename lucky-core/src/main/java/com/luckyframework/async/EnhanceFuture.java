package com.luckyframework.async;

import com.luckyframework.common.Console;
import com.luckyframework.common.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 功能增强的Future类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/20 04:06
 */
public class EnhanceFuture<T> {

    private static final Logger logger = LoggerFactory.getLogger(EnhanceFuture.class);
    private static final FutureExceptionHandler DEFAULT_EXCEPTION_HANDLER = tx -> {
        throw new EnhanceFutureException(tx);
    };

    /**
     * 默认线程名前缀
     */
    private final static String DEFAULT_TASK_PREFIX = "EnhanceFuture-";

    /**
     * 用于执行异步任务的线程池
     */
    private final Executor taskExecutor;
    /**
     * 用于异步处理结果的线程池
     */
    private final Executor asyncResultProcessExecutor;
    /**
     * Future Map
     */
    private final Map<String, Future<T>> futureMap = new LinkedHashMap<>();


    public EnhanceFuture(@NonNull Executor taskExecutor, @Nullable Executor asyncResultProcessExecutor) {
        this.taskExecutor = taskExecutor;
        this.asyncResultProcessExecutor = asyncResultProcessExecutor == null ? taskExecutor : asyncResultProcessExecutor;
    }

    public EnhanceFuture(@NonNull Executor taskExecutor) {
        this(taskExecutor, null);
    }

    public EnhanceFuture() {
        this(new SimpleAsyncTaskExecutor("enhance-future-"));
    }

    public Executor getTaskExecutor() {
        return taskExecutor;
    }

    public Executor getAsyncResultProcessExecutor() {
        return asyncResultProcessExecutor;
    }

    /**
     * 添加一个异步任务
     *
     * @param taskName  任务名称
     * @param asyncTask 异步任务
     */
    public synchronized void addAsyncTask(String taskName, Supplier<T> asyncTask) {
        if (futureMap.containsKey(taskName)) {
            throw new IllegalArgumentException("Task '" + taskName + "' already exists.");
        }
        futureMap.put(taskName, CompletableFuture.supplyAsync(asyncTask, taskExecutor));
    }

    /**
     * 添加一个异步任务，使用默认的任务名
     *
     * @param asyncTask 异步任务
     */
    public void addAsyncTask(Supplier<T> asyncTask) {
        addAsyncTask(DEFAULT_TASK_PREFIX + futureMap.size(), asyncTask);
    }

    /**
     * 直接添加一个{@link Future}
     *
     * @param taskName 任务名称
     * @param future   Future
     */
    public void addFuture(String taskName, Future<T> future) {
        if (futureMap.containsKey(taskName)) {
            throw new IllegalArgumentException("Task '" + taskName + "' already exists.");
        }
        futureMap.put(taskName, future);
    }

    /**
     * 直接添加一个{@link Future}，使用默认的任务名
     *
     * @param future Future
     */
    public void addFuture(Future<T> future) {
        addFuture(DEFAULT_TASK_PREFIX + futureMap.size(), future);
    }

    /**
     * 异步处理所有任务的结果
     *
     * @param resultProcess    结果处理器
     * @param exceptionHandler 异常处理器
     */
    public void asyncResultProcess(FutureResultProcess<T> resultProcess, FutureExceptionHandler exceptionHandler) {
        for (T result : getResults(exceptionHandler)) {
            CompletableFuture.runAsync(() -> {
                try {
                    resultProcess.resultProcess(result);
                } catch (Exception e) {
                    exceptionHandler.handleException(e);
                }
            }, getAsyncResultProcessExecutor());
        }
    }

    /**
     * 异步处理所有任务的结果，使用默认的异常处理器（打印错误堆栈信息）
     *
     * @param resultProcess 结果处理器
     */
    public void asyncResultProcess(FutureResultProcess<T> resultProcess) {
        asyncResultProcess(resultProcess, Throwable::printStackTrace);
    }

    public void resultProcess(FutureResultProcess<T> resultProcess, FutureExceptionHandler exceptionHandler) {
        for (Map.Entry<String, Future<T>> futureEntry : futureMap.entrySet()) {
            Future<T> future = futureEntry.getValue();
            try {
                resultProcess.resultProcess(future.get());
            } catch (Exception e) {
                logger.error("The task whose name is '{}' is normal.", futureEntry.getKey());
                exceptionHandler.handleException(e);
            }
        }
    }

    /**
     * 串行方法处理结果
     *
     * @param resultProcess 结果处理器
     */
    public void resultProcess(FutureResultProcess<T> resultProcess) {
        resultProcess(resultProcess, DEFAULT_EXCEPTION_HANDLER);
    }

    /**
     * 处理某个任务的返回结果
     *
     * @param taskName         任务名称
     * @param timeout          超时时间
     * @param timeoutUnit      超时时间单位
     * @param resultProcess    结果处理器
     * @param exceptionHandler 异常处理器
     */
    public void resultProcess(String taskName, long timeout, TimeUnit timeoutUnit, FutureResultProcess<T> resultProcess, FutureExceptionHandler exceptionHandler) {
        Future<T> future = futureMap.get(taskName);
        Assert.notNull(future, "Task '" + taskName + "' does not exist.");
        try {
            resultProcess.resultProcess(future.get(timeout, timeoutUnit));
        } catch (Exception e) {
            exceptionHandler.handleException(e);
        }
    }

    /**
     * 处理某个任务的返回结果
     *
     * @param taskName         任务名称
     * @param resultProcess    结果处理器
     * @param exceptionHandler 异常处理器
     */
    public void resultProcess(String taskName, FutureResultProcess<T> resultProcess, FutureExceptionHandler exceptionHandler) {
        Future<T> future = futureMap.get(taskName);
        Assert.notNull(future, "Task '" + taskName + "' does not exist.");
        try {
            resultProcess.resultProcess(future.get());
        } catch (Exception e) {
            exceptionHandler.handleException(e);
        }
    }

    /**
     * 处理某个任务的返回结果
     *
     * @param taskName      任务名称
     * @param timeout       超时时间
     * @param timeoutUnit   超时时间单位
     * @param resultProcess 结果处理器
     */
    public void resultProcess(String taskName, long timeout, TimeUnit timeoutUnit, FutureResultProcess<T> resultProcess) {
        resultProcess(taskName, timeout, timeoutUnit, resultProcess, DEFAULT_EXCEPTION_HANDLER);
    }

    /**
     * 处理某个任务的返回结果
     *
     * @param taskName      任务名称
     * @param resultProcess 结果处理器
     */
    public void resultProcess(String taskName, FutureResultProcess<T> resultProcess) {
        resultProcess(taskName, resultProcess, DEFAULT_EXCEPTION_HANDLER);
    }

    /**
     * 获取某个任务的返回结果
     *
     * @param taskName         任务名
     * @param timeout          超时时间
     * @param timeoutUnit      超时时间单位
     * @param exceptionHandler 异常处理器
     * @return 异步任务的处理结果
     */
    public T getTaskResult(String taskName, long timeout, TimeUnit timeoutUnit, FutureExceptionHandler exceptionHandler) {
        AtomicReference<T> result = new AtomicReference<>();
        resultProcess(taskName, timeout, timeoutUnit, result::set, exceptionHandler);
        return result.get();
    }

    /**
     * 获取某个任务的返回结果
     *
     * @param taskName         任务名
     * @param exceptionHandler 异常处理器
     * @return 异步任务的处理结果
     */
    public T getTaskResult(String taskName, FutureExceptionHandler exceptionHandler) {
        AtomicReference<T> result = new AtomicReference<>();
        resultProcess(taskName, result::set, exceptionHandler);
        return result.get();
    }

    /**
     * 获取某个任务的返回结果
     *
     * @param taskName    任务名
     * @param timeout     超时时间
     * @param timeoutUnit 超时时间单位
     * @return 异步任务的处理结果
     */
    public T getTaskResult(String taskName, long timeout, TimeUnit timeoutUnit) {
        return getTaskResult(taskName, timeout, timeoutUnit, DEFAULT_EXCEPTION_HANDLER);
    }

    /**
     * 获取某个任务的返回结果
     *
     * @param taskName 任务名
     * @return 异步任务的处理结果
     */
    public T getTaskResult(String taskName) {
        return getTaskResult(taskName, DEFAULT_EXCEPTION_HANDLER);
    }

    /**
     * 获取所有任务名与任务结果所组成的Map
     *
     * @param timeout          超时时间
     * @param timeoutUnit      超时时间单位
     * @param exceptionHandler 异常处理器
     * @return 所有任务名与任务结果所组成的Map
     * @throws TimeoutException 超时异常
     */
    public Map<String, T> getResultMap(long timeout, TimeUnit timeoutUnit, FutureExceptionHandler exceptionHandler) throws TimeoutException {
        Timeout _timeout = new Timeout();
        return _timeout.timeoutThrowException(() -> getResultMap(exceptionHandler), timeout, timeoutUnit);
    }

    /**
     * 获取所有任务名与任务结果所组成的Map
     *
     * @param exceptionHandler 异常处理器
     * @return 所有任务名与任务结果所组成的Map
     */
    public Map<String, T> getResultMap(FutureExceptionHandler exceptionHandler) {
        Map<String, T> resultMap = new LinkedHashMap<>(futureMap.size());
        for (String taskName : futureMap.keySet()) {
            resultMap.put(taskName, getTaskResult(taskName, exceptionHandler));
        }
        return resultMap;
    }

    /**
     * 获取所有任务名与任务结果所组成的Map
     *
     * @param timeout     超时时间
     * @param timeoutUnit 超时时间单位
     * @return 所有任务名与任务结果所组成的Map
     * @throws TimeoutException 超时异常
     */
    public Map<String, T> getResultMap(long timeout, TimeUnit timeoutUnit) throws TimeoutException {
        return getResultMap(timeout, timeoutUnit, DEFAULT_EXCEPTION_HANDLER);
    }

    /**
     * 获取所有任务名与任务结果所组成的Map
     *
     * @return 所有任务名与任务结果所组成的Map
     */
    public Map<String, T> getResultMap() {
        return getResultMap(DEFAULT_EXCEPTION_HANDLER);
    }

    /**
     * 获取所有任务的执行结果
     *
     * @param timeout          超时时间
     * @param timeoutUnit      超时时间单位
     * @param exceptionHandler 异常处理器
     * @return 所有任务的执行结果
     * @throws TimeoutException 超时异常
     */
    public Collection<T> getResults(long timeout, TimeUnit timeoutUnit, FutureExceptionHandler exceptionHandler) throws TimeoutException {
        return getResultMap(timeout, timeoutUnit, exceptionHandler).values();
    }

    /**
     * 获取所有任务的执行结果
     *
     * @param exceptionHandler 异常处理器
     * @return 所有任务的执行结果
     */
    public Collection<T> getResults(FutureExceptionHandler exceptionHandler) {
        return getResultMap(exceptionHandler).values();
    }

    /**
     * 获取所有任务的执行结果
     *
     * @param timeout     超时时间
     * @param timeoutUnit 超时时间单位
     * @return 所有任务的执行结果
     * @throws TimeoutException 超时异常
     */
    public Collection<T> getResults(long timeout, TimeUnit timeoutUnit) throws TimeoutException {
        return getResults(timeout, timeoutUnit, DEFAULT_EXCEPTION_HANDLER);
    }

    /**
     * 获取所有任务的执行结果
     *
     * @return 所有任务的执行结果
     */
    public Collection<T> getResults() {
        return getResults(DEFAULT_EXCEPTION_HANDLER);
    }

    /**
     * 直营一个异步任务
     * @param runnable 异步任务
     * @param handler  异常处理器
     */
    public void runAsync(Runnable runnable, FutureExceptionHandler handler) {
        CompletableFuture<Void> completableFuture = CompletableFuture.runAsync(runnable, getTaskExecutor());
        completableFuture.exceptionally(e -> {
            handler.handleException(e);
            return null;
        });
    }

    /**
     * 直营一个异步任务
     * @param runnable 异步任务
     */
    public void runAsync(Runnable runnable) {
        runAsync(runnable, DEFAULT_EXCEPTION_HANDLER);
    }

    /**
     * 清除所有已经注册的任务
     */
    public synchronized void clearTasks() {
        this.futureMap.clear();
    }

    /**
     * 优雅关机
     */
    public void shutdown() {
        clearTasks();
        if (taskExecutor instanceof ExecutorService) {
            ((ExecutorService) taskExecutor).shutdown();
        }
    }

    /**
     * 强制关机
     */
    public void shutdownNow() {
        clearTasks();
        if (taskExecutor instanceof ExecutorService) {
            ((ExecutorService) taskExecutor).shutdownNow();
        }
    }

}
