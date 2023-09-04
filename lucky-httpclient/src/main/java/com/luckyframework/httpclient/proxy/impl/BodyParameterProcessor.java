package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.BodySerialization;
import com.luckyframework.httpclient.proxy.ParameterProcessor;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;

import java.lang.annotation.Annotation;

/**
 * 请求体参数初处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/29 17:45
 */
public class BodyParameterProcessor implements ParameterProcessor {

    private static final String MIME_TYPE = "mimeType";
    private static final String CHARSET = "charset";
    private static final String SERIALIZATION_CLASS = "serializationClass";

    @Override
    @SuppressWarnings("unchecked")
    public BodyObject paramProcess(Object originalParam, Annotation dynamicParamAnn) {
        if (originalParam == null) {
            return null;
        }
        String mimeType = AnnotationUtils.getValue(dynamicParamAnn, MIME_TYPE, String.class);
        String charset = AnnotationUtils.getValue(dynamicParamAnn, CHARSET, String.class);
        Class<BodySerialization> serializationClass = (Class<BodySerialization>) AnnotationUtils.getValue(dynamicParamAnn, SERIALIZATION_CLASS);
        BodySerialization serializationScheme = ClassUtils.newObject(serializationClass);
        try {
            return BodyObject.builder(mimeType, charset, serializationScheme.serialization(originalParam));
        } catch (Exception e) {
            throw new IllegalArgumentException("Request body parameter serialization exception.", e);
        }
    }


    @Override
    public boolean needExpansionAnalysis() {
        return false;
    }
}
