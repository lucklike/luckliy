package com.luckyframework.httpclient.proxy;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.annotations.RequestInterceptorHandle;

import java.lang.annotation.Annotation;

/**
 * 请求拦截器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/23 23:41
 */
@FunctionalInterface
public interface RequestInterceptor extends SupportSpELImport {

    /**
     * 请求处理，在执行请求之前对请求进行最后的设置
     *
     * @param request               请求实例
     * @param context               方法上下文
     * @param requestAfterHandleAnn 当前{@link RequestInterceptorHandle}注解实例
     */
    void requestProcess(Request request, MethodContext context, Annotation requestAfterHandleAnn);

    /**
     * 优先级，数值越高优先级越低
     */
    default int requestPriority() {
        return Integer.MAX_VALUE;
    }
}
