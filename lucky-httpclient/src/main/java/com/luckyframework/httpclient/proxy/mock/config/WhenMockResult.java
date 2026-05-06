package com.luckyframework.httpclient.proxy.mock.config;


import java.util.Map;

/**
 * 带条件的模拟响应结果
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026/4/30 01:03
 */
public class WhenMockResult {

    /**
     * 使用此响应结果的条件
     */
    private String when;

    /**
     * 模拟延时配置，单位毫秒
     */
    private Long latency;

    /**
     * 模拟 HTTP 状态码
     */
    private Integer status;

    /**
     * 模拟响应头
     */
    private Map<String, Object> headers;

    /**
     * 模拟响应体
     */
    private MockBody body;

    /**
     * 获取模拟延时配置，单位毫秒
     *
     * @return 拟延时配置，单位毫秒
     */
    public Long getLatency() {
        return latency;
    }

    /**
     * 模拟延时配置，单位毫秒
     *
     * @param latency 拟延时配置，单位毫秒
     */
    public void setLatency(Long latency) {
        this.latency = latency;
    }

    /**
     * 获取模拟 HTTP 状态码
     *
     * @return HTTP 状态码
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 模拟 HTTP 状态码
     *
     * @param status HTTP 状态码
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 获取模拟响应头
     *
     * @return 模拟响应头
     */
    public Map<String, Object> getHeaders() {
        return headers;
    }

    /**
     * 模拟响应头
     *
     * @param headers 模拟响应头
     */
    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }

    /**
     * 获取模拟响应体
     *
     * @return 模拟响应体
     */
    public MockBody getBody() {
        return body;
    }

    /**
     * 模拟响应体
     *
     * @param body 模拟响应体
     */
    public void setBody(MockBody body) {
        this.body = body;
    }

    /**
     * 获取使用此响应结果的条件
     *
     * @return 使用此响应结果的条件
     */
    public String getWhen() {
        return when;
    }

    /**
     * 设置使用此响应结果的条件
     *
     * @param when 使用此响应结果的条件
     */
    public void setWhen(String when) {
        this.when = when;
    }
}
