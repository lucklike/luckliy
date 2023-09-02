package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.StaticParamResolver;
import com.luckyframework.reflect.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于注解value属性、配置使用'='分隔并支持SpEL表达式的静态参数解析器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 14:39
 */
public class SpELValueFieldEqualSeparationStaticParamResolver implements StaticParamResolver {



    @Override
    public List<TempPair<String, Object>> parser(Annotation staticParamAnn) {
        String[] values = (String[]) AnnotationUtils.getValue(staticParamAnn, "value");
        List<TempPair<String, Object>> tempPairList = new ArrayList<>(values.length);
        for (String value : values) {
            int index = value.indexOf("=");
            TempPair<String, Object> pair;
            if (index == -1) {
                throw new IllegalArgumentException("Wrong static expression: '" + value + "'");
            }
            String name = value.substring(0, index).trim();
            String valueExpression = value.substring(index + 1).trim();
            pair = TempPair.of(name, HttpClientProxyObjectFactory.getSpELConverter().parseExpression(valueExpression));
            tempPairList.add(postProcess(pair, staticParamAnn));
        }
        return tempPairList;
    }

    protected TempPair<String, Object> postProcess(TempPair<String, Object> originalPair, Annotation staticParamAnn) {
        return originalPair;
    }
}
