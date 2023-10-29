package com.luckyframework.httpclient.proxy.impl.dynamic;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.ValueContext;
import org.springframework.core.io.Resource;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * 资源参数处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/9/27 15:10
 */
public class ResourceDynamicParamResolver extends AbstractDynamicParamResolver {

    @Override
    public List<ParamInfo> doParser(ValueContext context, Annotation dynamicParamAnn) {
        return Collections.singletonList(new ParamInfo(getOriginalParamName(context), ConversionUtils.conversion(context.getValue(), Resource[].class)));
    }
}
