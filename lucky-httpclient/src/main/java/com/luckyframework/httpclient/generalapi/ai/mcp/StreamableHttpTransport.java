package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.vendor.AiMaps;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MCP的Streamable HTTP传输：把每条JSON-RPC消息POST到配置的地址，响应按单JSON或SSE事件流解析。
 * <pre>
 *     收发规则：
 *     1.请求头固定带 {@code Accept: application/json, text/event-stream}（注解里声明）与
 *       {@code Content-Type: application/json}（{@code @JsonBody} 决定），配置里的附加头逐项写入；
 *     2.握手响应携带 {@code Mcp-Session-Id} 时记住该值，之后的每个请求头都带上它；
 *     3.响应体为空（通知类消息常以202回执）时不交付任何消息；为SSE时按帧解析，
 *       只把与本请求 {@code id} 匹配的消息（以及无id的通知）交给监听器；
 *     4.HTTP 404表示会话失效：重新握手一次并重发该请求，再失败即报错，绝不无限重试。
 *
 *     一次send即完成「发出—拿到响应—交付」，所以不需要stdio那样的后台读线程。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @see McpHttpApi
 * @see StdioTransport
 */
public class StreamableHttpTransport implements McpTransport {

    /**
     * 会话标识头名
     */
    public static final String SESSION_ID_HEADER = "Mcp-Session-Id";

    /**
     * 本传输能接受的响应媒体类型
     */
    public static final String ACCEPT_HEADER_VALUE = "application/json, text/event-stream";

    private static final String DATA_PREFIX = "data:";

    private final McpServerConfig config;
    private final McpHttpApi api;

    /**
     * 会话失效时重新握手的动作，由 {@link McpClient} 注入；未注入时404直接报错
     */
    private Runnable reseeder;

    /**
     * 重握手期间置位，防止「重握手自身又遇404」时无限递归
     */
    private final AtomicBoolean reHandshaking = new AtomicBoolean(false);

    private volatile Listener listener;
    private volatile boolean open;
    private volatile boolean closed;
    private volatile String sessionId;

    public StreamableHttpTransport(McpServerConfig config) {
        this(config, createApi(config));
    }

    /**
     * 供测试注入已装载好地址的API代理
     */
    StreamableHttpTransport(McpServerConfig config, McpHttpApi api) {
        this.config = config;
        this.api = api;
    }

    @Override
    public void open(Listener listener) {
        this.listener = listener;
        this.open = true;
    }

    @Override
    public void send(String jsonFrame) {
        if (closed || !open) {
            throw McpException.disconnected(new IllegalStateException("the MCP http transport is already closed"));
        }
        Map<String, Object> frame = parseObject(jsonFrame, "the outgoing JSON-RPC frame");
        Integer requestId = AiMaps.getInteger(frame, "id");
        McpHttpResponse response = post(frame);
        if (response.getStatus() == McpHttpResponse.SESSION_NOT_FOUND) {
            if (!tryReHandshake()) {
                throw sessionExpired();
            }
            // 重握手会换成新的会话标识，这里只补发一次
            response = post(frame);
            if (response.getStatus() == McpHttpResponse.SESSION_NOT_FOUND) {
                throw sessionExpired();
            }
        }
        if (response.getStatus() >= 400) {
            throw new McpException("The MCP server [" + config.getServerName() + "] responded HTTP "
                    + response.getStatus() + ": " + response.getBody());
        }
        if (response.getSessionId() != null && !response.getSessionId().isEmpty()) {
            this.sessionId = response.getSessionId();
        }
        for (Map<String, Object> message : readMessages(response, requestId)) {
            listener.onMessage(message);
        }
    }

    @Override
    public boolean isOpen() {
        return open && !closed;
    }

    @Override
    public void close() {
        closed = true;
        open = false;
        sessionId = null;
    }

    /**
     * 注入「会话失效时重新握手」的动作，传null表示关闭404重连能力
     *
     * @param reseeder 重新握手动作
     */
    void setReseeder(Runnable reseeder) {
        this.reseeder = reseeder;
    }

    /**
     * 当前持有的会话标识，供断言与排障
     */
    String getSessionId() {
        return sessionId;
    }

