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
        Timeout timeout = context.toAnnotation(Timeout.class);
        int connectionTimeout;
        String connectionTimeoutExp = timeout.connectionTimeoutExp();
        if (StringUtils.hasText(connectionTimeoutExp)) {
            connectionTimeout = Integer.parseInt(String.valueOf(parseExpression(connectionTimeoutExp, context)).trim());
        } else {
            connectionTimeout = timeout.connectionTimeout();
        }

        int readTimeout;
        String readTimeoutExp = timeout.readTimeoutExp();
        if (StringUtils.hasText(readTimeoutExp)) {
            readTimeout = Integer.parseInt(String.valueOf(parseExpression(readTimeoutExp, context)).trim());
        } else {
            readTimeout = timeout.readTimeout();
        }

        int writeTimeout;
        String writeTimeoutExp = timeout.writeTimeoutExp();
        if (StringUtils.hasText(writeTimeoutExp)) {
            writeTimeout = Integer.parseInt(String.valueOf(parseExpression(writeTimeoutExp, context)).trim());
        } else {
            writeTimeout = timeout.writeTimeout();
        }
        TempTriple<Integer, Integer, Integer> timeoutTriple = TempTriple.of(connectionTimeout, readTimeout, writeTimeout);
        return Collections.singletonList(new ParamInfo("timeout", timeoutTriple));
    }
}
