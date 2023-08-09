package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.conversion.ConversionUtils;
import com.luckyframework.httpclient.proxy.ParameterProcessor;
import org.springframework.core.io.Resource;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * 资源参数处理器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/29 17:47
 */
public class ResourceParameterProcessor implements ParameterProcessor {

    @Override
    public Object paramProcess(Object originalParam, Annotation proxyHttpParamAnn) {
        if (originalParam == null){
            return null;
        }
        return ConversionUtils.conversion(originalParam, Resource[].class);
    }

}
