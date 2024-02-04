package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.proxy.ParamInfo;
import org.springframework.core.io.Resource;

/**
 * 资源解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 16:13
 */
public class ResourceStaticParamResolver extends SpELValueFieldEqualSeparationStaticParamResolver {

    @Override
    protected ParamInfo postProcess(StaticParamAnnContext context, ParamInfo originalParamInfo) {
        return new ParamInfo(originalParamInfo.getName(), ConversionUtils.conversion(originalParamInfo.getValue(), Resource[].class));
    }
}
