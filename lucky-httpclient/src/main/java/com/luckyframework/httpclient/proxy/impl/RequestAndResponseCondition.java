package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.core.Response;
import com.luckyframework.httpclient.proxy.RequestAfterProcessor;
import com.luckyframework.httpclient.proxy.ResponseAfterProcessor;

import java.lang.annotation.Annotation;

/**
 * 请求、响应条件判断
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/12 03:01
 */
public class RequestAndResponseCondition implements RequestAfterProcessor,  ResponseAfterProcessor {

    private final InteriorRequestCondition interiorRequestCondition;
    private final InteriorResponseCondition interiorResponseCondition;

    public RequestAndResponseCondition() {
        this.interiorRequestCondition = new InteriorRequestCondition();
        this.interiorResponseCondition = new InteriorResponseCondition();
    }

    @Override
    public void requestProcess(Request request, Annotation requestAfterHandleAnn) {
        interiorRequestCondition.requestProcess(request, requestAfterHandleAnn);
    }

    @Override
    public void responseProcess(Response response, Annotation responseAfterHandleAnn) {
        interiorResponseCondition.responseProcess(response, responseAfterHandleAnn);
    }


    static class InteriorResponseCondition extends ResponseCondition {
        @Override
        protected String getResponseConditionFieldName() {
            return "response";
        }
    }

    static class InteriorRequestCondition extends RequestCondition {
        @Override
        protected String getRequestConditionFieldName() {
            return "request";
        }
    }
}
