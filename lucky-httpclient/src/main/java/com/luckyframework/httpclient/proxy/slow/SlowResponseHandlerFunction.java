package com.luckyframework.httpclient.proxy.slow;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.MethodContext;

@FunctionalInterface
public interface SlowResponseHandlerFunction {

    /**
     * 用于处理慢响应的逻辑
     *
     * @param context           方法上下文
     * @param response          响应对象
     * @param responseTimeSpent 响应时间信息
     * @param configSlowTime    用户配置的慢响应时间
     */
    void handleSlowResponse(MethodContext context, Response response, ResponseTimeSpent responseTimeSpent, long configSlowTime);

}
