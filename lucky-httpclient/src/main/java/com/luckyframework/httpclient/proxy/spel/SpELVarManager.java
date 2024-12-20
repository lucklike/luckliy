package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.context.Context;
import org.springframework.lang.NonNull;

/**
 * SpEL变量管理
 */
public interface SpELVarManager {

    /**
     * 设置上下文变量
     */
    void setContextVar();

    /**
     * 获取上下文变量
     *
     * @return 上下文变量
     */
    @NonNull
    SpELVariate getContextVar();

    /**
     * 设置请求变量
     *
     * @param request 请求对象
     */
    void setRequestVar(Request request);

    /**
     * 设置响应变量
     *
     * @param response 响应对象
     * @param context  上下文对象
     */
    void setResponseVar(Response response, Context context);

}
