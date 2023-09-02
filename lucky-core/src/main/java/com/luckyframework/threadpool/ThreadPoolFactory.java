package com.luckyframework.threadpool;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池工厂
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/22 11:25
 */
public abstract class ThreadPoolFactory {

    /**
     * 创建一个有提交限制的线程池{@link LimitedSubmitThreadPoolExecutor}
     * @param poolParam 线程池参数
     * @return 有提交限制的线程池
     */
    public static LimitedSubmitThreadPoolExecutor createLimitedSubmitThreadPool(LimitedSubmitThreadPoolParam poolParam){
        LimitedSubmitThreadPoolExecutor threadPool = new LimitedSubmitThreadPoolExecutor(
                poolParam.corePoolSize,
                poolParam.getMaximumPoolSize(),
                poolParam.getKeepAliveTime(),
                TimeUnit.MILLISECONDS,
                poolParam.getBlockingQueue(),
                new NamedThreadFactory(poolParam.getNameFormat()),
                poolParam.getRejectedExecutionHandler()
        );
        threadPool.setLimitedSubmitCount(poolParam.getLimitedSubmitCount());
        threadPool.setSubmitWaitTime(poolParam.getSubmitWaitTime());
        threadPool.setSubmitWaitPromptName(poolParam.getSubmitWaitPromptName());
        return threadPool;
    }

    /**
     * 创建一个线程池{@link ThreadPoolExecutor}实例
     * @param poolParam 线程池参数
     * @return ThreadPoolExecutor线程池实例
     */
    public static ThreadPoolExecutor createThreadPool(ThreadPoolParam poolParam){
        return new ThreadPoolExecutor(
                poolParam.getCorePoolSize(),
                poolParam.getMaximumPoolSize(),
                poolParam.getKeepAliveTime(),
                TimeUnit.MILLISECONDS,
                poolParam.getBlockingQueue(),
                new NamedThreadFactory(poolParam.getNameFormat()),
                poolParam.getRejectedExecutionHandler()
        );
    }
}
