package com.luckyframework.threadpool;

import com.luckyframework.reflect.ClassUtils;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * 线程池参数
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/22 11:02
 */
public class ThreadPoolParam {
    /**
     * 核心线程数
     */
    protected int corePoolSize = 10;
    /**
     * 最大线程数
     */
    protected int maximumPoolSize = 15;
    /**
     * 阻塞队列的长度
     */
    protected int blockingQueueSize = 200;
    /**
     * 保活时间，空闲等待时间
     */
    protected long keepAliveTime = 0;
    /**
     * 线程名格式
     */
    protected String nameFormat = "thread-pool-";
    /**
     * 阻塞队列工厂
     */
    protected RunnableBlockingQueueFactory blockingQueueFactory = RunnableBlockingQueueFactory.DEFAULT_INSTANCE;
    /**
     * 拒绝策略工厂
     */
    protected RejectedExecutionHandlerFactory rejectedExecutionHandlerFactory = RejectedExecutionHandlerFactory.DEFAULT_INSTANCE;

    /**
     * 获取核心线程数
     *
     * @return 核心线程数
     */
    public int getCorePoolSize() {
        return corePoolSize;
    }

    /**
     * 设置核心线程数
     *
     * @param corePoolSize 核心线程数
     */
    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    /**
     * 获取最大线程数
     *
     * @return 最大线程数
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    /**
     * 设置最大线程数
     *
     * @param maximumPoolSize 最大线程数
     */
    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    /**
     * 获取阻塞队列的长度
     *
     * @return 阻塞队列的长度
     */
    public int getBlockingQueueSize() {
        return blockingQueueSize;
    }

    /**
     * 设置阻塞队列的长度
     *
     * @param blockingQueueSize 阻塞队列的长度
     */
    public void setBlockingQueueSize(int blockingQueueSize) {
        this.blockingQueueSize = blockingQueueSize;
    }

    /**
     * 获取保活时间
     *
     * @return 保活时间
     */
    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    /**
     * 设置保活时间（空闲等待时间）
     *
     * @param keepAliveTime 保活时间
     */
    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    /**
     * 获取线程名格式
     * @return 线程名格式
     */
    public String getNameFormat() {
        return nameFormat;
    }

    /**
     * 设置线程名格式，默认值：thread-pool-
     * @param nameFormat 线程名格式
     */
    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    /**
     * 获取阻塞队列
     *
     * @return 阻塞队列
     */
    public BlockingDeque<Runnable> getBlockingQueue() {
        return this.blockingQueueFactory.create(this.blockingQueueSize);
    }

    /**
     * 设置用于生成阻塞队列的工厂
     *
     * @param blockingQueueFactoryClass 用于生成阻塞队列的工厂
     */
    public void setBlockingQueueFactory(Class<? extends RunnableBlockingQueueFactory> blockingQueueFactoryClass) {
        this.blockingQueueFactory = ClassUtils.newObject(blockingQueueFactoryClass);
    }

    /**
     * 获取拒绝策略对象
     *
     * @return 拒绝策略对象
     */
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return rejectedExecutionHandlerFactory.create();
    }

    /**
     * 设置用于生成拒绝策略对象的工厂
     *
     * @param rejectedExecutionHandlerFactoryClass 用于生成拒绝策略对象的工厂
     */
    public void setRejectedExecutionHandlerFactory(Class<? extends RejectedExecutionHandlerFactory> rejectedExecutionHandlerFactoryClass) {
        this.rejectedExecutionHandlerFactory = ClassUtils.newObject(rejectedExecutionHandlerFactoryClass);
    }


    @Override
    public String toString() {
        return "ThreadPoolParam{" +
                "corePoolSize=" + corePoolSize +
                ", maximumPoolSize=" + maximumPoolSize +
                ", blockingQueueSize=" + blockingQueueSize +
                ", keepAliveTime=" + keepAliveTime +
                ", nameFormat='" + nameFormat + '\'' +
                ", blockingQueueFactory=" + blockingQueueFactory +
                ", rejectedExecutionHandlerFactory=" + rejectedExecutionHandlerFactory +
                '}';
    }
}
