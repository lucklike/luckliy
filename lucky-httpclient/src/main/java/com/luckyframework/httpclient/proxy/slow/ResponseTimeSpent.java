package com.luckyframework.httpclient.proxy.slow;

/**
 * 响应耗时信息
 */
public class ResponseTimeSpent {


    /**
     * 请求开始时间
     */
    private final long startTime;

    /**
     * 请求结束时间
     */
    private final long endTime;

    /**
     * 执行时间
     */
    private final long exeTime;


    public ResponseTimeSpent(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.exeTime = endTime - startTime;
    }


    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getExeTime() {
        return exeTime;
    }

}
