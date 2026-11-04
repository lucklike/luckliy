package com.luckyframework.httpclient.generalapi.ai.chat;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话中的一条消息。内容支持两种写法：纯字符串，或者内容分片数组；
 * assistant消息可携带零或多个工具调用，tool消息必须携带其所对应的工具调用标识。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    private ChatRole role;

    /**
     * String 或 List&lt;ContentPart&gt;
     */
    private Object content;

    private List<ToolCall> toolCalls;

    private String toolCallId;

    /**
     * 工具结果是否为失败结果。统一模型里以结果文本承载错误说明，
     * 本标志只给方言层用来输出各家要求的失败标记（如Anthropic的 {@code is_error}），不参与序列化
     */
    @JsonIgnore
    private transient boolean toolError;

    private String name;

    private final Map<String, Object> extra = new LinkedHashMap<>();

    public ChatMessage() {
    }

    private ChatMessage(ChatRole role, Object content) {
        this.role = role;
        this.content = content;
    }

    public static ChatMessage system(String text) {
        return new ChatMessage(ChatRole.SYSTEM, text);
    }

    public static ChatMessage user(String text) {
        return new ChatMessage(ChatRole.USER, text);
    }

    public static ChatMessage user(List<ContentPart> parts) {
        return new ChatMessage(ChatRole.USER, new ArrayList<Object>(parts));
    }

    public static ChatMessage assistant(String text) {
        return new ChatMessage(ChatRole.ASSISTANT, text);
    }

    /**
     * 构造一条携带工具调用请求的assistant消息
     */
    public static ChatMessage assistantToolCalls(List<ToolCall> toolCalls) {
        ChatMessage message = new ChatMessage(ChatRole.ASSISTANT, null);
        message.setToolCalls(toolCalls);
        return message;
    }

    /**
     * 构造一条工具执行结果消息
     */
    public static ChatMessage toolResult(String toolCallId, String result) {
        return toolResult(toolCallId, result, false);
    }

    /**
     * 构造一条工具执行结果消息，并标记它是否为失败结果
     */
    public static ChatMessage toolResult(String toolCallId, String result, boolean error) {
        ChatMessage message = new ChatMessage(ChatRole.TOOL, result);
        message.setToolCallId(toolCallId);
        message.setToolError(error);
        return message;
    }

    @JsonIgnore
    public boolean isToolError() {
        return toolError;
    }

    @JsonIgnore
    public void setToolError(boolean toolError) {
        this.toolError = toolError;
    }

    /**
     * 内容是否为纯字符串
     */
    @JsonIgnore
    public boolean isTextContent() {
        return content instanceof String;
    }

    /**
     * 取字符串内容，分片写法时返回null
     */
    @JsonIgnore
    public String getContentText() {
        return content instanceof String ? (String) content : null;
    }

    /**
     * 取内容分片列表，字符串写法时返回null
     */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public List<ContentPart> getContentParts() {
        return content instanceof List ? (List<ContentPart>) content : null;
    }

    public ChatRole getRole() {
        return role;
    }

    public void setRole(ChatRole role) {
        this.role = role;
    }

    public Object getContent() {
        return content;
    }

    /**
     * 设置内容。传入List时，其中的Map元素会被还原成 {@link ContentPart}，
     * 以保证从JSON反序列化后仍能得到强分片模型。
     */
    public void setContent(Object content) {
        this.content = content instanceof List ? normalizeParts((List<?>) content) : content;
    }

    private static List<Object> normalizeParts(List<?> raw) {
        List<Object> parts = new ArrayList<>(raw.size());
        for (Object element : raw) {
            parts.add(element instanceof Map ? toContentPart((Map<?, ?>) element) : element);
        }
        return parts;
    }

    private static ContentPart toContentPart(Map<?, ?> map) {
        ContentPart part = new ContentPart();
        part.setType(asString(map.get("type")));
        part.setText(asString(map.get("text")));
        Object imageUrl = map.get("image_url");
        if (imageUrl instanceof Map) {
            ContentPart.ImageUrl url = new ContentPart.ImageUrl();
            url.setUrl(asString(((Map<?, ?>) imageUrl).get("url")));
            url.setDetail(asString(((Map<?, ?>) imageUrl).get("detail")));
            part.setImageUrl(url);
        }
        return part;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    @JsonProperty("tool_calls")
    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    @JsonProperty("tool_calls")
    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    @JsonProperty("tool_call_id")
    public String getToolCallId() {
        return toolCallId;
    }

    @JsonProperty("tool_call_id")
    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 厂商专属字段的扩展容器，写入的键值在序列化时原样输出到本条消息的JSON对象内
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
        return "ChatMessage{role=" + role + ", content=" + content + ", toolCalls=" + toolCalls + ", toolCallId=" + toolCallId + "}";
    }
}
