package com.luckyframework.httpclient.generalapi.ai.chat;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话请求，字段命名与层级对齐OpenAI chat/completions请求体。
 * 消息列表可被程序逐条追加，回环引擎只在 {@link #copy()} 出的副本上追加，不就地修改调用方对象。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {

    private String model;
    private List<ChatMessage> messages = new ArrayList<>();
    private Double temperature;
    private Integer maxTokens;
    private Object stop;
    private Boolean stream;
    private Object toolChoice;

    @JsonIgnore
    private List<ToolSpec> tools;

    /**
     * 回环轮次标记，仅用于框架内部控制，序列化时必须排除
     */
    @JsonIgnore
    private transient int loopRound;

    private final Map<String, Object> extra = new LinkedHashMap<>();

    public ChatRequest() {
    }

    public ChatRequest(String model) {
        this.model = model;
    }

    public ChatRequest(String model, List<ChatMessage> messages) {
        this.model = model;
        if (messages != null) {
            this.messages = new ArrayList<>(messages);
        }
    }

    /**
     * 追加一条消息
     */
    public ChatRequest addMessage(ChatMessage message) {
        this.messages.add(message);
        return this;
    }

    /**
     * 追加一条携带工具调用请求的assistant消息
     */
    public ChatRequest addAssistantToolCalls(List<ToolCall> toolCalls) {
        return addMessage(ChatMessage.assistantToolCalls(toolCalls));
    }

    /**
     * 追加一条工具执行结果消息
     */
    public ChatRequest addToolResult(String toolCallId, String result) {
        return addMessage(ChatMessage.toolResult(toolCallId, result));
    }

    /**
     * 浅复制出一个副本：消息列表新建，其余字段直接复制，用于回环追加历史而不污染调用方对象
     */
    public ChatRequest copy() {
        ChatRequest copy = new ChatRequest();
        copy.model = this.model;
        copy.messages = new ArrayList<>(this.messages);
        copy.temperature = this.temperature;
        copy.maxTokens = this.maxTokens;
        copy.stop = this.stop;
        copy.stream = this.stream;
        copy.toolChoice = this.toolChoice;
        copy.tools = this.tools;
        copy.loopRound = this.loopRound;
        copy.extra.putAll(this.extra);
        return copy;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages == null ? new ArrayList<>() : messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    @JsonProperty("max_tokens")
    public Integer getMaxTokens() {
        return maxTokens;
    }

    @JsonProperty("max_tokens")
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Object getStop() {
        return stop;
    }

    public void setStop(Object stop) {
        this.stop = stop;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    /**
     * 工具选择策略，取值为 {@code auto}/{@code none}/{@code required}，或指定某个工具的对象
     */
    @JsonProperty("tool_choice")
    public Object getToolChoice() {
        return toolChoice;
    }

    @JsonProperty("tool_choice")
    public void setToolChoice(Object toolChoice) {
        this.toolChoice = toolChoice;
    }

    @JsonIgnore
    public List<ToolSpec> getToolSpecs() {
        return tools;
    }

    @JsonIgnore
    public void setToolSpecs(Collection<ToolSpec> toolSpecs) {
        this.tools = toolSpecs == null ? null : new ArrayList<>(toolSpecs);
    }

    /**
     * 报文里的工具声明列表，按统一模型的包装形状输出
     */
    @JsonProperty("tools")
    public List<Map<String, Object>> getDeclarations() {
        if (tools == null || tools.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> declarations = new ArrayList<>(tools.size());
        for (ToolSpec spec : tools) {
            declarations.add(spec.toDeclaration());
        }
        return declarations;
    }

    @JsonProperty("tools")
    @SuppressWarnings("unchecked")
    public void setDeclarations(Collection<Map<String, Object>> declarations) {
        if (declarations == null || declarations.isEmpty()) {
            this.tools = null;
            return;
        }
        List<ToolSpec> toolSpecs = new ArrayList<>(declarations.size());
        for (Map<String, Object> declaration : declarations) {
            toolSpecs.add(ToolSpec.fromDeclaration(declaration));
        }
        this.tools = toolSpecs;
    }

    /**
     * 回环轮次，大于0表示本次调用来自回环引擎的重放
     */
    @JsonIgnore
    public int getLoopRound() {
        return loopRound;
    }

    @JsonIgnore
    public void setLoopRound(int loopRound) {
        this.loopRound = loopRound;
    }

    /**
     * 厂商专属字段的扩展容器，写入的键值在序列化时原样输出到请求体根层级
     */
    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return extra;
    }

    @JsonAnySetter
    public void putExtra(String key, Object value) {
        this.extra.put(key, value);
    }

    @Override
    public String toString() {
        return "ChatRequest{model=" + model + ", messages=" + messages.size() + ", tools=" + (tools == null ? 0 : tools.size()) + "}";
    }
}
