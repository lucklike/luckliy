package com.luckyframework.httpclient.proxy.impl.interceptor;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.RequestInterceptor;
import com.luckyframework.httpclient.proxy.ResponseInterceptor;

import java.lang.annotation.Annotation;

/**
 * 请求、响应条件判断
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/12 03:01
 */
public class RequestAndResponseConditionInterceptor implements RequestInterceptor, ResponseInterceptor {

    private final InteriorRequestConditionInterceptor interiorRequestConditionInterceptor;
    private final InteriorResponseConditionInterceptor interiorResponseConditionInterceptor;

    public RequestAndResponseConditionInterceptor() {
        this.interiorRequestConditionInterceptor = new InteriorRequestConditionInterceptor();
        this.interiorResponseConditionInterceptor = new InteriorResponseConditionInterceptor();
    }

    @Override
    public void requestProcess(Request request, MethodContext context, Annotation requestAfterHandleAnn) {
        interiorRequestConditionInterceptor.requestProcess(request, context, requestAfterHandleAnn);
    }

    @Override
    public int requestPriority() {
        return interiorRequestConditionInterceptor.requestPriority();
    }

    @Override
    public void responseProcess(Response response, MethodContext context, Annotation responseInterceptorHandleAnn) {
        interiorResponseConditionInterceptor.responseProcess(response, context, responseInterceptorHandleAnn);
    }

    @Override
    public int responsePriority() {
        return interiorResponseConditionInterceptor.responsePriority();
    }

    static class InteriorResponseConditionInterceptor extends ResponseConditionInterceptor {
        @Override
        protected String getResponseConditionFieldName() {
            return "response";
        }
    }

    static class InteriorRequestConditionInterceptor extends RequestConditionInterceptor {
        @Override
        protected String getRequestConditionFieldName() {
            return "request";
        }
    }
}
