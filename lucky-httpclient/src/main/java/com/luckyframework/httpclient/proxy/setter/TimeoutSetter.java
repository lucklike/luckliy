package com.luckyframework.httpclient.proxy.setter;

import com.luckyframework.common.TempTriple;
import com.luckyframework.httpclient.core.meta.Request;

/**
 * 超时时间参数设置器
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 11:07
 */
public class TimeoutSetter extends ValueNameParameterSetter {

    @Override
    @SuppressWarnings("unchecked")
    public void doSet(Request request, String paramName, Object paramValue) {
        TempTriple<Integer, Integer, Integer> triple = (TempTriple<Integer, Integer, Integer>) paramValue;
        Integer connectionTimeout = triple.getOne();
        Integer readTimeout = triple.getTwo();
        Integer writerTimeout = triple.getThree();
        if (connectionTimeout > 0) {
            request.setConnectTimeout(connectionTimeout);
        }
        if (readTimeout > 0) {
            request.setReadTimeout(readTimeout);
        }
        if (writerTimeout > 0) {
            request.setWriterTimeout(writerTimeout);
        }
    }
}
