package com.luckyframework.httpclient.proxy.statics;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.proxy.annotations.Timeout;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;

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

        // 连接超时时间
        int connectionTimeout;
        String connectionTimeoutExp = timeout.connectTimeoutExp();
        if (StringUtils.hasText(connectionTimeoutExp)) {
            connectionTimeout = context.parseExpression(connectionTimeoutExp, int.class);
        } else {
            connectionTimeout = timeout.connectTimeout();
        }

        // 数据读取超时时间
        int readTimeout;
        String readTimeoutExp = timeout.readTimeoutExp();
        if (StringUtils.hasText(readTimeoutExp)) {
            readTimeout = context.parseExpression(readTimeoutExp, int.class);
        } else {
            readTimeout = timeout.readTimeout();
        }

        // 数据写入超时时间
        int writeTimeout;
        String writeTimeoutExp = timeout.writeTimeoutExp();
        if (StringUtils.hasText(writeTimeoutExp)) {
            writeTimeout = context.parseExpression(writeTimeoutExp, int.class);
        } else {
            writeTimeout = timeout.writeTimeout();
        }

        // 整体调用超时时间
        int callTimeout;
        String callTimeoutExp = timeout.callTimeoutExp();
        if (StringUtils.hasText(callTimeoutExp)) {
            callTimeout = context.parseExpression(callTimeoutExp, int.class);
        } else {
            callTimeout = timeout.callTimeout();
        }

        // 获取链接超时时间
        int connectionRequestTimeout;
        String connectionRequestTimeoutExp = timeout.connectionRequestTimeoutExp();
        if (StringUtils.hasText(connectionRequestTimeoutExp)) {
            connectionRequestTimeout = context.parseExpression(connectionRequestTimeoutExp, int.class);
        } else {
            connectionRequestTimeout = timeout.connectionRequestTimeout();
        }

        return Collections.singletonList(new ParamInfo("timeout", TimeoutConfig.of(connectionTimeout, readTimeout, writeTimeout, callTimeout, connectionRequestTimeout)));
    }


    /**
     * 超时时间配置
     */
    public static class TimeoutConfig {
        private final int connectTimeout;
        private final int readTimeout;
        private final int writeTimeout;
        private final int callTimeout;
        private final int connectionRequestTimeout;


        private TimeoutConfig(int connectTimeout, int readTimeout, int writeTimeout, int callTimeout, int connectionRequestTimeout) {
            this.connectTimeout = connectTimeout;
            this.readTimeout = readTimeout;
            this.writeTimeout = writeTimeout;
            this.callTimeout = callTimeout;
            this.connectionRequestTimeout = connectionRequestTimeout;
        }

        public static TimeoutConfig of(int connectTimeout, int readTimeout, int writeTimeout, int callTimeout, int connectionRequestTimeout) {
            return new TimeoutConfig(connectTimeout, readTimeout, writeTimeout, callTimeout, connectionRequestTimeout);
        }

        public int getConnectTimeout() {
            return connectTimeout;
        }

        public int getReadTimeout() {
            return readTimeout;
        }

        public int getWriteTimeout() {
            return writeTimeout;
        }

        public int getCallTimeout() {
            return callTimeout;
        }

        public int getConnectionRequestTimeout() {
            return connectionRequestTimeout;
        }
    }
}
