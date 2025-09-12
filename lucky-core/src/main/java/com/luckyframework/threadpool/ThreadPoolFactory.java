package com.luckyframework.threadpool;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池工厂
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/22 11:25
 */
public abstract class ThreadPoolFactory {

    // 获取服务器CPU核心数
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * 创建适用于IO密集型任务的线程池
     *
     * @param nameFormat 线程池名称前缀
     * @return 适用于IO密集型任务的线程池
     */
    public static ThreadPoolExecutor createCPUIntensiveThreadPool(String nameFormat) {
        return new ThreadPoolExecutor(
                CPU_CORES,
                CPU_CORES,
                0L,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                new NamedThreadFactory(nameFormat),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }


    /**
     * 创建适用于IO密集型任务的线程池
     *
     * @param nameFormat 线程池名称前缀
     * @return 适用于IO密集型任务的线程池
     */
    public static ThreadPoolExecutor createIOIntensiveThreadPool(String nameFormat) {
        return new ThreadPoolExecutor(
                CPU_CORES * 2,
                CPU_CORES * 5,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(200),
                new NamedThreadFactory(nameFormat),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 创建一个线程池{@link ThreadPoolExecutor}实例
     *
     * @param poolParam 线程池参数
     * @return ThreadPoolExecutor线程池实例
     */
    public static ThreadPoolExecutor createThreadPool(ThreadPoolParam poolParam) {
        return new ThreadPoolExecutor(
                poolParam.getCorePoolSize(),
                poolParam.getMaximumPoolSize(),
                poolParam.getKeepAliveTime(),
                poolParam.getTimeUnit(),
                poolParam.getBlockingQueue(),
                new NamedThreadFactory(poolParam.getNameFormat()),
                poolParam.getRejectedExecutionHandler()
        );
    }

    /**
     * 创建一个固定线程数的线程池{@link ThreadPoolExecutor}实例
     *
     * @param threadSize    线程数
     * @param threadFactory 线程工程
     * @return 固定线程数的线程池实例
     */
    public static ThreadPoolExecutor createFixedThreadPool(int threadSize, ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(
                threadSize,
                threadSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(0),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 创建一个固定线程数的线程池{@link ThreadPoolExecutor}实例
     *
     * @param threadSize 线程数
     * @param nameFormat 线程池名称前缀
     * @return 固定线程数的线程池实例
     */
    public static ThreadPoolExecutor createFixedThreadPool(int threadSize, String nameFormat) {
        return createFixedThreadPool(threadSize, new NamedThreadFactory(nameFormat));
    }
}
