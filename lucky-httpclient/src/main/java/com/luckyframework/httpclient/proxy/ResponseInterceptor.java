package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.annotations.ResponseInterceptorHandle;

import java.lang.annotation.Annotation;

/**
 * 响应拦截器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/23 23:41
 */
@FunctionalInterface
public interface ResponseInterceptor extends SupportSpELImport {

    /**
     * 获取到响应结果之后执行
     *
     * @param response                     响应
     * @param context                      方法上下文
     * @param responseInterceptorHandleAnn 当前{@link ResponseInterceptorHandle}注解实例
     */
    void responseProcess(Response response, MethodContext context, Annotation responseInterceptorHandleAnn);


    /**
     * 优先级，数值越高优先级越低
     */
    default int responsePriority() {
        return Integer.MAX_VALUE;
    }
}
