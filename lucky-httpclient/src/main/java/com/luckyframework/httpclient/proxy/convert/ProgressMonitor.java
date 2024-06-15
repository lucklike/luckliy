package com.luckyframework.httpclient.proxy.convert;

/**
 * 进度监控器
 */
@FunctionalInterface
public interface ProgressMonitor {

    /**
     * 开始之前执行
     *
     * @param progress 进度对象
     */
    default void beforeBeginning(Progress progress) {

    }


    /**
     * 进度嗅探
     *
     * @param progress 进度对象
     */
    void sniffing(Progress progress);


    /**
     * 结束之后执行
     *
     * @param progress 进度对象
     */
    default void afterCompleted(Progress progress) {

    }

    /**
     * 失败之后执行
     *
     * @param progress 进度对象
     * @param e        异常
     */
    default void afterFailed(Progress progress, Exception e) throws Exception {
        throw e;
    }
}
