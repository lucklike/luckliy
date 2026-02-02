package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;

/**
 * 慢响应信息
 */
public class SlowResponseInfo {

    /**
     * 请求的唯一ID
     */
    private final String uniqueId;

    /**
     * 响应对象
     */
    private final Response response;

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


    public SlowResponseInfo(String uniqueId, Response response, long startTime, long endTime, long exeTime) {
        this.uniqueId = uniqueId;
        this.response = response;
        this.startTime = startTime;
        this.endTime = endTime;
        this.exeTime = exeTime;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Response getResponse() {
        return response;
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

    public Request getRequest() {
        return response.getRequest();
    }
}
