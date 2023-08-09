package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.SpELConvert;
import com.luckyframework.httpclient.proxy.StaticParamResolver;
import com.luckyframework.reflect.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * Basic Auth配置解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 12:34
 */
public class BasicAuthStaticParamResolver implements StaticParamResolver {

    @Override
    public List<TempPair<String, Object>> parser(Annotation staticParamAnn) {
        String username = (String) AnnotationUtils.getValue(staticParamAnn, "username");
        String password = (String) AnnotationUtils.getValue(staticParamAnn, "password");
        SpELConvert spELConverter = HttpClientProxyObjectFactory.getSpELConverter();
        String userNameResult = String.valueOf(spELConverter.analyze(username));
        Object passwordResult = spELConverter.analyze(password);
        return Collections.singletonList(TempPair.of(userNameResult, passwordResult));
    }
}
