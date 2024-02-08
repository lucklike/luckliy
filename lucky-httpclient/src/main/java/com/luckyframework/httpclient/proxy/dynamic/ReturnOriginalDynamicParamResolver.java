package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.context.ValueContext;

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
    public List<ParamInfo> parser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), valueContext.getValue()));
    }
}
