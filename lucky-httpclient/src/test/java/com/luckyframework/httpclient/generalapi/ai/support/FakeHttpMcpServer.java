package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.mcp.JsonRpcMessage;
import com.luckyframework.httpclient.generalapi.ai.mcp.StreamableHttpTransport;
import com.luckyframework.httpclient.generalapi.ai.vendor.AiMaps;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import com.luckyframework.serializable.SerializationTypeToken;
import com.sun.net.httpserver.HttpExchange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地假MCP端点（Streamable HTTP）：按JSON-RPC方法名分发录制好的响应帧，
 * 并模拟「颁发会话标识 / 会话过期 / SSE响应 / 一律404」四种行为，全程只监听回环地址。
 * <pre>
 *     帧内容取自 {@code src/test/resources/ai/mcp/*.json}，响应里的 id 会按本次请求重新盖章，
 *     于是「请求-响应配对」「会话头保持」「404重连」这些契约都能在离线环境里断言。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class FakeHttpMcpServer implements TestHttpServer.ExchangeHandler {

    /**
     * 不带额外响应头时的取值
     */
    private static final Map<String, String> NO_HEADERS = Collections.emptyMap();

    private final TestHttpServer server;
    private final String path;

    /**
     * 当前有效的会话标识，initialize成功后换一个颁发给客户端
     */
    private String currentSession;

    /**
     * tools/call是否以SSE事件流回应（流里额外掺一条别的id的消息）
     */
    private boolean replyToolsCallWithEventStream;

    /**
     * 是否对所有请求（含initialize）回404，用来验证重连有界
     */
    private boolean rejectEverything;

    private int sessionSeq;
    private final List<Integer> statuses = new ArrayList<>();
    private final List<String> methods = new ArrayList<>();

    public FakeHttpMcpServer(TestHttpServer server, String path) {
        this.server = server;
        this.path = path;
        server.registerHandler(path, this);
    }

    @Override
    public synchronized void handle(HttpExchange exchange) throws Exception {
        TestHttpServer.RecordedRequest sent = server.lastRequest(path);
        Map<String, Object> frame = AiJsonUtils.fromJson(sent.body(),
                new SerializationTypeToken<Map<String, Object>>() {
                });
        String method = String.valueOf(frame.get("method"));
        Object id = frame.get("id");
        methods.add(method);

        if (rejectEverything) {
            reply(exchange, 404, "{}");
            return;
        }
        if ("initialize".equals(method)) {
            currentSession = "s" + (++sessionSeq);
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put(StreamableHttpTransport.SESSION_ID_HEADER, currentSession);
            reply(exchange, 200, headers, recorded("ai/mcp/initialize-response.json", id));
            return;
        }
        String presented = sent.header(StreamableHttpTransport.SESSION_ID_HEADER);
        if (!currentSession.equals(presented)) {
            reply(exchange, 404, "{}");
            return;
        }
        if (method.startsWith("notifications/")) {
            // 通知没有回执，以202空响应体表达「已接受」
            reply(exchange, 202, NO_HEADERS, "");
            return;
        }
        if ("tools/list".equals(method)) {
            reply(exchange, 200, NO_HEADERS, recorded("ai/mcp/tools-list-response.json", id));
            return;
        }
        if ("tools/call".equals(method)) {
            String matched = recorded("ai/mcp/tools-call-response.json", id);
            if (replyToolsCallWithEventStream) {
                // 掺一条别的id的帧：本请求只该取到与自己id匹配的那一条
                String decoy = recorded("ai/mcp/tools-list-response.json", 9999);
                server.respondEventStream(exchange, NO_HEADERS, Arrays.asList(decoy, matched));
                statuses.add(200);
                return;
            }
            reply(exchange, 200, NO_HEADERS, matched);
            return;
        }
        Integer requestId = AiMaps.getInteger(frame, "id");
        reply(exchange, 200, NO_HEADERS,
                JsonRpcMessage.response(requestId == null ? 0 : requestId, Collections.emptyMap()).toJson());
    }

    /**
     * 让客户端手里的会话标识失效，模拟会话过期
     */
    public synchronized void expireSession() {
        currentSession = "s-rotated";
    }

    /**
     * tools/call改以SSE事件流回应
     *
     * @param eventStream 是否用事件流回应
     */
    public synchronized void setReplyToolsCallWithEventStream(boolean eventStream) {
        this.replyToolsCallWithEventStream = eventStream;
    }

    /**
     * 对所有请求一律回404（含initialize），用于验证重连次数有界
     *
     * @param reject 是否一律拒绝
     */
    public synchronized void setRejectEverything(boolean reject) {
        this.rejectEverything = reject;
    }

    /**
     * 指定JSON-RPC方法收到的请求次数
     *
     * @param method 方法名
     * @return 次数
     */
    public synchronized int countMethod(String method) {
        int count = 0;
        for (String seen : methods) {
            if (seen.equals(method)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 指定HTTP状态码的回包次数
     *
     * @param status 状态码
     * @return 次数
     */
    public synchronized int countStatus(int status) {
        int count = 0;
        for (Integer seen : statuses) {
            if (seen.intValue() == status) {
                count++;
            }
        }
        return count;
    }

    /**
     * 把录制样本按本次请求的id重新盖章后作为响应文本
     */
    private static String recorded(String path, Object id) {
        Map<String, Object> node = AiFixtures.jsonResource(path);
        node.put("id", id);
        return AiJsonUtils.toJson(node);
    }

    private void reply(HttpExchange exchange, int status, Map<String, String> headers, String body) throws Exception {
        statuses.add(status);
        server.respond(exchange, status, "application/json;charset=UTF-8", headers, body);
    }

    private void reply(HttpExchange exchange, int status, String body) throws Exception {
        reply(exchange, status, NO_HEADERS, body);
    }
}
