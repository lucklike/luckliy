package com.luckyframework.httpclient.proxy.slow;

import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 慢响应处理器
 */
public interface SlowResponseHandler {


    /**
     * 用于处理危险级别的响应
     *
     * @param context          方法上下文
     * @param responseTimeSpent 危险级别的响应信息
     * @param configSlowTime   用户配置的触发危险的最小响应时间
     */
    default void handleSlowResponse(MethodContext context, ResponseTimeSpent responseTimeSpent, long configSlowTime) {

    }

}
