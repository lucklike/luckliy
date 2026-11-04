package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolExecutor;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolResult;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;
import com.luckyframework.httpclient.generalapi.ai.vendor.AiMaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * MCP工具桥接：把 {@code tools/list} 拉回来的远端工具登记成本地工具声明，
 * 并把 {@code tools/call} 包装成 {@link ToolExecutor}。
 * 对回环引擎完全透明——引擎只看到一条普通 {@link ToolSpec}，
 * 远端工具与本地注解工具在注册表、报文工具列表与回环行为上不可区分。
 * <pre>
 *     重名策略：默认抛冲突异常（由 {@link ToolRegistry} 表达），
 *     需要共存时用 {@link #prefix(String)} 选项，注册名带server名前缀，
 *     而远端 {@code tools/call} 永远使用其原始名。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @see McpClient
 */
public class McpToolBridge {

    /**
     * 注册名与远端原始名之间的分隔符
     */
    public static final String NAME_SEPARATOR = "_";

    private static final String PART_TYPE_TEXT = "text";

    private final McpClient client;
    private final String namePrefix;

    public McpToolBridge(McpClient client) {
        this(client, null);
    }

    private McpToolBridge(McpClient client, String namePrefix) {
        if (client == null) {
            throw new McpException("The MCP client is required to build a tool bridge.");
        }
        this.client = client;
        this.namePrefix = namePrefix;
    }

    /**
     * 以server名作为注册名前缀，用于远端工具与本地工具重名时共存
     *
     * @param serverName 前缀名，通常取 {@link McpClient#getServerName()} 或配置里的server名
     * @return 带前缀的桥接实例，原实例不受影响
     */
    public McpToolBridge prefix(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            throw new McpException("The MCP tool name prefix cannot be empty.");
        }
        return new McpToolBridge(client, serverName + NAME_SEPARATOR);
    }

    /**
     * 拉取远端工具清单并全部登记（同名冲突时抛异常）
     *
     * @param registry 目标注册表
     * @return 本次登记的本地工具声明，顺序与远端清单一致
     */
    public List<ToolSpec> registerAll(ToolRegistry registry) {
        return registerAll(registry, false);
    }

    /**
     * 拉取远端工具清单并全部登记
     *
     * @param registry  目标注册表
     * @param overwrite 远端工具与已登记工具同名时是否覆盖
     * @return 本次登记的本地工具声明，顺序与远端清单一致
     */
    public List<ToolSpec> registerAll(ToolRegistry registry, boolean overwrite) {
        List<ToolSpec> registered = new ArrayList<>();
        if (!client.serverHasTools()) {
            McpLogs.warn("The MCP server [{}] declares no 'tools' capability, registerAll() registers nothing.",
                    client.getServerName());
            return registered;
        }
        for (Map<String, Object> tool : client.listTools()) {
            String remoteName = AiMaps.getString(tool, "name");
            if (remoteName == null || remoteName.trim().isEmpty()) {
                throw new McpException("The MCP server [" + client.getServerName()
                        + "] returned a tool without a name: " + tool);
            }
            String localName = namePrefix == null ? remoteName : namePrefix + remoteName;
            registry.register(toToolSpec(localName, tool), executorOf(remoteName), overwrite);
            registered.add(registry.getSpec(localName));
        }
        McpLogs.info("The MCP server [{}] registered {} remote tool(s): {}", client.getServerName(),
                registered.size(), namesOf(registered));
        return registered;
    }

    /**
     * 远端工具声明转本地声明：name/description原样沿用，inputSchema原样保留，缺失时补空object schema
     */
    @SuppressWarnings("unchecked")
    public static ToolSpec toToolSpec(String localName, Map<String, Object> tool) {
        Object inputSchema = tool.get("inputSchema");
        ConfigurationMap parameters = inputSchema instanceof Map
                ? new ConfigurationMap((Map<String, Object>) inputSchema)
                : ToolSpec.emptyParameters();
        return new ToolSpec(localName, AiMaps.getString(tool, "description"), parameters);
    }

    /**
     * 把 {@code tools/call} 包装成工具执行实现
     *
     * @param remoteName 远端原始工具名，请求里永远用它，与本地注册名无关
     * @return 执行实现
     */
    public ToolExecutor executorOf(final String remoteName) {
        return new ToolExecutor() {
            @Override
            public Object execute(Map<String, Object> args) {
                return toToolResult(remoteName, client.callTool(remoteName, args));
            }
        };
    }

    /**
     * 把远端结果转成工具结果：text分片按序换行连接，{@code isError} 与JSON-RPC错误都转成失败结果，
     * 非text分片以可识别的占位说明保留，不静默丢弃
     *
     * @param remoteName 远端原始工具名，只用于占位说明与空结果诊断
     * @param result     {@code tools/call} 的result节点
     * @return 成/败两态的工具结果，交由回环引擎回填，不抛给业务方
     */
    @SuppressWarnings("unchecked")
    public static ToolResult toToolResult(String remoteName, Map<String, Object> result) {
        List<Object> parts = AiMaps.getList(result, "content");
        List<String> texts = new ArrayList<>();
        for (Object part : parts) {
            Map<String, Object> node = part instanceof Map
                    ? (Map<String, Object>) part
                    : Collections.<String, Object>emptyMap();
            String type = AiMaps.getString(node, "type");
            texts.add(PART_TYPE_TEXT.equals(type) ? AiMaps.getString(node, "text") : unsupportedPart(type, node));
        }
        if (texts.isEmpty()) {
            texts.add("远端工具 [" + remoteName + "] 未返回任何内容分片。");
        }
        String content = join(texts);
        return isFailure(result) ? ToolResult.failure(content) : ToolResult.success(content);
    }

    /**
     * 非text分片的占位说明：保留分片可见性，MUST NOT 静默丢弃
     */
    private static String unsupportedPart(String type, Map<String, Object> part) {
        String mimeType = AiMaps.getString(part, "mimeType");
        return "[非文本分片 type=" + (type == null ? "unknown" : type)
                + (mimeType == null ? "" : ", mimeType=" + mimeType) + "，本版本不解析其内容]";
    }

    /**
     * isError为true即业务失败；JSON-RPC层错误由 {@link McpClient#callTool} 标记成同样的结构
     */
    private static boolean isFailure(Map<String, Object> result) {
        Object flag = result.get("isError");
        return Boolean.TRUE.equals(flag) || "true".equalsIgnoreCase(String.valueOf(flag));
    }

    private static String join(List<String> texts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < texts.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(texts.get(i) == null ? "null" : texts.get(i));
        }
        return builder.toString();
    }

    private static List<String> namesOf(List<ToolSpec> specs) {
        List<String> names = new ArrayList<>(specs.size());
        for (ToolSpec spec : specs) {
            names.add(spec.getName());
        }
        return names;
    }
}
