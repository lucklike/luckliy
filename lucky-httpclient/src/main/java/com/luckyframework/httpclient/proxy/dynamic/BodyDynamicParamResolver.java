package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.serialization.BodySerialization;
import com.luckyframework.httpclient.proxy.annotations.BodyParam;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

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

        // 获取注解属性
        BodyParam bodyParamAnn = context.toAnnotation(BodyParam.class);
        String mimeType = context.parseExpression(bodyParamAnn.mimeType(), String.class);
        String charsetStr = context.parseExpression(bodyParamAnn.charset(), String.class);
        Charset charset = StringUtils.hasText(charsetStr) ? Charset.forName(charsetStr) : null;

        // 获取响应体转换器
        BodySerialization bodySerialization = context.generateObject(bodyParamAnn.serialization());
        ValueContext valueContext = context.getContext();
        Object value = valueContext.getValue();
        try {
            byte[] valueBytes = bodySerialization.serialization(value, charset);
            BodyObject bodyObject = BodyObject.builder(mimeType, charset, valueBytes);
            Supplier<String> stringSupplier = bodySerialization.stringSupplier(value, valueBytes, mimeType, charset);
            if (stringSupplier != null) {
                bodyObject.setStringSupplier(stringSupplier);
            }
            return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), bodyObject));
        } catch (Exception e) {
            throw new IllegalArgumentException("Request body parameter '" + value + "' serialization exception.", e);
        }
    }
}
