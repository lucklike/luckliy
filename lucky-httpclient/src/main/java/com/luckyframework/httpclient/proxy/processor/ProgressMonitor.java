package com.luckyframework.httpclient.proxy.processor;

/**
 * 进度监控器
 */
@FunctionalInterface
public interface ProgressMonitor {

    void sniffing(Progress progress);

}
