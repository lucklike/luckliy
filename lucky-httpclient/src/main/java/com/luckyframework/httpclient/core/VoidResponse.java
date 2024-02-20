package com.luckyframework.httpclient.core;

import java.util.List;
import java.util.Map;

/**
 * 不需要返回值的响应
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/2/1 02:46
 */
public class VoidResponse {

    private final ResponseMetaData metaData;

    public VoidResponse(ResponseMetaData metaData) {
        this.metaData = metaData;
    }

    public Request getRequest() {
        return this.metaData.getRequest();
    }

    public int getStatus() {
        return metaData.getStatus();
    }

    public String getProtocol() {
        return metaData.getProtocol();
    }

    public long getContentLength() {
        return metaData.getContentLength();
    }

    public ContentType getContentType() {
        return metaData.getContentType();
    }

    public HttpHeaderManager getHeaderManager() {
        return metaData.getHeaderManager();
    }

    public List<Header> getCookies() {
        return metaData.getCookies();
    }

    public Map<String, Object> getSimpleHeaders() {
        return getHeaderManager().getSimpleHeaders();
    }

    public Map<String, Object> getSimpleCookies() {
        return metaData.getSimpleCookies();
    }

}
