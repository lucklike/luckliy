package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatMessage;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRole;
import com.luckyframework.httpclient.generalapi.ai.chat.ContentPart;
import com.luckyframework.httpclient.generalapi.ai.chat.FinishReason;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.chat.Usage;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Anthropic方言：统一模型与messages/messages形态之间的翻译层。
 * <pre>
 *     请求侧差异（用 {@code ConfigurationMap} 式的Map组装，不引入第二套请求DTO）：
 *     1. {@code system} 移出 messages，合并为顶层字符串，多条system按先后顺序以换行连接；
 *     2. 工具声明放在 {@code tools[].input_schema}，不包 type 层；
 *     3. assistant的工具调用写作 {@code content[].tool_use}，工具结果写作 user 角色消息内的
 *        {@code content[].tool_result}，失败结果带 {@code is_error:true}；
 *     4. {@code max_tokens} 为必填项，业务未设置时取可配置默认值（默认1024），
 *        默认值也未配置时在发出请求前抛异常。
 *
 *     响应侧把 {@code content[]} 里的 text/tool_use 分片显式取值归一为统一模型，
 *     {@code stop_reason} 归一为统一的结束原因，{@code usage.input_tokens/output_tokens} 归一到统一用量字段。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class AnthropicDialect implements VendorAdapter {

    /**
     * 业务与方言都未给出 max_tokens 时的缺省取值
     */
    public static final int DEFAULT_MAX_TOKENS = 1024;

    /**
     * 协议版本头的默认取值，{@link AnthropicChatApi} 的注解里使用的就是它
     */
    public static final String PROTOCOL_VERSION = "2023-06-01";

    private static final String PART_TEXT = "text";
    private static final String PART_IMAGE = "image";
    private static final String PART_TOOL_USE = "tool_use";
    private static final String PART_TOOL_RESULT = "tool_result";

    private final Integer defaultMaxTokens;

    public AnthropicDialect() {
        this(DEFAULT_MAX_TOKENS);
    }

    /**
     * 指定缺省 max_tokens 的方言实例
     *
     * @param defaultMaxTokens 业务未设置最大输出token时的取值，传null表示「未配置」——此时会提前失败
     */
    public AnthropicDialect(Integer defaultMaxTokens) {
        this.defaultMaxTokens = defaultMaxTokens;
    }

    @Override
    public Object writeBody(ChatRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        List<Object> messages = new ArrayList<>();
        List<String> systemTexts = new ArrayList<>();
        for (ChatMessage message : request.getMessages()) {
            if (message.getRole() == ChatRole.SYSTEM) {
                String text = textOf(message);
                if (text != null) {
                    systemTexts.add(text);
                }
                continue;
            }
            messages.add(translateMessage(message));
        }

        putIfNotNull(body, "model", request.getModel());
        body.put("messages", messages);
        if (!systemTexts.isEmpty()) {
            body.put("system", String.join("\n", systemTexts));
        }
        putIfNotNull(body, "temperature", request.getTemperature());
        body.put("max_tokens", resolveMaxTokens(request));
        if (request.getStop() != null) {
            body.put("stop_sequences", toStopSequences(request.getStop()));
        }
        putIfNotNull(body, "stream", request.getStream());
        List<Map<String, Object>> tools = translateToolSpecs(request.getToolSpecs());
        if (!tools.isEmpty()) {
            body.put("tools", tools);
        }
        body.putAll(request.getExtra());
        return body;
    }

    /**
     * Anthropic 必填的 max_tokens：业务未设置时取方言的默认值，两侧都没有则提前失败
     */
    private Integer resolveMaxTokens(ChatRequest request) {
        Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : defaultMaxTokens;
        if (maxTokens == null) {
            throw new VendorDialectException("Anthropic请求缺少必填字段 max_tokens：业务未设置最大输出token，"
                    + "且方言未配置默认值（new AnthropicDialect(1024) 可以给出默认值）。");
        }
        return maxTokens;
    }

    private Map<String, Object> translateMessage(ChatMessage message) {
        ChatRole role = message.getRole() == null ? ChatRole.USER : message.getRole();
        Map<String, Object> node = new LinkedHashMap<>();
        if (role == ChatRole.TOOL) {
            node.put("role", ChatRole.USER.getValue());
            node.put("content", Collections.<Object>singletonList(toolResultPart(message)));
            return node;
        }
        node.put("role", role.getValue());
        List<ToolCall> toolCalls = message.getToolCalls();
        if (toolCalls != null && !toolCalls.isEmpty()) {
            List<Object> parts = new ArrayList<>();
            String text = textOf(message);
            if (text != null) {
                parts.add(textPart(text));
            }
            for (ToolCall toolCall : toolCalls) {
                parts.add(toolUsePart(toolCall));
            }
            node.put("content", parts);
        } else {
            node.put("content", translateContent(message));
        }
        return node;
    }

    /**
     * 字符串内容原样给出，分片内容逐片翻译
     */
    private Object translateContent(ChatMessage message) {
        if (message.isTextContent()) {
            return message.getContentText();
        }
        List<ContentPart> contentParts = message.getContentParts();
        if (contentParts == null) {
            return message.getContent();
        }
        List<Object> parts = new ArrayList<>(contentParts.size());
        for (ContentPart contentPart : contentParts) {
            parts.add(translateContentPart(contentPart));
        }
        return parts;
    }

    private Map<String, Object> translateContentPart(ContentPart part) {
        if (ContentPart.TYPE_IMAGE_URL.equals(part.getType())) {
            return imagePart(part);
        }
        return textPart(part.getText());
    }

    private static Map<String, Object> textPart(String text) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("type", PART_TEXT);
        part.put("text", text);
        return part;
    }

    /**
     * 图片分片：内联数据用 base64 source，公网URL用 url source
     */
    private static Map<String, Object> imagePart(ContentPart part) {
        Map<String, Object> source = new LinkedHashMap<>();
        if (part.isInlineImage()) {
            source.put("type", "base64");
            source.put("media_type", part.getInlineMimeType());
            source.put("data", part.getInlineBase64Data());
        } else {
            source.put("type", "url");
            source.put("url", part.getImageUrl() == null ? null : part.getImageUrl().getUrl());
        }
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", PART_IMAGE);
        node.put("source", source);
        return node;
    }

    private Map<String, Object> toolUsePart(ToolCall toolCall) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("type", PART_TOOL_USE);
        part.put("id", toolCall.getId());
        part.put("name", toolCall.getName());
        part.put("input", parseArgumentsObject(toolCall));
        return part;
    }

    private Map<String, Object> toolResultPart(ChatMessage message) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("type", PART_TOOL_RESULT);
        part.put("tool_use_id", message.getToolCallId());
        part.put("content", textOf(message));
        if (message.isToolError()) {
            part.put("is_error", Boolean.TRUE);
        }
        return part;
    }

    /**
     * 参数JSON文本 → 参数对象，两家要求报文里是对象而不是带转义的字符串
     */
    private Map<String, Object> parseArgumentsObject(ToolCall toolCall) {
        String arguments = toolCall.getArguments();
        if (arguments == null || arguments.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return AiJsonUtils.fromJson(arguments, new SerializationTypeToken<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new VendorDialectException("The arguments JSON of tool call [" + toolCall.getName()
                    + "] cannot be translated into an input object: " + arguments, e);
        }
    }

    private List<Map<String, Object>> translateToolSpecs(List<ToolSpec> toolSpecs) {
        if (toolSpecs == null || toolSpecs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> tools = new ArrayList<>(toolSpecs.size());
        for (ToolSpec spec : toolSpecs) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("name", spec.getName());
            node.put("description", spec.getDescription());
            node.put("input_schema", spec.getParameters());
            tools.add(node);
        }
        return tools;
    }

    private static List<Object> toStopSequences(Object stop) {
        if (stop instanceof List) {
            return new ArrayList<>((List<?>) stop);
        }
        return new ArrayList<>(Collections.singletonList(stop));
    }

    private static String textOf(ChatMessage message) {
        if (message.isTextContent()) {
            return message.getContentText();
        }
        List<ContentPart> parts = message.getContentParts();
        if (parts == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (ContentPart part : parts) {
            if (part.getText() != null) {
                text.append(part.getText());
            }
        }
        return text.length() == 0 ? null : text.toString();
    }

    private static void putIfNotNull(Map<String, Object> body, String key, Object value) {
        if (value != null) {
            body.put(key, value);
        }
    }

    @Override
    public ChatResponse parseResponse(Map<String, Object> raw) {
        StringBuilder text = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<>();
        for (Map<String, Object> part : AiMaps.toMapList(AiMaps.getList(raw, "content"))) {
            String type = AiMaps.getString(part, "type");
            if (PART_TEXT.equals(type)) {
                String delta = AiMaps.getString(part, "text");
                if (delta != null) {
                    text.append(delta);
                }
            } else if (PART_TOOL_USE.equals(type)) {
                toolCalls.add(ToolCall.of(AiMaps.getString(part, "id"), AiMaps.getString(part, "name"),
                        AiJsonUtils.toJson(AiMaps.getMap(part, "input"))));
            }
        }

        ChatMessage message = new ChatMessage();
        ChatRole role = ChatRole.of(AiMaps.getString(raw, "role"));
        message.setRole(role == null ? ChatRole.ASSISTANT : role);
        message.setContent(text.length() == 0 ? null : text.toString());
        if (!toolCalls.isEmpty()) {
            message.setToolCalls(toolCalls);
        }
        FinishReason finishReason = normalizeFinishReason(AiMaps.getString(raw, "stop_reason"), !toolCalls.isEmpty());
        List<ChatResponse.Choice> choices = new ArrayList<>(1);
        choices.add(new ChatResponse.Choice(0, message, finishReason));
        return new ChatResponse(choices, parseUsage(AiMaps.getMap(raw, "usage")));
    }

    /**
     * stop_reason 归一：tool_use → 要求调用工具，end_turn/stop_sequence → 正常结束，max_tokens → 长度上限；
     * 未给出stop_reason但出现了tool_use分片时同样归一为「要求调用工具」
     */
    private static FinishReason normalizeFinishReason(String stopReason, boolean hasToolUse) {
        if ("tool_use".equals(stopReason)) {
            return FinishReason.TOOL_CALLS;
        }
        if ("end_turn".equals(stopReason) || "stop_sequence".equals(stopReason)) {
            return FinishReason.STOP;
        }
        if ("max_tokens".equals(stopReason)) {
            return FinishReason.LENGTH;
        }
        FinishReason reason = FinishReason.of(stopReason);
        if (reason == FinishReason.UNKNOWN && hasToolUse) {
            return FinishReason.TOOL_CALLS;
        }
        return reason;
    }

    private Usage parseUsage(Map<String, Object> usageNode) {
        Usage usage = new Usage(
                AiMaps.getInteger(usageNode, "input_tokens"),
                AiMaps.getInteger(usageNode, "output_tokens"),
                AiMaps.getInteger(usageNode, "total_tokens"));
        usage.fillTotalIfAbsent();
        return usage;
    }

    @Override
    public Capabilities capabilities() {
        return Capabilities.standard();
    }

    @Override
    public StreamAccumulator newAccumulator() {
        return new AnthropicStreamAccumulator();
    }
}
