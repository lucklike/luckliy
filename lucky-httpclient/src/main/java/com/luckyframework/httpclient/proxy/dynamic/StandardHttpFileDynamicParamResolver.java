package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.core.executor.HttpExecutor;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.context.ValueContext;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * 标准资源参数解析器
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/30 02:45
 */
public class StandardHttpFileDynamicParamResolver extends AbstractDynamicParamResolver {
    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), HttpExecutor.toHttpFiles(valueContext.getValue())));
    }
}
