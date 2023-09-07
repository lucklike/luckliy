package com.luckyframework.httpclient.proxy.impl;

import com.luckyframework.common.TempPair;
import com.luckyframework.httpclient.core.HttpExecutorException;
import com.luckyframework.reflect.AnnotationUtils;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;

/**
 * 基于注解value属性、配置使用'='分隔的静态参数解析器
 * 加上是否需要URLEncode编码的逻辑
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/8/4 15:51
 */
public class URLEncodeStaticParamResolver extends SpELValueFieldEqualSeparationStaticParamResolver {

    @Override
    protected TempPair<String, Object> postProcess(TempPair<String, Object> originalPair, Annotation staticParamAnn) {
        boolean urlEncode = (boolean) AnnotationUtils.getValue(staticParamAnn, "urlEncode");
        if (urlEncode) {
            String charset = (String) AnnotationUtils.getValue(staticParamAnn, "charset");
            try {
                String encodeValue = URLEncoder.encode(String.valueOf(originalPair.getTwo()), charset);
                return TempPair.of(originalPair.getOne(), encodeValue);
            } catch (UnsupportedEncodingException e) {
                throw new HttpExecutorException("url encoding(" + charset + ") exception: value='" + originalPair.getTwo() + "'", e);
            }
        }
        return super.postProcess(originalPair, staticParamAnn);
    }
}
