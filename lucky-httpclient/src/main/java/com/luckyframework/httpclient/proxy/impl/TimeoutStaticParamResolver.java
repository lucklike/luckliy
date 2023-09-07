package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.TempPair;
import com.luckyframework.common.TempTriple;
import com.luckyframework.httpclient.proxy.StaticParamResolver;
import com.luckyframework.reflect.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * 超时时间配置解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 20:40
 */
public class TimeoutStaticParamResolver implements StaticParamResolver {

    @Override
    public List<TempPair<String, Object>> parser(Annotation staticParamAnn) {
        int connectionTimeout = (int) AnnotationUtils.getValue(staticParamAnn, "connectionTimeout");
        int readTimeout = (int) AnnotationUtils.getValue(staticParamAnn, "readTimeout");
        int writeTimeout = (int) AnnotationUtils.getValue(staticParamAnn, "writeTimeout");
        TempTriple<Integer, Integer, Integer> timeoutTriple = TempTriple.of(connectionTimeout, readTimeout, writeTimeout);
        return Collections.singletonList(TempPair.of("", timeoutTriple));
    }

}
