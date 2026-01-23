package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.statics.TimeoutStaticParamResolver;

import java.util.function.Consumer;

import static com.luckyframework.httpclient.core.executor.Constant.HTTPCLIENT_PM_CONNECTION_REQUEST_TIMEOUT;
import static com.luckyframework.httpclient.core.executor.Constant.OKHTTP_PM_CALL_TIMEOUT;
import static com.luckyframework.httpclient.core.executor.Constant.OKHTTP_PM_WRITE_TIMEOUT;

/**
 * 超时时间参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class TimeoutSetter extends ValueNameParameterSetter {

    @Override
    public void doSet(Request request, String paramName, Object paramValue) {
        TimeoutStaticParamResolver.TimeoutConfig timeoutConfig = (TimeoutStaticParamResolver.TimeoutConfig) paramValue;

        // 通用
        setTimeout(timeoutConfig.getConnectTimeout(), request::setConnectTimeout);
        setTimeout(timeoutConfig.getReadTimeout(), request::setReadTimeout);

        // OkHttp
        setTimeout(timeoutConfig.getWriteTimeout(), timeout -> request.addAdditionalParameter(OKHTTP_PM_WRITE_TIMEOUT, timeout));
        setTimeout(timeoutConfig.getCallTimeout(), timeout -> request.addAdditionalParameter(OKHTTP_PM_CALL_TIMEOUT, timeout));

        // HttpClient
        setTimeout(timeoutConfig.getConnectionRequestTimeout(), timeout -> request.addAdditionalParameter(HTTPCLIENT_PM_CONNECTION_REQUEST_TIMEOUT, timeout));
    }


    /**
     * 设置超时时间，当且仅当超时时间大于0时才会进行设置
     *
     * @param timeout    超时时间
     * @param timeSetter 超时时间设置逻辑
     */
    private void setTimeout(int timeout, Consumer<Integer> timeSetter) {
        if (timeout > 0) {
            timeSetter.accept(timeout);
        }
    }
}
