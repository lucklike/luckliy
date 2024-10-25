package com.luckyframework.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 功能增强的Future类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/20 04:06
 */
public final class EnhanceFuture<T> {

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

    /**
     * 索引和名称映射关系的Map
     */
    private final Map<Integer, String> indexTaskMap = new HashMap<>();


    EnhanceFuture(@NonNull Executor taskExecutor, @Nullable Executor asyncResultProcessExecutor) {
        this.taskExecutor = taskExecutor;
        this.asyncResultProcessExecutor = asyncResultProcessExecutor == null ? taskExecutor : asyncResultProcessExecutor;
    }

    EnhanceFuture(@NonNull Executor taskExecutor) {
        this(taskExecutor, null);
    }

    EnhanceFuture() {
        this(new SimpleAsyncTaskExecutor("enhance-future-"));
    }

    public Executor getTaskExecutor() {
        return taskExecutor;
    }

    public Executor getAsyncResultProcessExecutor() {
        return asyncResultProcessExecutor;
    }

    public boolean hashTask(String taskName) {
        return futureMap.containsKey(taskName);
    }

    public int getTaskSize() {
        return futureMap.size();
    }

    /**
     * 添加一个异步任务
     *
     * @param taskName  任务名称
     * @param asyncTask 异步任务
     */
    public void addAsyncTask(String taskName, Supplier<T> asyncTask) {
        addTask(taskName, CompletableFuture.supplyAsync(asyncTask, taskExecutor));
    }

    /**
     * 添加一个异步任务，使用默认的任务名
     *
     * @param asyncTask 异步任务
     */
    public void addAsyncTask(Supplier<T> asyncTask) {
        addAsyncTask(getDefaultTaskName(), asyncTask);
    }

    /**
     * 直接添加一个{@link Future}
     *
     * @param taskName 任务名称
     * @param future   Future
     */
    public void addFuture(String taskName, Future<T> future) {
        addTask(taskName, future);
    }

    /**
     * 直接添加一个{@link Future}，使用默认的任务名
     *
     * @param future Future
     */
    public void addFuture(Future<T> future) {
        addFuture(getDefaultTaskName(), future);
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
        for (Map.Entry<String, Future<T>> futureEntry : getFutureMap().entrySet()) {
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
        Future<T> future = getFutureMap().get(taskName);
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
     * @param taskIndex        任务索引
     * @param timeout          超时时间
     * @param timeoutUnit      超时时间单位
     * @param resultProcess    结果处理器
     * @param exceptionHandler 异常处理器
     */
    public void resultProcess(int taskIndex, long timeout, TimeUnit timeoutUnit, FutureResultProcess<T> resultProcess, FutureExceptionHandler exceptionHandler) {
        resultProcess(getTaskNameByIndex(taskIndex), timeout, timeoutUnit, resultProcess, exceptionHandler);
    }

    /**
     * 处理某个任务的返回结果
     *
     * @param taskName         任务名称
     * @param resultProcess    结果处理器
     * @param exceptionHandler 异常处理器
     */
    public void resultProcess(String taskName, FutureResultProcess<T> resultProcess, FutureExceptionHandler exceptionHandler) {
        Future<T> future = getFutureMap().get(taskName);
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
     * @param taskIndex        任务索引
     * @param resultProcess    结果处理器
     * @param exceptionHandler 异常处理器
     */
    public void resultProcess(int taskIndex, FutureResultProcess<T> resultProcess, FutureExceptionHandler exceptionHandler) {
        resultProcess(getTaskNameByIndex(taskIndex), resultProcess, exceptionHandler);
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
     * @param taskIndex     任务索引
     * @param timeout       超时时间
     * @param timeoutUnit   超时时间单位
     * @param resultProcess 结果处理器
     */
    public void resultProcess(int taskIndex, long timeout, TimeUnit timeoutUnit, FutureResultProcess<T> resultProcess) {
        resultProcess(getTaskNameByIndex(taskIndex), timeout, timeoutUnit, resultProcess);
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
     * 处理某个任务的返回结果
     *
     * @param taskIndex     任务索引
     * @param resultProcess 结果处理器
     */
    public void resultProcess(int taskIndex, FutureResultProcess<T> resultProcess) {
        resultProcess(getTaskNameByIndex(taskIndex), resultProcess);
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
     * @param taskIndex        任务索引
     * @param timeout          超时时间
     * @param timeoutUnit      超时时间单位
     * @param exceptionHandler 异常处理器
     * @return 异步任务的处理结果
     */
    public T getTaskResult(int taskIndex, long timeout, TimeUnit timeoutUnit, FutureExceptionHandler exceptionHandler) {
        return getTaskResult(getTaskNameByIndex(taskIndex), timeout, timeoutUnit, exceptionHandler);
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
     * @param taskIndex        任务索引
     * @param exceptionHandler 异常处理器
     * @return 异步任务的处理结果
     */
    public T getTaskResult(int taskIndex, FutureExceptionHandler exceptionHandler) {
        return getTaskResult(getTaskNameByIndex(taskIndex), exceptionHandler);
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
     * @param taskIndex   任务索引
     * @param timeout     超时时间
     * @param timeoutUnit 超时时间单位
     * @return 异步任务的处理结果
     */
    public T getTaskResult(int taskIndex, long timeout, TimeUnit timeoutUnit) {
        return getTaskResult(getTaskNameByIndex(taskIndex), timeout, timeoutUnit);
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
     * 获取某个任务的返回结果
     *
     * @param taskIndex 任务索引
     * @return 异步任务的处理结果
     */
    public T getTaskResult(int taskIndex) {
        return getTaskResult(getTaskNameByIndex(taskIndex));
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
        Map<String, T> resultMap = new LinkedHashMap<>(getTaskSize());
        for (String taskName : getFutureMap().keySet()) {
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
     * 执行一个异步任务
     *
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
     * 获取所有的Future
     *
     * @return 所有的Future
     */
    public Collection<Future<T>> getFutures() {
        return futureMap.values();
    }

    /**
     * 执行一个异步任务
     *
     * @param runnable 异步任务
     */
    public void runAsync(Runnable runnable) {
        runAsync(runnable, DEFAULT_EXCEPTION_HANDLER);
    }


    private Map<String, Future<T>> getFutureMap() {
        return futureMap;
    }

    private synchronized void addTask(String taskName, Future<T> future) {
        if (hashTask(taskName)) {
            throw new IllegalArgumentException("Task '" + taskName + "' already exists.");
        }
        indexTaskMap.put(getTaskSize(), taskName);
        futureMap.put(taskName, future);
    }

    private Future<T> getTaskByIndex(Integer index) {
        return this.futureMap.get(getTaskNameByIndex(index));
    }

    private String getTaskNameByIndex(Integer index) {
        String taskName = this.indexTaskMap.get(index);
        Assert.notNull(taskName, "There is no corresponding task for index '" + index + "'");
        return taskName;
    }

    private synchronized String getDefaultTaskName() {
        return DEFAULT_TASK_PREFIX + getTaskSize();
    }

}
