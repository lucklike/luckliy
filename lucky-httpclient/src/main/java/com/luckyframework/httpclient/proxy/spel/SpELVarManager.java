package com.luckyframework.httpclient.proxy.spel;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.context.Context;
import org.springframework.lang.NonNull;

/**
 * SpEL变量管理
 */
public interface SpELVarManager {

    /**
     * 获取全局变量
     *
     * @return 全局变量
     */
    @NonNull
    MapRootParamWrapper getGlobalVar();

    /**
     * 设置上下文变量
     *
     */
    void setContextVar();

    /**
     * 获取上下文变量
     *
     * @return 上下文变量
     */
    @NonNull
    MapRootParamWrapper getContextVar();

    /**
     * 设置请求变量
     *
     * @param request 请求对象
     */
    void setRequestVar(Request request);

    /**
     * 获取请求变量
     *
     * @return 请求变量
     */
    @NonNull
    MapRootParamWrapper getRequestVar();

    /**
     * 设置Void类型响应变量
     *
     * @param voidResponse Void类型响应对象
     * @param context 上下文对象
     */
    void setVoidResponseVar(VoidResponse voidResponse, Context context);

    /**
     * 获取Void类型响应变量
     *
     * @return Void类型响应变量
     */
    @NonNull
    MapRootParamWrapper getVoidResponseVar();

    /**
     * 设置响应变量
     *
     * @param response 响应对象
     * @param context 上下文对象
     */
    void setResponseVar(Response response, Context context);

    /**
     * 获取响应变量
     *
     * @return 响应变量
     */
    @NonNull
    MapRootParamWrapper getResponseVar();

    /**
     * 获取最终生成的变量
     *
     * @return 最终生成的变量
     */
    @NonNull
    MapRootParamWrapper getFinallyVar();

}
