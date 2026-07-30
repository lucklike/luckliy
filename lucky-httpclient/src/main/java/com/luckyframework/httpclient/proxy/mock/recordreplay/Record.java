package com.luckyframework.httpclient.proxy.mock.recordreplay;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 响应记录
 * 用于封装一次HTTP请求的完整响应信息，包括状态码、时间信息、响应头和响应体
 *
 * @author fk7075
 * @version 3.0.3
 * @since 2026-07-30 00:47:02
 */
public class Record {
    /**
     * 记录 ID
     * 唯一标识一条响应记录
     */
    private String id;

    /**
     * HTTP 状态码
     * 如 200、404、500 等
     */
    private Integer status;

    /**
     * 请求开始时间
     * 记录请求发起的时刻
     */
    private Date startTime;

    /**
     * 接收到响应的时间
     * 记录完整响应接收完成的时刻
     */
    private Date endTime;

    /**
     * 执行时间
     * 从请求开始到响应接收完成所耗费的毫秒数
     */
    private Long executionTime;

    /**
     * 响应体内容
     * 以字节数组形式存储的原始响应数据
     */
    private byte[] body;

    /**
     * 响应头数据
     * Map结构，键为响应头名称，值为该名称对应的所有值列表
     */
    private Map<String, List<Object>> headers;

    /**
     * 获取记录 ID
     *
     * @return 记录的唯一标识符
     */
    public String getId() {
        return id;
    }

    /**
     * 设置记录 ID
     *
     * @param id 记录的唯一标识符
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取 HTTP 状态码
     *
     * @return HTTP 状态码，如 200、404、500 等
     */
    public Integer getStatus() {
        return status;
    }

    /**
     * 设置 HTTP 状态码
     *
     * @param status HTTP 状态码
     */
    public void setStatus(Integer status) {
        this.status = status;
    }

    /**
     * 获取响应头数据
     *
     * @return 响应头 Map，键为响应头名称，值为该名称对应的所有值列表
     */
    public Map<String, List<Object>> getHeaders() {
        return headers;
    }

    /**
     * 设置响应头数据
     *
     * @param headers 响应头 Map
     */
    public void setHeaders(Map<String, List<Object>> headers) {
        this.headers = headers;
    }

    /**
     * 获取请求开始时间
     *
     * @return 请求发起的时刻
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * 设置请求开始时间
     *
     * @param startTime 请求发起的时刻
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * 获取接收到响应的时间
     *
     * @return 完整响应接收完成的时刻
     */
    public Date getEndTime() {
        return endTime;
    }

    /**
     * 设置接收到响应的时间
     *
     * @param endTime 完整响应接收完成的时刻
     */
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    /**
     * 获取执行时间
     *
     * @return 从请求开始到响应接收完成所耗费的毫秒数
     */
    public Long getExecutionTime() {
        return executionTime;
    }

    /**
     * 设置执行时间
     *
     * @param executionTime 从请求开始到响应接收完成所耗费的毫秒数
     */
    public void setExecutionTime(Long executionTime) {
        this.executionTime = executionTime;
    }

    /**
     * 获取响应体内容
     *
     * @return 以字节数组形式存储的原始响应数据
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * 设置响应体内容
     *
     * @param body 以字节数组形式存储的原始响应数据
     */
    public void setBody(byte[] body) {
        this.body = body;
    }
}