package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.httpclient.proxy.annotations.StaticRef;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * Basic Auth配置解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 12:34
 */
public class RefStaticParamResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        StaticRef refAnn = context.toAnnotation(StaticRef.class);
        String ref = context.parseExpression(refAnn.value());
        return Collections.singletonList(new ParamInfo(refAnn.type(), ref));
    }
}
