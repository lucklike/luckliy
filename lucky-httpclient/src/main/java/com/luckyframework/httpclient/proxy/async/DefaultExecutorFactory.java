package com.luckyframework.httpclient.proxy.async;

import com.luckyframework.threadpool.ThreadPoolFactory;
import com.luckyframework.threadpool.ThreadPoolParam;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 默认的线程池工厂
 */
public class DefaultExecutorFactory {

    // 获取服务器CPU核心数
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    public static Executor getDefaultExecutor() {
        ThreadPoolParam poolParam = new ThreadPoolParam();
        poolParam.setCorePoolSize(CPU_CORES * 2);
        poolParam.setMaximumPoolSize(CPU_CORES * 5);
        poolParam.setKeepAliveTime(60L);
        poolParam.setTimeUnit(TimeUnit.SECONDS);
        poolParam.setBlockingQueueSize(200);
        poolParam.setNameFormat("http-task-");
        return ThreadPoolFactory.createThreadPool(poolParam);
    }

}
