package com.luckyframework.httpclient.proxy.logging.processor;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.proxy.logging.BodyProcessor;

import java.util.Map;

public class MultipartFormDataProcessor implements BodyProcessor<Map<String, Object>> {

    @Override
    public Map<String, Object> convert(Request request) {
        return request.getMultipartFormParameters();
    }

    @Override
    public String process(Map<String, Object> body) {
        return "";
    }
}
