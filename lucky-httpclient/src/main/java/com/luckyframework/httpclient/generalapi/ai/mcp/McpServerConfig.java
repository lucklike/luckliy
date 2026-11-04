package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.vendor.AiMaps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个MCP server的连接配置：stdio走「命令+参数+环境变量」，Streamable HTTP走「地址+请求头」。
 * <pre>
 *     配置一律来自配置文件或编程式配置，代码里不出现任何硬编码的命令、地址与凭据：
 *     <code>
 *     ai:
 *       mcp:
 *         weather:                       # server名，一个server一段配置
 *           command: java                # stdio：可执行程序
 *           args: ["-cp", "target/test-classes", "com.x.FakeMcpServer"]
 *           env: { TOKEN: "#{fromSecret}" }
 *           timeoutMillis: 5000
 *         remote:
 *           url: http://127.0.0.1:18080/mcp   # HTTP：地址
 *           headers: { Authorization: "Bearer xxx" }
 *     </code>
 *     读取入口：{@code McpServerConfig.from(aiConfiguration, "weather")}，
 *     其中 {@code aiConfiguration} 是 {@link AiConfiguration#load(String)} 或编程式 {@code create().put(...)} 得到的配置。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class McpServerConfig {

    /**
     * MCP配置在 {@link AiConfiguration} 里的固定挂载段
     */
    public static final String CONFIG_SECTION = "mcp";

    /**
     * 单次请求的默认超时毫秒数
     */
    public static final long DEFAULT_TIMEOUT_MILLIS = 30_000L;

    private final String serverName;
    private final List<String> command;
    private final Map<String, String> environment;
    private final String url;
    private final Map<String, String> headers;
    private final long timeoutMillis;

    private McpServerConfig(String serverName, List<String> command, Map<String, String> environment,
                            String url, Map<String, String> headers, long timeoutMillis) {
        this.serverName = serverName;
        this.command = command == null ? Collections.<String>emptyList() : command;
        this.environment = environment == null ? Collections.emptyMap() : environment;
        this.url = url;
        this.headers = headers == null ? Collections.emptyMap() : headers;
        this.timeoutMillis = timeoutMillis > 0 ? timeoutMillis : DEFAULT_TIMEOUT_MILLIS;
    }

    /**
     * 编程式配置的起点
     *
     * @param serverName server名，也是重名工具加前缀时用的名字
     * @return 配置构建器
     */
    public static Builder builder(String serverName) {
        return new Builder(serverName);
    }

    /**
     * 空配置（只有默认超时），供只需要协议行为的场景使用
     */
    public static McpServerConfig create() {
        return builder("").build();
    }

    /**
     * 从AI配置里读取某个server的连接配置
     *
     * @param config     AI配置（{@code ai} 段）
     * @param serverName server名，对应 {@code ai.mcp.<serverName>} 这一段
     * @return 连接配置
     */
    public static McpServerConfig from(AiConfiguration config, String serverName) {
        return fromMap(config.asMap(), serverName);
    }

    /**
     * 从原始配置Map里读取，键路径为 {@code mcp.<serverName>}
     *
     * @param root       配置根Map
     * @param serverName server名
     * @return 连接配置
     */
    @SuppressWarnings("unchecked")
    public static McpServerConfig fromMap(Map<String, Object> root, String serverName) {
        Object section = root.get(CONFIG_SECTION);
        Map<String, Object> serverNode = AiMaps.getMap(section instanceof Map ? (Map<String, Object>) section : null,
                serverName);
        Builder builder = builder(serverName);
        List<String> command = new ArrayList<>();
        Object commandValue = serverNode.get("command");
        if (commandValue instanceof List) {
            for (Object item : (List<Object>) commandValue) {
                command.add(String.valueOf(item));
            }
        } else if (commandValue != null) {
            command.add(String.valueOf(commandValue));
        }
        Object args = serverNode.get("args");
        if (args instanceof List) {
            for (Object item : (List<Object>) args) {
                command.add(String.valueOf(item));
            }
        } else if (args != null) {
            command.add(String.valueOf(args));
        }
        builder.command(command);
        builder.environment(stringMap(serverNode.get("env")));
        builder.url(AiMaps.getString(serverNode, "url"));
        builder.headers(stringMap(serverNode.get("headers")));
        Integer timeout = AiMaps.getInteger(serverNode, "timeoutMillis");
        if (timeout != null) {
            builder.timeoutMillis(timeout);
        }
        return builder.build();
    }

    /**
     * server名，用于工具重名时加前缀
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * stdio命令与参数，HTTP配置时为空
     */
    public List<String> getCommand() {
        return Collections.unmodifiableList(command);
    }

    public Map<String, String> getEnvironment() {
        return Collections.unmodifiableMap(environment);
    }

    /**
     * Streamable HTTP地址，stdio配置时为空
     */
    public String getUrl() {
        return url;
    }

    /**
     * Streamable HTTP的附加请求头（可能含凭据，输出日志时由框架脱敏）
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * 单次请求的超时毫秒数
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * 是否为stdio配置（命令非空）
     */
    public boolean isStdio() {
        return !command.isEmpty();
    }

    /**
     * 创建本配置对应的传输通道：有命令走stdio，有地址走Streamable HTTP
     *
     * @return 未打开的传输通道
     */
    public McpTransport createTransport() {
        if (isStdio()) {
            return new StdioTransport(command, environment);
        }
        if (url == null || url.trim().isEmpty()) {
            throw new McpException("The MCP server [" + serverName + "] has neither 'command' nor 'url' configured.");
        }
        return new StreamableHttpTransport(this);
    }

    private static Map<String, String> stringMap(Object node) {
        if (!(node instanceof Map)) {
            return Collections.emptyMap();
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
            values.put(String.valueOf(entry.getKey()), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
        }
        return values;
    }

    @Override
    public String toString() {
        // 不含请求头与环境变量的值，避免凭据进日志
        return "McpServerConfig{serverName=" + serverName + ", command=" + command
                + ", url=" + url + ", timeoutMillis=" + timeoutMillis + "}";
    }

    /**
     * 编程式配置构建器
     */
    public static class Builder {

        private final String serverName;
        private List<String> command = new ArrayList<>();
        private Map<String, String> environment = new LinkedHashMap<>();
        private String url;
        private Map<String, String> headers = new LinkedHashMap<>();
        private long timeoutMillis = DEFAULT_TIMEOUT_MILLIS;

        private Builder(String serverName) {
            this.serverName = serverName;
        }

        public Builder command(List<String> command) {
            this.command = command == null ? new ArrayList<String>() : new ArrayList<>(command);
            return this;
        }

        public Builder command(String... commandLine) {
            return command(commandLine == null ? Collections.<String>emptyList()
                    : java.util.Arrays.asList(commandLine));
        }

        public Builder environment(Map<String, String> environment) {
            this.environment = environment == null ? new LinkedHashMap<String, String>()
                    : new LinkedHashMap<>(environment);
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers == null ? new LinkedHashMap<String, String>() : new LinkedHashMap<>(headers);
            return this;
        }

        public Builder timeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public McpServerConfig build() {
            return new McpServerConfig(serverName, command, environment, url, headers, timeoutMillis);
        }
    }
}
