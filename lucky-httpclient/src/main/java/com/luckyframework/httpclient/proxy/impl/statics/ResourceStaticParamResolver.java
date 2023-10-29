package com.luckyframework.httpclient.proxy.impl.statics;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.ParamInfo;
import org.springframework.core.io.Resource;

import java.lang.annotation.Annotation;

/**
 * 资源解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 16:13
 */
public class ResourceStaticParamResolver extends SpELValueFieldEqualSeparationStaticParamResolver {

    @Override
    protected ParamInfo postProcess(MethodContext context, Annotation staticParamAnn, ParamInfo originalParamInfo) {
        return new ParamInfo(originalParamInfo.getName(), ConversionUtils.conversion(originalParamInfo.getValue(), Resource[].class));
    }
}
