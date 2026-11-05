package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.core.meta.Header;
import com.luckyframework.httpclient.core.meta.HttpHeaderManager;
import com.luckyframework.httpclient.core.meta.Response;

/**
 * MCP Streamable HTTP传输的响应快照：状态码、Content-Type、会话标识头与响应体文本。
 * 之所以不做成「对象结果」，是因为同一个端点可能返回JSON、SSE事件流、404（会话失效）三种形态，
 * 传输层必须看到原始文本与状态码才能分支处理。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API，由 {@link StreamableHttpTransport} 使用，经 {@link McpHttpResponseConvert} 挂载到注解上
 */
public class McpHttpResponse {

    /**
     * 会话失效的HTTP状态码
     */
    public static final int SESSION_NOT_FOUND = 404;

    private static final String EVENT_STREAM = "text/event-stream";

    private final int status;
    private final String contentType;
    private final String sessionId;
    private final String body;

    public McpHttpResponse(int status, String contentType, String sessionId, String body) {
        this.status = status;
        this.contentType = contentType;
        this.sessionId = sessionId;
        this.body = body;
    }

    /**
     * 从框架响应对象里取出四项原始信息
     */
    public static McpHttpResponse from(Response response) {
        HttpHeaderManager headers = response.getHeaderManager();
        return new McpHttpResponse(
                response.getStatus(),
                headerValue(headers, "Content-Type"),
                headerValue(headers, StreamableHttpTransport.SESSION_ID_HEADER),
                response.getStringResult());
    }

    public int getStatus() {
        return status;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getBody() {
        return body;
    }

    /**
     * 响应体是否为SSE事件流（需要按帧解析出与本请求id匹配的消息）
     */
    public boolean isEventStream() {
        return contentType != null && contentType.contains(EVENT_STREAM);
    }

    /**
     * 响应体是否为空（通知类消息常以202+空响应体回执）
     */
    public boolean isEmptyBody() {
        return body == null || body.trim().isEmpty();
    }

    private static String headerValue(HttpHeaderManager headers, String name) {
        Header header = headers.getFirstHeader(name);
        return header == null || header.getValue() == null ? null : String.valueOf(header.getValue());
    }
}
