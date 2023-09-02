package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.httpclient.proxy.ParameterProcessor;
import com.luckyframework.reflect.AnnotationUtils;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;

/**
 * URL编码处理的参数处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 10:16
 */
public class URLEncoderParameterProcessor implements ParameterProcessor {

    private static final String CHARSET = "charset";

    @Override
    public String paramProcess(Object originalParam, Annotation dynamicParamAnn) {
        if (originalParam == null) {
            return null;
        }
        String charset = AnnotationUtils.getValue(dynamicParamAnn, CHARSET, String.class);
        try {
            return URLEncoder.encode(String.valueOf(originalParam), charset);
        } catch (UnsupportedEncodingException e) {
            throw new HttpExecutorException("url encoding(" + charset + ") exception: value='" + originalParam + "'", e);
        }
    }
}
