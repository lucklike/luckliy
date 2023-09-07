package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.proxy.ParameterProcessor;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 不做任何处理的参数处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 10:16
 */
public class NotProcessor implements ParameterProcessor {

    @Override
    public Object paramProcess(Object originalParam, Annotation dynamicParamAnn) {
        return originalParam;
    }
}
