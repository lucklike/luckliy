package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.httpclient.proxy.context.MethodContext;

/**
 * 慢请求处理器
 */
public interface SlowResponseHandler {


    /**
     * 用于处理警告级别的响应
     *
     * @param context          方法上下文
     * @param warnResponseInfo 警告级别的响应信息
     * @param configWarnTime   用户配置的触发警告的最小响应时间
     */
    void handleWarnResponse(MethodContext context, SlowResponseInfo warnResponseInfo, long configWarnTime);

    /**
     * 用于处理危险级别的响应
     *
     * @param context          方法上下文
     * @param slowResponseInfo 危险级别的响应信息
     * @param configSlowTime   用户配置的触发危险的最小响应时间
     */
    void handleSlowResponse(MethodContext context, SlowResponseInfo slowResponseInfo, long configSlowTime);

}
