package com.luckyframework.httpclient.proxy.impl.dynamic;

import com.luckyframework.httpclient.proxy.DynamicParamResolver;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.ValueContext;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * 基本的动态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/10/1 07:23
 */
public abstract class AbstractDynamicParamResolver implements DynamicParamResolver {
    @Override
    public List<? extends ParamInfo> parser(ValueContext context, Annotation dynamicParamAnn) {
        if (context.isNullValue()) {
            return Collections.emptyList();
        }
        return doParser(context, dynamicParamAnn);
    }

    protected abstract List<? extends ParamInfo> doParser(ValueContext context, Annotation dynamicParamAnn);
}
