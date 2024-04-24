package com.luckyframework.httpclient.proxy.context;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;
import com.luckyframework.httpclient.proxy.spel.MapRootParamWrapper;
import org.springframework.lang.NonNull;

/**
 * SpEL变量管理
 */
public interface SpELVarManager {

    /**
     * 设置全局变量
     *
     * @param globalVar 全局变量
     */
    void setGlobalVar(MapRootParamWrapper globalVar);

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
     */
    void setVoidResponseVar(VoidResponse voidResponse);

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
     * @param metaType 响应转换元类型
     */
    void setResponseVar(Response response, Class<?> metaType);

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
    default MapRootParamWrapper getFinallyVar() {
        MapRootParamWrapper finalVar = new MapRootParamWrapper();
        finalVar.mergeVar(getGlobalVar());
        finalVar.mergeVar(getContextVar());
        finalVar.mergeVar(getRequestVar());
        finalVar.mergeVar(getVoidResponseVar());
        finalVar.mergeVar(getResponseVar());
        return finalVar;
    }

}
