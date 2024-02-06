package com.luckyframework.httpclient.proxy.interceptor;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.core.VoidResponse;

/**
 * 重定向规则
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/6 22:58
 */
public interface RedirectRule {

    /**
     * 是否需要重定向，void方法时
     *
     * @param voidResponse Void方法的返回结果
     * @param context 拦截器上下文
     * @return 是否需要重定向
     */
    boolean isRedirect(VoidResponse voidResponse, InterceptorContext context);

    /**
     * 重定向请求实例
     *
     * @param voidResponse Void方法的返回结果
     * @param context 拦截器上下文
     * @return 重定向请求实例
     */
    Request getRedirectRequest(VoidResponse voidResponse, InterceptorContext context);

    /**
     * 是否需要重定向，非void方法时
     *
     * @param response 非Void方法的返回结果
     * @param context 拦截器上下文
     * @return 是否需要重定向
     */
    boolean isRedirect(Response response, InterceptorContext context);


    /**
     * 重定向请求实例
     *
     * @param response 非Void方法的返回结果
     * @param context 拦截器上下文
     * @return 重定向请求实例
     */
    Request getRedirectRequest(Response response, InterceptorContext context);

}
