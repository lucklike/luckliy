package com.luckyframework.httpclient.proxy.impl.statics;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempTriple;
import com.luckyframework.httpclient.proxy.MethodContext;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.StaticParamResolver;

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
    public List<ParamInfo> parser(MethodContext context, Annotation staticParamAnn) {
        int connectionTimeout;
        String connectionTimeoutExp = context.getAnnotationAttribute(staticParamAnn, "connectionTimeoutExp", String.class);
        if (StringUtils.hasText(connectionTimeoutExp)) {
            connectionTimeout = Integer.parseInt(String.valueOf(parseExpression(connectionTimeoutExp, context, staticParamAnn)).trim());
        } else {
            connectionTimeout = context.getAnnotationAttribute(staticParamAnn, "connectionTimeout", int.class);
        }

        int readTimeout;
        String readTimeoutExp = context.getAnnotationAttribute(staticParamAnn, "readTimeoutExp", String.class);
        if (StringUtils.hasText(readTimeoutExp)) {
            readTimeout = Integer.parseInt(String.valueOf(parseExpression(readTimeoutExp, context, staticParamAnn)).trim());
        } else {
            readTimeout = context.getAnnotationAttribute(staticParamAnn, "readTimeout", int.class);
        }

        int writeTimeout;
        String writeTimeoutExp = context.getAnnotationAttribute(staticParamAnn, "writeTimeoutExp", String.class);
        if (StringUtils.hasText(writeTimeoutExp)) {
            writeTimeout = Integer.parseInt(String.valueOf(parseExpression(writeTimeoutExp, context, staticParamAnn)).trim());
        } else {
            writeTimeout = context.getAnnotationAttribute(staticParamAnn, "writeTimeout", int.class);
        }
        TempTriple<Integer, Integer, Integer> timeoutTriple = TempTriple.of(connectionTimeout, readTimeout, writeTimeout);
        return Collections.singletonList(new ParamInfo("timeout", timeoutTriple));
    }
}
