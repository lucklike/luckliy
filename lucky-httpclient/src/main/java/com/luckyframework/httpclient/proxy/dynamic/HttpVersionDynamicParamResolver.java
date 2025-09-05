package com.luckyframework.httpclient.proxy.dynamic;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.core.meta.Version;
import com.luckyframework.httpclient.proxy.context.ValueContext;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

import java.util.Collections;
import java.util.List;


/**
 * Http版本处理器
 *
 * @author fukang
 * @version 3.0.2
 * @date 2023/9/5 00:10
 */
public class HttpVersionDynamicParamResolver extends AbstractDynamicParamResolver {

    @Override
    protected List<? extends ParamInfo> doParser(DynamicParamContext context) {
        ValueContext valueContext = context.getContext();
        return Collections.singletonList(new ParamInfo(getOriginalParamName(valueContext), ConversionUtils.conversion(valueContext.getValue(), Version.class)));
    }

}
