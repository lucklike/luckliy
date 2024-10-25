package com.luckyframework.threadpool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 有提交限制的线程池
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/22 10:29
 */
public class LimitedSubmitThreadPoolExecutor extends ThreadPoolExecutor {

    private final static Logger logger = LoggerFactory.getLogger(LimitedSubmitThreadPoolExecutor.class);

    /** 提交限制，当队列中的任务达到该限制时会对'提交操作'进行限制*/
    private int limitedSubmitCount = -1;
    /** 提交等待时间，当队列中的任务达到限制时，提交操作将进入等待状态，该值为等待时长*/
    private long submitWaitTime = 1000L;
    /** 提示信息*/
    private String submitWaitPromptName;

    public LimitedSubmitThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public LimitedSubmitThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public LimitedSubmitThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public LimitedSubmitThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    public int getLimitedSubmitCount() {
        return limitedSubmitCount;
    }

    public void setLimitedSubmitCount(int limitedSubmitCount) {
        this.limitedSubmitCount = limitedSubmitCount;
    }

    public long getSubmitWaitTime() {
        return submitWaitTime;
    }

    public void setSubmitWaitTime(long submitWaitTime) {
        this.submitWaitTime = submitWaitTime;
    }

    public String getSubmitWaitPromptName() {
        return submitWaitPromptName;
    }

    public void setSubmitWaitPromptName(String submitWaitPromptName) {
        this.submitWaitPromptName = submitWaitPromptName;
    }

    @Override
    public void execute(Runnable command) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        super.execute(command);
    }

    public void execute(Runnable command, int limitedSubmitCount, long submitWaitTime) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        super.execute(command);
    }

    public void execute(Runnable command, int limitedSubmitCount) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        super.execute(command);
    }

    @Override
    public Future<?> submit(Runnable task) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        return super.submit(task);
    }

    public Future<?> submit(Runnable task, int limitedSubmitCount, long submitWaitTime) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        return super.submit(task);
    }

    public Future<?> submit(Runnable task, int limitedSubmitCount) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        return super.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        return super.submit(task, result);
    }

    public <T> Future<T> submit(Runnable task, T result, int limitedSubmitCount, long submitWaitTime) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        return super.submit(task, result);
    }

    public <T> Future<T> submit(Runnable task, T result, int limitedSubmitCount) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        return super.submit(task, result);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        return super.submit(task);
    }

    public <T> Future<T> submit(Callable<T> task, int limitedSubmitCount, long submitWaitTime) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        return super.submit(task);
    }

    public <T> Future<T> submit(Callable<T> task, int limitedSubmitCount) {
        submitStatusCheck(submitWaitPromptName, limitedSubmitCount, submitWaitTime);
        return super.submit(task);
    }

    /**
     * 提交状态校验，当队列中任务的数量达到限制时会进入等待状态，
     * 直到队列中的任务数量下降至限制数量以下
     *
     * @param promptName            提示信息，用于日志中
     * @param limitedSubmitCount    禁止提交的最大队列长度
     */
    private void submitStatusCheck(String promptName, int limitedSubmitCount, long submitWaitTime){
        if(-1 == limitedSubmitCount){
            return;
        }

        promptName = promptName == null ? this.submitWaitPromptName : promptName;

        while (getQueue().size() >= limitedSubmitCount){
            logger.info("[{}] The queue length '{}' in the current thread pool has reached the submission limit '{}', and it starts to enter the waiting state...", promptName, getQueue().size(), limitedSubmitCount);
            try {
                Thread.sleep(submitWaitTime);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
