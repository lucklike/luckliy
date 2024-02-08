package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.core.BodyObject;
import com.luckyframework.httpclient.core.BodySerialization;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.annotations.BodyParam;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.reflect.ClassUtils;

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

    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        BodyParam bodyParamAnn = context.toAnnotation(BodyParam.class);
        String mimeType = bodyParamAnn.mimeType();
        String charset = bodyParamAnn.charset();
        Class<? extends BodySerialization> serializationClass = bodyParamAnn.serializationClass();
        BodySerialization bodySerialization = ClassUtils.newObject(serializationClass);
        try {
            ValueContext valueContext = context.getContext();
            return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), BodyObject.builder(mimeType, charset, bodySerialization.serialization(valueContext.getValue()))));
        } catch (Exception e) {
            throw new IllegalArgumentException("Request body parameter serialization exception.", e);
        }
    }
}
