package com.luckyframework.httpclient.proxy.impl.dynamic;

import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.BodySerialization;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.ValueContext;
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
    public List<ParamInfo> doParser(ValueContext context, Annotation dynamicParamAnn) {
        String mimeType = context.getAnnotationAttribute(dynamicParamAnn, MIME_TYPE, String.class);
        String charset = context.getAnnotationAttribute(dynamicParamAnn, CHARSET, String.class);
        Class<BodySerialization> serializationClass = (Class<BodySerialization>) context.getAnnotationAttribute(dynamicParamAnn, SERIALIZATION_CLASS);
        BodySerialization serializationScheme = ClassUtils.newObject(serializationClass);
        try {
            return Collections.singletonList(new ParamInfo(getOriginalParamName(context), BodyObject.builder(mimeType, charset, serializationScheme.serialization(context.getValue()))));
        } catch (Exception e) {
            throw new IllegalArgumentException("Request body parameter serialization exception.", e);
        }
    }
}
