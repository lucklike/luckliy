package com.luckyframework.httpclient.proxy.configapi;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock配置
 *
 * @author fukang
 * @version 1.0.0
 * @date 2024/8/17 04:57
 */
public class MockConf {

    private String enable;
    private String response;
    private Integer status = 200;
    private List<String> header = new ArrayList<>();
    private String body = "";

    public String getEnable() {
        return enable;
    }

    public void setEnable(String enable) {
        this.enable = enable;
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

    public List<String> getHeader() {
        return header;
    }

    public void setHeader(List<String> header) {
        this.header = header;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
