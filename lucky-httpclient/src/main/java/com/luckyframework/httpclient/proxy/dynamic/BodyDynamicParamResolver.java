package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.serialization.BodySerialization;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.annotations.BodyParam;
import com.luckyframework.httpclient.proxy.context.ValueContext;

import java.nio.charset.Charset;
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
        String mimeType = context.parseExpression(bodyParamAnn.mimeType(), String.class);
        String charsetStr = context.parseExpression(bodyParamAnn.charset(), String.class);
        Charset charset = StringUtils.hasText(charsetStr) ? Charset.forName(charsetStr) : null;
        BodySerialization bodySerialization = context.generateObject(bodyParamAnn.serialization());
        ValueContext valueContext = context.getContext();
        try {
            return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), BodyObject.builder(mimeType, charset, bodySerialization.serialization(valueContext.getValue(), charset))));
        } catch (Exception e) {
            throw new IllegalArgumentException("Request body parameter '" + valueContext.getValue() + "' serialization exception.", e);
        }
    }
}
