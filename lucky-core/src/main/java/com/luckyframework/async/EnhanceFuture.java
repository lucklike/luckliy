package com.luckyframework.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * @author FK7075
 * @version 1.0.0
 * @date 2022/9/13 11:46
 */
public class EnhanceFuture<T> {

    private final static Logger logger = LoggerFactory.getLogger(EnhanceFuture.class);
    private final static String DEFAULT_TASK_PREFIX = "EnhanceFuture-";

    private final Executor executor;

    private final Map<String, Future<T>> taskMap = new ConcurrentHashMap<>();
    private final Map<String, EnhanceFutureExceptionHandler> exceptionHandlerMap = new ConcurrentHashMap<>();

    public EnhanceFuture(Executor executor) {
        this.executor = executor;
    }

    public EnhanceFuture(){
        this(new SimpleAsyncTaskExecutor("enhance-future-"));
    }

    public void addTask(Supplier<T> supplier){
        addTask(supplier, null);
    }

    public void addTask(Supplier<T> supplier,EnhanceFutureExceptionHandler exceptionHandler){
        String taskName = DEFAULT_TASK_PREFIX + taskMap.size();
        addTask(taskName, supplier, exceptionHandler);
    }

    public void addTask(String name, Supplier<T> task){
        addTask(name, task, null);
    }

    public void addTask(String name, Supplier<T> task, EnhanceFutureExceptionHandler exceptionHandler){
        Future<T> future = taskMap.get(name);
        if(future != null){
            if(!future.isDone()){
                future.cancel(true);
            }
        }
        if(exceptionHandler != null){
            exceptionHandlerMap.put(name, exceptionHandler);
        }
        taskMap.put(name, CompletableFuture.supplyAsync(task, executor));
    }

    public Set<String> getTaskNames(){
        return taskMap.keySet();
    }

    public void resultHandling(String taskName, long timeout, TimeUnit timeUnit, FutureResultProcess<T> resultProcess){
        Future<T> future = taskMap.get(taskName);
        if(future == null){
            throw new EnhanceFutureTaskNotFountException("No task with name {} found.", taskName).printException(logger);
        }
        resultProcess.resultProcess(() -> future.get(timeout,timeUnit));
    }


    public void resultHandling(String taskName, FutureResultProcess<T> resultProcess){
        Future<T> future = taskMap.get(taskName);
        if(future == null){
            throw new EnhanceFutureTaskNotFountException("No task with name {} found.", taskName).printException(logger);
        }
        resultProcess.resultProcess(future::get);
    }

    public T getResult(String taskName, EnhanceFutureExceptionHandler exceptionHandler){
        AtomicReference<T> result = new AtomicReference<>(null);
        resultHandling(taskName, (fs) -> {
            try {
                result.set(fs.getResult());
            } catch (Exception e) {
                exceptionHandler.handleException(e);
            }
        });
        return result.get();
    }

    public T getResult(String taskName){
        EnhanceFutureExceptionHandler exceptionHandler = exceptionHandlerMap.get(taskName);
        exceptionHandler = exceptionHandler != null
                ? exceptionHandler
                : (tx) -> {throw new  FutureGetException(tx, "Exception getting result of task '{}'.", taskName).printException(logger);};
        return getResult(taskName, exceptionHandler);
    }

    public T getResult(String taskName, long timeout, TimeUnit timeUnit, EnhanceFutureExceptionHandler exceptionHandler){
        AtomicReference<T> result = new AtomicReference<>(null);
        resultHandling(taskName, timeout, timeUnit, (fs) -> {
            try {
                result.set(fs.getResult());
            } catch (Exception e) {
                exceptionHandler.handleException(e);
            }
        });
        return result.get();
    }

    public T getResult(String taskName, long timeout, TimeUnit timeUnit){
        EnhanceFutureExceptionHandler exceptionHandler = exceptionHandlerMap.get(taskName);
        exceptionHandler = exceptionHandler != null
                ? exceptionHandler
                : (tx) -> {throw new  FutureGetException(tx, "Exception getting result of task '{}'.", taskName).printException(logger);};
        return getResult(taskName, timeout, timeUnit, exceptionHandler);
    }


    public Map<String, T> getResultMap(){
        Map<String, T> results = new LinkedHashMap<>();
        Set<String> taskNames = getTaskNames();
        for (String taskName : taskNames) {
            results.put(taskName, getResult(taskName));
        }
        return results;
    }

    public Collection<T> getResults(){
        return getResultMap().values();
    }

}
