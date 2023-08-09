package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.TempPair;
import com.luckyframework.conversion.ConversionUtils;
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
    protected TempPair<String, Object> postProcess(TempPair<String, Object> originalPair, Annotation staticParamAnn) {
        return TempPair.of(originalPair.getOne(), ConversionUtils.conversion(originalPair.getTwo(), Resource[].class));
    }

}
