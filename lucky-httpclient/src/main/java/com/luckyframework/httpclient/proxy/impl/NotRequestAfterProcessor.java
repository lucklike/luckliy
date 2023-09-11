package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.core.Request;
import com.luckyframework.httpclient.proxy.RequestAfterProcessor;

import java.lang.annotation.Annotation;

/**
 * 不做任何处理的请求预处理类
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 04:38
 */
public class NotRequestAfterProcessor implements RequestAfterProcessor {

    @Override
    public void requestProcess(Request request, Annotation requestAfterHandleAnn) {

    }
}
