package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

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
    public List<? extends ParamInfo> parser(DynamicParamContext context) {
        if (context.getContext().isNullValue()) {
            return Collections.emptyList();
        }
        return doParser(context);
    }

    protected abstract List<? extends ParamInfo> doParser(DynamicParamContext context);
}
