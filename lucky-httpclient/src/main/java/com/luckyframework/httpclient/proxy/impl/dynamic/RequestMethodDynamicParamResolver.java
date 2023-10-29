package com.luckyframework.httpclient.proxy.impl.dynamic;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.RequestMethod;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.ValueContext;

import java.lang.annotation.Annotation;
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
    public List<ParamInfo> doParser(ValueContext context, Annotation dynamicParamAnn) {
        return Collections.singletonList(new ParamInfo(getOriginalParamName(context), ConversionUtils.conversion(context.getValue(), RequestMethod.class)));
    }
}
