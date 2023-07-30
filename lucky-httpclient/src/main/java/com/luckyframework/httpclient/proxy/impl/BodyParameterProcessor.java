package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.BodySerialization;
import com.luckyframework.httpclient.proxy.ParameterProcessor;
import com.luckyframework.reflect.ClassUtils;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * 请求体参数初处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/29 17:45
 */
public class BodyParameterProcessor implements ParameterProcessor {

    public static final String MIME_TYPE = "mimeType";
    public static final String CHARSET = "charset";
    public static final String SERIALIZATION_SCHEME_CLASS = "serializationSchemeClass";

    @Override
    @SuppressWarnings("unchecked")
    public BodyObject paramProcess(Object originalParam, Map<String, String> extraParmMap) {
        if (originalParam == null) {
            return null;
        }
        String mimeType = extraParmMap.get(MIME_TYPE);
        Assert.notNull(mimeType, "The necessary additional configuration '" + MIME_TYPE + "' is missing.");

        String charset = extraParmMap.get(CHARSET);
        Assert.notNull(charset, "The necessary additional configuration '" + CHARSET + "' is missing.");

        if (originalParam instanceof String) {
            return BodyObject.builder(mimeType, charset, (String) originalParam);
        }
        String serializationSchemeClassStr = extraParmMap.get(SERIALIZATION_SCHEME_CLASS);
        Assert.notNull(serializationSchemeClassStr, "The necessary additional configuration '" + SERIALIZATION_SCHEME_CLASS + "' is missing.");
        Class<?> serializationSchemeClass = ClassUtils.forName(serializationSchemeClassStr, ClassUtils.getDefaultClassLoader());

        if (BodySerialization.class.isAssignableFrom(serializationSchemeClass)) {
            BodySerialization bodySerialization = ClassUtils.newObject((Class<BodySerialization>) serializationSchemeClass);
            try {
                return BodyObject.builder(mimeType, charset, bodySerialization.serialization(originalParam));
            } catch (Exception e) {
                throw new IllegalArgumentException("Request body parameter serialization exception.", e);
            }
        } else {
            throw new IllegalArgumentException("The 'serializationSchemeClass' configuration value must be the full name of a 'com.luckyframework.httpclient.core.BodySerialization' interface implementation class.");
        }
    }


    @Override
    public boolean needExpansionAnalysis() {
        return false;
    }
}
