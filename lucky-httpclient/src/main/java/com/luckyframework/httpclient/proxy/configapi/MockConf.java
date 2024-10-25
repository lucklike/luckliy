package com.luckyframework.httpclient.proxy.configapi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/8/17 04:57
 */
public class MockConf {

    private String enable;
    private Boolean cache;
    private String response;
    private Integer status = 200;
    private Map<String, List<Object>> header = new LinkedHashMap<>();
    private String body = "";

    public String getEnable() {
        return enable;
    }

    public void setEnable(String enable) {
        this.enable = enable;
    }

    public Boolean getCache() {
        return cache;
    }

    public void setCache(Boolean cache) {
        this.cache = cache;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Map<String, List<Object>> getHeader() {
        return header;
    }

    public void setHeader(Map<String, List<Object>> header) {
        this.header = header;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
