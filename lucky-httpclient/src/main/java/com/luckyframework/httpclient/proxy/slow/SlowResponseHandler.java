package com.luckyframework.httpclient.proxy.slow;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 慢响应处理器
 */
public interface SlowResponseHandler {


    /**
     * 用于处理慢响应的逻辑
     *
     * @param context           方法上下文
     * @param response          响应对象
     * @param responseTimeSpent 响应时间信息
     */
    void handleSlowResponse(MethodContext context, Response response, ResponseTimeSpent responseTimeSpent);

    /**
     * 定义慢响应时间
     *
     * @param context 方法上下文
     * @return 慢响应时间
     */
    long getSlowTime(MethodContext context);

    /**
     * 当前响应是否为慢响应
     *
     * @param context           方法上下文
     * @param responseTimeSpent 响应时间信息
     * @return 否为慢响应
     */
    default boolean isSlowResponse(MethodContext context, ResponseTimeSpent responseTimeSpent) {
        long slowTime = getSlowTime(context);
        return slowTime > 0 && slowTime <= responseTimeSpent.getExeTime();
    }

}
