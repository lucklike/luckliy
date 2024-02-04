package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.BodySerialization;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.reflect.ClassUtils;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * 请求体参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:13
 */
public class BodyDynamicParamResolver extends AbstractDynamicParamResolver {

    private static final String MIME_TYPE = "mimeType";
    private static final String CHARSET = "charset";
    private static final String SERIALIZATION_CLASS = "serializationClass";

    @Override
    @SuppressWarnings("unchecked")
    public List<ParamInfo> doParser(DynamicParamContext context) {
        String mimeType = context.getAnnotationAttribute(MIME_TYPE, String.class);
        String charset = context.getAnnotationAttribute(CHARSET, String.class);
        Class<BodySerialization> serializationClass = (Class<BodySerialization>) context.getAnnotationAttribute(SERIALIZATION_CLASS);
        BodySerialization serializationScheme = ClassUtils.newObject(serializationClass);
        try {
            ValueContext valueContext = context.getContext();
            return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), BodyObject.builder(mimeType, charset, serializationScheme.serialization(valueContext.getValue()))));
        } catch (Exception e) {
            throw new IllegalArgumentException("Request body parameter serialization exception.", e);
        }
    }
}
