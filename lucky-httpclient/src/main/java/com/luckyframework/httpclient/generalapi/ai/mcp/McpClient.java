package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.vendor.AiMaps;
import com.luckyframework.httpclient.proxy.Version;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP客户端：只实现 tools 能力面所需的五个消息（initialize / notifications/initialized /
 * tools/list / tools/call / ping），其余方法一律「未知通知容忍、未知响应报错」。
 * <pre>
 *     会话生命周期：
 *     1. {@link #initialize()} 发 initialize 请求，收到响应后再发 notifications/initialized 通知；
 *     2. 握手完成前任何工具相关请求都会抛出「会话未初始化」，且不会发出该请求；
 *     3. server返回的协议版本与请求不一致时，采用server返回的版本继续会话；
 *     4. server的 capabilities 里没有 tools 时不注册任何远端工具，只给出可诊断说明。
 *
 *     客户端信息里的版本号取自 lucky-httpclient 自身版本（{@link Version#LUCKY_VERSION}），
 *     不额外硬编码，连接的命令/地址/凭据一律来自 {@link McpServerConfig}。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @see McpToolBridge
 * @see McpServerConfig
 */
public class McpClient implements Closeable {

    /**
     * 客户端要求的协议版本（本实现按该版本的语义收发）
     */
    public static final String REQUESTED_PROTOCOL_VERSION = "2025-06-18";

    /**
     * 客户端名称，写进 initialize 的 clientInfo
     */
    public static final String CLIENT_NAME = "lucky-httpclient";

    private static final String METHOD_INITIALIZE = "initialize";
    private static final String METHOD_INITIALIZED_NOTIFICATION = "notifications/initialized";
    private static final String METHOD_TOOLS_LIST = "tools/list";
    private static final String METHOD_TOOLS_CALL = "tools/call";
    private static final String METHOD_PING = "ping";

    /**
     * tools/list 分页拉取的最大页数，防止 server 一直返回游标时无限循环
     */
    private static final int MAX_PAGES = 100;

    private final JsonRpcChannel channel;
    private final McpServerConfig config;

    private volatile boolean initialized;
    private volatile String protocolVersion = REQUESTED_PROTOCOL_VERSION;
    private volatile Map<String, Object> serverCapabilities = Collections.emptyMap();
    private volatile String serverName;
    private volatile String serverVersion;

    public McpClient(McpTransport transport, McpServerConfig config) {
        this.config = config == null ? McpServerConfig.create() : config;
        // HTTP传输发现会话失效（404）时需要客户端重新握手，把这只能力交给它
        if (transport instanceof StreamableHttpTransport) {
            ((StreamableHttpTransport) transport).setReseeder(this::reInitialize);
        }
        this.channel = new JsonRpcChannel(transport)
                .timeout(this.config.getTimeoutMillis())
                // stdio超时必须回收子进程；HTTP是无状态的一请求一响应，超时不该顺手关掉整个会话
                .closeOnTimeout(!(transport instanceof StreamableHttpTransport));
    }

    /**
     * 供已经完成握手的上层（如测试）直接接管通道
     */
    McpClient(JsonRpcChannel channel, McpServerConfig config) {
        this.channel = channel;
        this.config = config == null ? McpServerConfig.create() : config;
    }

    /**
     * 初始化握手：initialize 请求 → 响应 → notifications/initialized 通知
     *
     * @return server声明的能力集合
     */
    public synchronized Map<String, Object> initialize() {
        if (initialized) {
            return serverCapabilities;
        }
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocolVersion", REQUESTED_PROTOCOL_VERSION);
        params.put("capabilities", new LinkedHashMap<String, Object>());
        params.put("clientInfo", clientInfo());

        Map<String, Object> response = channel.request(METHOD_INITIALIZE, params);
        JsonRpcMessage<Object> message = JsonRpcMessage.fromFrame(response);
        if (message == null || !message.isResponse()) {
            throw new McpException("The MCP server did not answer the initialize request.");
        }
        if (message.isError()) {
            throw new McpException("The MCP server rejected the initialize request: " + message.getError());
        }
        Map<String, Object> result = message.getResultMap();
        String agreed = AiMaps.getString(result, "protocolVersion");
        // server返回的版本与请求不一致时采用server的版本，绝不混用两个版本的行为
        this.protocolVersion = agreed == null ? REQUESTED_PROTOCOL_VERSION : agreed;
        this.serverCapabilities = AiMaps.getMap(result, "capabilities");
        Map<String, Object> info = AiMaps.getMap(result, "serverInfo");
        this.serverName = AiMaps.getString(info, "name");
        this.serverVersion = AiMaps.getString(info, "version");

        // 通知没有id，也不等待任何回执
        channel.notify(METHOD_INITIALIZED_NOTIFICATION, null);
        initialized = true;
        if (!serverHasTools()) {
            McpLogs.warn("The MCP server [{}] declares no 'tools' capability, so no remote tool will be registered.",
                    serverName);
        }
        return serverCapabilities;
    }

    /**
     * 拉取远端工具清单，带 nextCursor 时按 cursor 续拉直至取完
     *
     * @return 远端工具声明（name/description/inputSchema），顺序与远端一致
     * @throws McpException 会话未初始化时抛出
     */
    public List<Map<String, Object>> listTools() {
        ensureInitialized(METHOD_TOOLS_LIST);
        List<Map<String, Object>> tools = new ArrayList<>();
        String cursor = null;
        // 分页取全：响应带nextCursor时必须继续请求，直到不再带游标
        for (int page = 0; page < MAX_PAGES; page++) {
            Map<String, Object> params = new LinkedHashMap<>();
            if (cursor != null) {
                params.put("cursor", cursor);
            }
            JsonRpcMessage<Object> message = JsonRpcMessage.fromFrame(channel.request(METHOD_TOOLS_LIST, params));
            Map<String, Object> result = resultOrThrow(message, METHOD_TOOLS_LIST);
            for (Object tool : AiMaps.getList(result, "tools")) {
                if (tool instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cast = (Map<String, Object>) tool;
                    tools.add(cast);
                }
            }
            cursor = AiMaps.getString(result, "nextCursor");
            if (cursor == null || cursor.isEmpty()) {
                return tools;
            }
        }
        throw new McpException("The MCP server keeps returning nextCursor, tools/list stopped after "
                + MAX_PAGES + " pages.");
    }

    /**
     * 调用远端工具，返回 {@code tools/call} 的 result 对象（由 {@link McpToolBridge} 转成工具结果）
     *
     * @param toolName  远端原始工具名
     * @param arguments 参数对象
     * @return result节点，含 content 与 isError
     * @throws McpException 会话未初始化或传输层失败（超时、断开）时抛出；
     *                      JSON-RPC error与业务失败都不走异常，而是留在返回的结构里
     */
    public Map<String, Object> callTool(String toolName, Map<String, Object> arguments) {
        ensureInitialized(METHOD_TOOLS_CALL);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", toolName);
        params.put("arguments", arguments == null ? new LinkedHashMap<String, Object>() : arguments);
        JsonRpcMessage<Object> message = JsonRpcMessage.fromFrame(channel.request(METHOD_TOOLS_CALL, params));
        if (message == null) {
            throw new McpException("The MCP server returned an ill-formed response for tools/call.");
        }
        if (message.isError()) {
            // JSON-RPC层错误也交给桥接层转成失败结果，不在这里抛给业务方
            Map<String, Object> errorAsResult = new LinkedHashMap<>();
            List<Object> content = new ArrayList<>();
            Map<String, Object> text = new LinkedHashMap<>();
            text.put("type", "text");
            text.put("text", "JSON-RPC error " + message.getError().getCode() + ": " + message.getError().getMessage());
            content.add(text);
            errorAsResult.put("content", content);
            errorAsResult.put("isError", true);
            errorAsResult.put("jsonRpcError", true);
            return errorAsResult;
        }
        return message.getResultMap();
    }

    /**
     * 可用性探测：收到 {@code result: {}} 即视为成功
     *
     * @return 探测是否成功；无响应或超时返回false
     */
    public boolean ping() {
        if (!initialized) {
            throw McpException.notInitialized(METHOD_PING);
        }
        try {
            JsonRpcMessage<Object> message = JsonRpcMessage.fromFrame(channel.request(METHOD_PING, null));
            return message != null && !message.isError();
        } catch (McpException e) {
            McpLogs.warn("The MCP ping failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 握手是否已完成
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 协商后的协议版本（server返回的版本优先）
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * server声明的能力集合，握手完成前为空
     */
    public Map<String, Object> getServerCapabilities() {
        return serverCapabilities;
    }

    /**
     * server是否声明了tools能力；未声明时不注册任何远端工具
     */
    public boolean serverHasTools() {
        return serverCapabilities.containsKey("tools");
    }

    /**
     * server名字，用于重名工具的前缀注册
     */
    public String getServerName() {
        return serverName;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public McpServerConfig getConfig() {
        return config;
    }

    /**
     * 关闭会话并回收通道资源（stdio场景即回收子进程）
     */
    @Override
    public void close() {
        channel.close();
    }

    /**
     * 重新发起握手（Streamable HTTP传输发现会话失效时会用到）
     */
    synchronized void reInitialize() {
        initialized = false;
        initialize();
    }

    JsonRpcChannel getChannel() {
        return channel;
    }

    private void ensureInitialized(String method) {
        if (!initialized) {
            throw McpException.notInitialized(method);
        }
    }

    private static Map<String, Object> resultOrThrow(JsonRpcMessage<Object> message, String method) {
        if (message == null) {
            throw new McpException("The MCP server returned an ill-formed response for " + method + ".");
        }
        if (message.isError()) {
            throw new McpException("The MCP server rejected '" + method + "': " + message.getError());
        }
        return message.getResultMap();
    }

    private Map<String, Object> clientInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", CLIENT_NAME);
        info.put("version", Version.LUCKY_VERSION);
        return info;
    }
}