    /**
     * 发一次HTTP请求，地址与头之外的失败一律包成 {@link McpException}
     */
    private McpHttpResponse post(Map<String, Object> frame) {
        try {
            return api.post(requestHeaders(), frame);
        } catch (McpException e) {
            throw e;
        } catch (Throwable e) {
            throw new McpException(e, "Failed to POST the MCP message to server ["
                    + config.getServerName() + "] at " + config.getUrl() + ": " + e);
        }
    }

    /**
     * 配置里的请求头 + 会话标识头（未握手时不放该键）
     */
    private Map<String, Object> requestHeaders() {
        Map<String, Object> headers = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : config.getHeaders().entrySet()) {
            headers.put(entry.getKey(), entry.getValue());
        }
        String session = sessionId;
        if (session != null && !session.isEmpty()) {
            headers.put(SESSION_ID_HEADER, session);
        }
        return headers;
    }

    /**
     * 会话失效时重新握手一次：置位防递归，清掉失效的会话标识（initialize请求本身不带它）
     *
     * @return 是否成功完成一次重握手，未注入动作或已在重握手中时返回false
     */
    private boolean tryReHandshake() {
        Runnable reseed = reseeder;
        if (reseed == null || !reHandshaking.compareAndSet(false, true)) {
            return false;
        }
        sessionId = null;
        try {
            reseed.run();
            return sessionId != null;
        } catch (Throwable e) {
            McpLogs.warn("The MCP session re-handshake failed: {}", e.getMessage());
            return false;
        } finally {
            reHandshaking.set(false);
        }
    }

    private McpException sessionExpired() {
        return new McpException("The MCP session of server [" + config.getServerName()
                + "] is expired (HTTP 404) and one re-handshake did not help（已重握手并重发一次仍失败，不再重试）.");
    }

    /**
     * 按响应形态取出待交付的消息：SSE按帧解析，单JSON直接取，空响应体不交付任何消息
     *
     * @param requestId 本次请求的id，null表示这是通知（不等任何响应）
     */
    private List<Map<String, Object>> readMessages(McpHttpResponse response, Integer requestId) {
        if (response.isEmptyBody()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> frames = response.isEventStream()
                ? readEventStream(response.getBody())
                : Collections.singletonList(
                        parseObject(response.getBody(), "the response body of " + config.getUrl()));
        List<Map<String, Object>> matched = new ArrayList<>();
        for (Map<String, Object> frame : frames) {
            Integer id = AiMaps.getInteger(frame, "id");
            // 无id的通知照常交付（由通道容忍），带id但配不上对的响应跳过——它本来就唤醒不了任何等待
            if (requestId == null || id == null || requestId.equals(id)) {
                matched.add(frame);
            }
        }
        return matched;
    }

    /**
     * 解析SSE文本：以空行为事件边界，把一个事件内多条 {@code data:} 行按序换行连接后作为一帧JSON
     */
    private static List<Map<String, Object>> readEventStream(String body) {
        List<Map<String, Object>> frames = new ArrayList<>();
        StringBuilder data = new StringBuilder();
        for (String line : body.split("\r?\n")) {
            if (line.startsWith(DATA_PREFIX)) {
                if (data.length() > 0) {
                    data.append('\n');
                }
                data.append(line.substring(DATA_PREFIX.length()).trim());
            } else if (line.trim().isEmpty()) {
                appendFrame(frames, data);
            }
            // 其余行（event:/id:/retry:/注释）与本传输的取消息语义无关，忽略即可
        }
        appendFrame(frames, data);
        return frames;
    }

    private static void appendFrame(List<Map<String, Object>> frames, StringBuilder data) {
        if (data.length() == 0) {
            return;
        }
        frames.add(parseObject(data.toString(), "an SSE data frame"));
        data.setLength(0);
    }

    private static Map<String, Object> parseObject(String json, String what) {
        try {
            return AiJsonUtils.fromJson(json, new SerializationTypeToken<Map<String, Object>>() {
            });
        } catch (Throwable e) {
            throw new McpException(e, "The MCP server sent an ill-formed JSON as " + what + ": " + json);
        }
    }

    private static McpHttpApi createApi(McpServerConfig config) {
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        AiConfiguration.create().put(McpHttpApi.URL_CONFIG_PATH, config.getUrl()).install(factory);
        return factory.getProxyObject(McpHttpApi.class);
    }
}
