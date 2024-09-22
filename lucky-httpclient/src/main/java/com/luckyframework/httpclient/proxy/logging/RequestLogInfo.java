package com.luckyframework.httpclient.proxy.logging;

import com.luckyframework.common.ContainerUtils;
import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.Request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.luckyframework.httpclient.proxy.logging.KeyValueProcessor.DEFAULT;

/**
 * @author fukang
 * @version 1.0.0
 * @date 2024/9/22 13:16
 */
public class RequestLogInfo {

    private final Request request;

    private KeyValueProcessor headerProcessor = DEFAULT;

    private KeyValueProcessor queryProcessor = DEFAULT;

    private KeyValueProcessor pathProcessor = DEFAULT;

    private BodyProcessors bodyProcessors;

    public RequestLogInfo(Request request) {
        this.request = request;
    }

    public void setHeaderProcessor(KeyValueProcessor headerProcessor) {
        this.headerProcessor = headerProcessor;
    }

    public void setQueryProcessor(KeyValueProcessor queryProcessor) {
        this.queryProcessor = queryProcessor;
    }

    public void setPathProcessor(KeyValueProcessor pathProcessor) {
        this.pathProcessor = pathProcessor;
    }

    public void setBodyProcessors(BodyProcessors bodyProcessors) {
        this.bodyProcessors = bodyProcessors;
    }

    public String getUrl() {
        String url = request.getUrl();
        int index = url.indexOf("?");
        if (index != -1) {
            url = url.substring(0, index);
        }
        return url;
    }

    public Map<Object, List<Object>> getQuery() {
        Map<Object, List<Object>> resultMap = new LinkedHashMap<>();
        Map<String, List<Object>> queryParameters = request.getQueryParameters();
        for (Map.Entry<String, List<Object>> entry : queryParameters.entrySet()) {
            KeyValueProcessor.KV kv = queryProcessor.process(entry.getKey(), entry.getValue());
            Object name = kv.getKey();
            List<Object> valueList = (List<Object>) kv.getValue();

            resultMap.put(name, valueList);
        }
        return resultMap;
    }

    public Map<Object, List<Object>> getHeader() {
        Map<Object, List<Object>> resultMap = new LinkedHashMap<>();
        for (Map.Entry<String, List<Header>> entry : request.getHeaderMap().entrySet()) {
            String key = entry.getKey();
            List<Header> headers = filterHeader(entry.getValue());
            KeyValueProcessor.KV kv = headerProcessor.process(key, headers);
            resultMap.put(kv.getKey(), (List<Object>) kv.getValue());
        }
        return resultMap;
    }

    public String getBody() {
        return bodyProcessors.process(request);
    }

    private List<Header> filterHeader(List<Header> list) {
        List<Header> resultHeaders = new ArrayList<>();
        for (Header header : list) {
            if (header.getHeaderType() == Header.HeaderType.SET) {
                resultHeaders.clear();
            }
            resultHeaders.add(header);
        }
        return resultHeaders;
    }
}
