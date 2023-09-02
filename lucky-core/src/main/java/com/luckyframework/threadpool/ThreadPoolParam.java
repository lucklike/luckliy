package com.luckyframework.threadpool;

import com.luckyframework.reflect.ClassUtils;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;

/**
 * 线程池参数
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/22 11:02
 */
public class ThreadPoolParam {
    /** 核心线程数*/
    protected int                               corePoolSize                        = 10;
    /** 最大线程数*/
    protected int                               maximumPoolSize                     = 15;
    /** 阻塞队列的长度*/
    protected int                               blockingQueueSize                   = 200;
    /** 保活时间，空闲等待时间*/
    protected long                              keepAliveTime                       = 0;
    /** 线程名格式*/
    protected String                            nameFormat                          = "thread-pool-";
    /** 阻塞队列工厂*/
    protected RunnableBlockingQueueFactory      blockingQueueFactory                = RunnableBlockingQueueFactory.DEFAULT_INSTANCE;
    /** 拒绝策略工厂*/
    protected RejectedExecutionHandlerFactory   rejectedExecutionHandlerFactory     = RejectedExecutionHandlerFactory.DEFAULT_INSTANCE;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getBlockingQueueSize() {
        return blockingQueueSize;
    }

    public void setBlockingQueueSize(int blockingQueueSize) {
        this.blockingQueueSize = blockingQueueSize;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public String getNameFormat() {
        return nameFormat;
    }

    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    public BlockingDeque<Runnable> getBlockingQueue() {
        return this.blockingQueueFactory.create(this.blockingQueueSize);
    }

    public void setBlockingQueueFactory(Class<? extends RunnableBlockingQueueFactory> blockingQueueFactoryClass) {
       this.blockingQueueFactory = ClassUtils.newObject(blockingQueueFactoryClass);
    }

    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return rejectedExecutionHandlerFactory.create();
    }

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
