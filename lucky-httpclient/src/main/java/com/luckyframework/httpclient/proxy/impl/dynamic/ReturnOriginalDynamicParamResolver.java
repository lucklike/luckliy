package com.luckyframework.httpclient.proxy.impl.dynamic;

import com.luckyframework.httpclient.proxy.DynamicParamResolver;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.ValueContext;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * 返回原始参数的动态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 14:30
 */
public class ReturnOriginalDynamicParamResolver implements DynamicParamResolver {

    @Override
    public List<ParamInfo> parser(ValueContext context, Annotation dynamicParamAnn) {
        return Collections.singletonList(new ParamInfo(getOriginalParamName(context), context.getValue()));
    }
}
