package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.StringUtils;
import com.luckyframework.common.TempTriple;
import com.luckyframework.httpclient.proxy.ParamInfo;
import com.luckyframework.httpclient.proxy.annotations.Timeout;

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
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        int connectionTimeout;
        String connectionTimeoutExp = context.getAnnotationAttribute(Timeout.ATTRIBUTE_CONNECTION_TIMEOUT_EXP, String.class);
        if (StringUtils.hasText(connectionTimeoutExp)) {
            connectionTimeout = Integer.parseInt(String.valueOf(parseExpression(connectionTimeoutExp, context)).trim());
        } else {
            connectionTimeout = context.getAnnotationAttribute(Timeout.ATTRIBUTE_CONNECTION_TIMEOUT, int.class);
        }

        int readTimeout;
        String readTimeoutExp = context.getAnnotationAttribute(Timeout.ATTRIBUTE_READ_TIMEOUT_EXP, String.class);
        if (StringUtils.hasText(readTimeoutExp)) {
            readTimeout = Integer.parseInt(String.valueOf(parseExpression(readTimeoutExp, context)).trim());
        } else {
            readTimeout = context.getAnnotationAttribute(Timeout.ATTRIBUTE_READ_TIMEOUT, int.class);
        }

        int writeTimeout;
        String writeTimeoutExp = context.getAnnotationAttribute(Timeout.ATTRIBUTE_WRITE_TIMEOUT_EXP, String.class);
        if (StringUtils.hasText(writeTimeoutExp)) {
            writeTimeout = Integer.parseInt(String.valueOf(parseExpression(writeTimeoutExp, context)).trim());
        } else {
            writeTimeout = context.getAnnotationAttribute(Timeout.ATTRIBUTE_WRITE_TIMEOUT, int.class);
        }
        TempTriple<Integer, Integer, Integer> timeoutTriple = TempTriple.of(connectionTimeout, readTimeout, writeTimeout);
        return Collections.singletonList(new ParamInfo("timeout", timeoutTriple));
    }
}
