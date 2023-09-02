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
 * 代理配置解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 12:34
 */
public class ProxyStaticParamResolver implements StaticParamResolver {

    @Override
    public List<TempPair<String, Object>> parser(Annotation staticParamAnn) {
        String ip = (String) AnnotationUtils.getValue(staticParamAnn, "ip");
        String port = (String) AnnotationUtils.getValue(staticParamAnn, "port");
        SpELConvert spELConverter = HttpClientProxyObjectFactory.getSpELConverter();
        String spELIp = String.valueOf(spELConverter.parseExpression(ip));
        Integer spELPort = Integer.parseInt(String.valueOf(spELConverter.parseExpression(port)).trim());
        return Collections.singletonList(TempPair.of(spELIp, spELPort));
    }
}
