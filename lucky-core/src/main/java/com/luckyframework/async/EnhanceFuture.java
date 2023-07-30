package com.luckyframework.async;

import com.luckyframework.common.Console;
import com.luckyframework.common.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2023/5/20 04:06
 */
public class EnhanceFuture<T> {

    private static final Logger log = LoggerFactory.getLogger(EnhanceFuture.class);
    private static final FutureExceptionHandler DEFAULT_EXCEPTION_HANDLER = tx -> {
        throw new EnhanceFutureException(tx);
    };

    private final static String DEFAULT_TASK_PREFIX = "EnhanceFuture-";

    private final Executor executor;

    private final Map<String, Future<T>> futureMap = new LinkedHashMap<>();

    public EnhanceFuture(){
        this(new SimpleAsyncTaskExecutor("enhance-future-"));
    }

    public EnhanceFuture(Executor executor) {
        this.executor = executor;
    }

    public synchronized void addAsyncTask(String taskName, Supplier<T> asyncTask) {
        if (futureMap.containsKey(taskName)) {
            throw new IllegalArgumentException("Task '" + taskName + "' already exists.");
        }
        futureMap.put(taskName, CompletableFuture.supplyAsync(asyncTask, executor));
    }

    public void addAsyncTask(Supplier<T> asyncTask) {
        addAsyncTask(DEFAULT_TASK_PREFIX + futureMap.size(), asyncTask);
    }

    public void resultProcess(String taskName, long timeout, TimeUnit timeoutUnit, FutureResultProcess<T> resultProcess, FutureExceptionHandler exceptionHandler) {
        Future<T> future = futureMap.get(taskName);
        Assert.notNull(future, "Task '" + taskName + "' does not exist.");
        try {
            resultProcess.resultProcess(future.get(timeout, timeoutUnit));
        } catch (Exception e) {
            exceptionHandler.handleException(e);
        }
    }


    public void resultProcess(String taskName, FutureResultProcess<T> resultProcess, FutureExceptionHandler exceptionHandler) {
        Future<T> future = futureMap.get(taskName);
        Assert.notNull(future, "Task '" + taskName + "' does not exist.");
        try {
            resultProcess.resultProcess(future.get());
        } catch (Exception e) {
            exceptionHandler.handleException(e);
        }
    }

    public void resultProcess(String taskName, long timeout, TimeUnit timeoutUnit, FutureResultProcess<T> resultProcess) {
        resultProcess(taskName, timeout, timeoutUnit, resultProcess, DEFAULT_EXCEPTION_HANDLER);
    }

    public void resultProcess(String taskName, FutureResultProcess<T> resultProcess) {
        resultProcess(taskName, resultProcess, DEFAULT_EXCEPTION_HANDLER);
    }

    public T getTaskResult(String taskName, long timeout, TimeUnit timeoutUnit, FutureExceptionHandler exceptionHandler) {
        AtomicReference<T> result = new AtomicReference<>();
        resultProcess(taskName, timeout, timeoutUnit, result::set, exceptionHandler);
        return result.get();
    }

    public T getTaskResult(String taskName, FutureExceptionHandler exceptionHandler) {
        AtomicReference<T> result = new AtomicReference<>();
        resultProcess(taskName, result::set, exceptionHandler);
        return result.get();
    }

    public T getTaskResult(String taskName, long timeout, TimeUnit timeoutUnit) {
        return getTaskResult(taskName, timeout, timeoutUnit, DEFAULT_EXCEPTION_HANDLER);
    }

    public T getTaskResult(String taskName) {
        return getTaskResult(taskName, DEFAULT_EXCEPTION_HANDLER);
    }

    public Map<String, T> getResultMap(FutureExceptionHandler exceptionHandler, long timeout, TimeUnit timeoutUnit) throws TimeoutException {
        Timeout _timeout = new Timeout();
        return _timeout.timeoutThrowException(() -> getResultMap(exceptionHandler), timeout, timeoutUnit);
    }

    public Map<String, T> getResultMap(FutureExceptionHandler exceptionHandler){
        Map<String, T> resultMap = new LinkedHashMap<>(futureMap.size());
        for (String taskName : futureMap.keySet()) {
            resultMap.put(taskName, getTaskResult(taskName, exceptionHandler));
        }
        return resultMap;
    }

    public Map<String, T> getResultMap(long timeout, TimeUnit timeoutUnit) throws TimeoutException {
        return getResultMap(DEFAULT_EXCEPTION_HANDLER, timeout, timeoutUnit);
    }

    public Map<String, T> getResultMap(){
        return getResultMap(DEFAULT_EXCEPTION_HANDLER);
    }

    public Collection<T> getResults(FutureExceptionHandler exceptionHandler, long timeout, TimeUnit timeoutUnit) throws TimeoutException {
        return getResultMap(exceptionHandler, timeout, timeoutUnit).values();
    }

    public Collection<T> getResults(FutureExceptionHandler exceptionHandler){
        return getResultMap(exceptionHandler).values();
    }

    public Collection<T> getResults(long timeout, TimeUnit timeoutUnit) throws TimeoutException {
        return getResults(DEFAULT_EXCEPTION_HANDLER, timeout, timeoutUnit);
    }

    public Collection<T> getResults(){
        return getResults(DEFAULT_EXCEPTION_HANDLER);
    }

    public synchronized void clearTasks(){
        this.futureMap.clear();
    }

    public void shutdown(){
        clearTasks();
        if(executor instanceof ExecutorService){
            ((ExecutorService) executor).shutdown();
        }
    }

    public static void main(String[] args) throws TimeoutException {
        EnhanceFuture<String> enhanceFuture = new EnhanceFuture<>();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("addTask");
        enhanceFuture.addAsyncTask(EnhanceFuture::test);
        enhanceFuture.addAsyncTask(EnhanceFuture::test);
        enhanceFuture.addAsyncTask(EnhanceFuture::test);
        enhanceFuture.addAsyncTask(EnhanceFuture::test);
        stopWatch.stopLast();

        stopWatch.start("get1");
        enhanceFuture.getResultMap(1020L, TimeUnit.MILLISECONDS).forEach((k, v) -> Console.println("{}={}", k, v));
        stopWatch.stopLast();

        stopWatch.start("get2");
        enhanceFuture.getResultMap().forEach((k, v) -> Console.printlnCyan("{}={}", k, v));
        stopWatch.stopWatch();

        System.out.println(stopWatch.prettyPrintMillis());
        enhanceFuture.shutdown();
    }

    private static String test(){
        try {
            Thread.sleep(1000L);
            return UUID.randomUUID().toString();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
