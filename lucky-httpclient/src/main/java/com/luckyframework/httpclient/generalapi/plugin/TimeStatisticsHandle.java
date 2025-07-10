package com.luckyframework.httpclient.generalapi.plugin;

@FunctionalInterface
public interface TimeStatisticsHandle {

    /**
     * 耗时处理逻辑
     *
     * @param info 耗时信息
     * @throws Exception 统计过程中可能出现的异常
     */
    void handle(TimeStatisticsInfo info) throws Exception;
}
