package com.luckyframework.async;

import com.luckyframework.exception.ExecutorServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 超时处理器
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/27 16:22
 */
public class Timeout {

    private final static Logger logger = LoggerFactory.getLogger(Timeout.class);

    /**
     * 执行一个任务并指定一个超时时间，当任务执行超时时将直接抛出{@link TimeoutException}
     *
     * @param task     任务
     * @param timeout  超时时间
     * @param timeUnit 超时时间的单位
     * @param <T>      结果泛型
     * @return 任务执行结果
     * @throws TimeoutException 当任务执行超时时将直接抛出该异常
     */
    public <T> T timeoutThrowException(Callable<T> task, long timeout, TimeUnit timeUnit) throws TimeoutException {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        Future<T> future = null;
        try {
            future = threadPool.submit(task);
            return future.get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException e) {
            throw new ExecutorServiceException(e).printException(logger);
        } catch (TimeoutException te) {
            future.cancel(true);
            throw te;
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * 执行一个任务并指定一个以毫秒为单位的超时时间，当任务执行超时时将直接抛出{@link TimeoutException}
     *
     * @param task          任务
     * @param timeoutMillis 超时时间(单位:毫秒)
     * @param <T>           结果泛型
     * @return 任务执行结果
     * @throws TimeoutException 当任务执行超时时将直接抛出该异常
     */
    public <T> T timeoutThrowException(Callable<T> task, long timeoutMillis) throws TimeoutException {
        return timeoutThrowException(task, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 执行一个任务并指定一个超时时间和超时返回结果，当任务执行超时时会直接返回指定的结果
     *
     * @param task         任务
     * @param timeoutValue 超时后返回的值
     * @param timeout      超时时间
     * @param timeUnit     超时时间的单位
     * @param <T>          结果泛型
     * @return 任务执行结果
     */
    public <T> T timeoutReturnValue(Callable<T> task, T timeoutValue, long timeout, TimeUnit timeUnit) {
        try {
            return timeoutThrowException(task, timeout, timeUnit);
        } catch (TimeoutException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("If the execution time of the original task exceeds the given maximum timeout time {}({}), it will return to the default timeout value set.", timeout, timeUnit);
            }
            return timeoutValue;
        }
    }

    /**
     * 执行一个任务并指定一个以毫秒为单位的超时时间和超时返回结果，当任务执行超时时会直接返回指定的结果
     *
     * @param task          任务
     * @param timeoutValue  超时后返回的值
     * @param timeoutMillis 超时时间(单位:毫秒)
     * @param <T>           结果泛型
     * @return 任务执行结果
     */
    public <T> T timeoutReturnValue(Callable<T> task, T timeoutValue, long timeoutMillis) {
        return timeoutReturnValue(task, timeoutValue, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 执行一个任务并指定超时时间以及一个超时后要执行的{@link Function},当任务执行超时时会执行设置的{@link Function}返回结果
     *
     * @param task            任务
     * @param timeoutFunction 超时后执行的替代逻辑
     * @param timeout         超时时间
     * @param timeUnit        超时时间的单位
     * @param <T>             结果泛型
     * @return 任务执行结果
     */
    public <T> T timeoutExecuteFunction(Callable<T> task, Function<TimeoutException, T> timeoutFunction, long timeout, TimeUnit timeUnit) {
        try {
            return timeoutThrowException(task, timeout, timeUnit);
        } catch (TimeoutException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("The execution time of the original task exceeds the given maximum timeout time {}({}), and the timeout logic starts to be executed.", timeout, timeUnit);
            }
            return timeoutFunction.apply(e);
        }
    }

    /**
     * 执行一个任务并指定一个以毫秒为单位的超时时间以及一个超时后要执行的{@link Function},当任务执行超时时会执行设置的{@link Function}返回结果
     *
     * @param task            任务
     * @param timeoutFunction 超时后执行的替代逻辑
     * @param timeoutMillis   超时时间(单位:毫秒)
     * @param <T>             结果泛型
     * @return 任务执行结果
     */
    public <T> T timeoutExecuteFunction(Callable<T> task, Function<TimeoutException, T> timeoutFunction, long timeoutMillis) {
        return timeoutExecuteFunction(task, timeoutFunction, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 执行一个任务并指定一个超时时间，当任务执行超时时将直接抛出{@link TimeoutException}
     *
     * @param task     任务
     * @param timeout  超时时间
     * @param timeUnit 超时时间的单位
     * @throws TimeoutException 当任务执行超时时将直接抛出该异常
     */
    public void execute(Runnable task, long timeout, TimeUnit timeUnit) throws TimeoutException {
        ExecutorService threadPool = Executors.newFixedThreadPool(1);
        Future<?> future = null;
        try {
            future = threadPool.submit(task);
            future.get(timeout, timeUnit);
        } catch (InterruptedException | ExecutionException e) {
            throw new ExecutorServiceException(e).printException(logger);
        } catch (TimeoutException te) {
            future.cancel(true);
            throw te;
        } finally {
            threadPool.shutdown();
        }
    }

    /**
     * 执行一个任务并指定一个以毫秒为单位的超时时间，当任务执行超时时将直接抛出{@link TimeoutException}
     *
     * @param task          任务
     * @param timeoutMillis 超时时间(单位:毫秒)
     * @throws TimeoutException 当任务执行超时时将直接抛出该异常
     */
    public void execute(Runnable task, long timeoutMillis) throws TimeoutException {
        execute(task, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 执行一个任务并指定一个超时时间和超时后的异常处理器{@link Consumer},当任务执行超时时会执行设置的{@link Consumer}来处理异常结果
     *
     * @param task           任务
     * @param timeoutHandler 超时异常处理器
     * @param timeout        超时时间
     * @param timeUnit       超时时间的单位
     */
    public void execute(Runnable task, Consumer<TimeoutException> timeoutHandler, long timeout, TimeUnit timeUnit) {
        try {
            execute(task, timeout, timeUnit);
        } catch (TimeoutException e) {
            timeoutHandler.accept(e);
        }
    }

    /**
     * 执行一个任务并指定一个以毫秒为单位的超时时间和超时后的异常处理器{@link Consumer},当任务执行超时时会执行设置的{@link Consumer}来处理异常结果
     *
     * @param task           任务
     * @param timeoutHandler 超时异常处理器
     * @param timeoutMillis  超时时间(单位:毫秒)
     */
    public void execute(Runnable task, Consumer<TimeoutException> timeoutHandler, long timeoutMillis) {
        execute(task, timeoutHandler, timeoutMillis, TimeUnit.MILLISECONDS);
    }

}
