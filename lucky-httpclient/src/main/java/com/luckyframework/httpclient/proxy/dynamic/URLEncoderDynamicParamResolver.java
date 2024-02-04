package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.context.ValueContext;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * 支持URL编码处理的参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:04
 */
public class URLEncoderDynamicParamResolver extends AbstractDynamicParamResolver {


    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        String charset = context.getAnnotationAttribute(URLEncoderUtils.CHARSET, String.class);
        ValueContext valueContext = context.getContext();
        return Collections.singletonList(new ParamInfo(
                getOriginalParamName(valueContext),
                URLEncoderUtils.encode(valueContext.getValue(), charset)
        ));
    }
}
