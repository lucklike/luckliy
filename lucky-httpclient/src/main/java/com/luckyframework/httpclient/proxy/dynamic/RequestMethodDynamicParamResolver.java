package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.meta.RequestMethod;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;

/**
 * RequestMethod处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:10
 */
public class RequestMethodDynamicParamResolver extends AbstractDynamicParamResolver {

    @Override
    public List<ParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), ConversionUtils.conversion(valueContext.getValue(), RequestMethod.class)));
    }
}
