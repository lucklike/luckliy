package com.luckyframework.httpclient.proxy.impl.dynamic;

import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.ValueContext;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

import static com.luckyframework.httpclient.proxy.impl.dynamic.URLEncoderUtils.CHARSET;
import static com.luckyframework.httpclient.proxy.impl.dynamic.URLEncoderUtils.encode;

/**
 * 支持URL编码处理的参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:04
 */
public class URLEncoderDynamicParamResolver extends AbstractDynamicParamResolver {


    @Override
    public List<ParamInfo> doParser(ValueContext context, Annotation dynamicParamAnn) {
        String charset = context.getAnnotationAttribute(dynamicParamAnn, CHARSET, String.class);
        return Collections.singletonList(new ParamInfo(
                getOriginalParamName(context),
                encode(context.getValue(), charset)
        ));
    }
}
