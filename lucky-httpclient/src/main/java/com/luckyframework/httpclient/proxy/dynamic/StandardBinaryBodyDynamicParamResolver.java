package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.core.meta.BodyObject;
import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * 标准二进制参数解析器
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/30 02:45
 */
public class StandardBinaryBodyDynamicParamResolver extends AbstractDynamicParamResolver {
    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), BodyObject.byteBody(HttpExecutor.toByte(valueContext.getValue()))));
    }
}
