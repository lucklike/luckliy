package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.proxy.annotations.RefParam;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * 返回原始参数的动态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 14:30
 */
public class RefDynamicParamResolver implements DynamicParamResolver {

    @Override
    public List<ParamInfo> parser(DynamicParamContext context) {
        RefParam refAnn = context.toAnnotation(RefParam.class);
        Object value = refAnn.prefix() + context.getValue() + refAnn.suffix();
        return Collections.singletonList(new ParamInfo(refAnn.type(), value));
    }
}
