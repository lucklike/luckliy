package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.BodySerialization;
import com.luckyframework.httpclient.proxy.ParameterProcessor;
import com.luckyframework.reflect.AnnotationUtils;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.core.annotation.MergedAnnotation;

import java.lang.annotation.Annotation;

/**
 * 请求体参数初处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/29 17:45
 */
public class BodyParameterProcessor implements ParameterProcessor {

    @Override
    @SuppressWarnings("unchecked")
    public BodyObject paramProcess(Object originalParam, Annotation proxyHttpParamAnn) {
        if (originalParam == null) {
            return null;
        }
        MergedAnnotation<?> mergedAnnotation = AnnotationUtils.getSpringRootMergedAnnotation(proxyHttpParamAnn);
        String mimeType = mergedAnnotation.getString("mimeType");
        String charset = mergedAnnotation.getString("charset");
        Class<BodySerialization> serializationClass = (Class<BodySerialization>) mergedAnnotation.getClass("serializationClass");
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
