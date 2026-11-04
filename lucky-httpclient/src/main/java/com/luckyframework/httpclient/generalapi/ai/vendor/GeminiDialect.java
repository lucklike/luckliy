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
 * Gemini方言：统一模型与generateContent报文形态之间的翻译层。
 * <pre>
 *     请求侧差异（同样用Map组装，不引入第二套请求DTO）：
 *     1. {@code system} 移出 contents，放进顶层 {@code systemInstruction.parts[].text}，
 *        多条system按先后顺序以换行连接；
 *     2. 历史消息角色映射为 user/model（统一模型的 assistant → model），工具结果固定用 user 角色；
 *     3. 工具声明放在 {@code tools[0].functionDeclarations[]}，Schema的类型名用大写枚举，
 *        由本方言内的「递归改Map树中type取值」的纯函数完成，不改ToolSchemaGenerator的输出；
 *     4. 工具调用写作 {@code parts[].functionCall}，结果写作 {@code parts[].functionResponse}（按工具名配对）；
 *     5. 模型标识在目标地址里（{@code models/{model}:generateContent}），不出现在请求体中；
 *        生成参数收敛到 {@code generationConfig}。
 *
 *     响应侧把 {@code candidates[0].content.parts[]} 里的 text/functionCall 显式取值归一为统一模型，
 *     报文里没有调用标识，归一化时合成同一响应内唯一且稳定的标识供回填报文配对。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class GeminiDialect implements VendorAdapter {

    /**
     * 合成调用标识的前缀，Gemini报文不提供id，回环靠它在统一模型里配对
     */
    public static final String SYNTHESIZED_CALL_ID_PREFIX = "gemini-call-";

    private static final String ROLE_MODEL = "model";
    private static final String PART_FUNCTION_CALL = "functionCall";
    private static final String PART_FUNCTION_RESPONSE = "functionResponse";

    @Override
    public Object writeBody(ChatRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        List<Object> contents = new ArrayList<>();
        List<String> systemTexts = new ArrayList<>();
        Map<String, String> callIdToName = new LinkedHashMap<>();

        for (ChatMessage message : request.getMessages()) {
            if (message.getRole() == ChatRole.SYSTEM) {
                String text = textOf(message);
                if (text != null) {
                    systemTexts.add(text);
                }
                continue;
            }
            if (message.getRole() == ChatRole.TOOL) {
                contents.add(functionResponseContent(message, callIdToName));
            } else {
                contents.add(translateMessage(message, callIdToName));
            }
        }

        if (!systemTexts.isEmpty()) {
            Map<String, Object> systemInstruction = new LinkedHashMap<>();
            systemInstruction.put("parts", Collections.<Object>singletonList(textPart(String.join("\n", systemTexts))));
            body.put("systemInstruction", systemInstruction);
        }
        body.put("contents", contents);
        Map<String, Object> generationConfig = translateGenerationConfig(request);
        if (!generationConfig.isEmpty()) {
            body.put("generationConfig", generationConfig);
        }
        List<Map<String, Object>> declarations = translateToolSpecs(request.getToolSpecs());
        if (!declarations.isEmpty()) {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("functionDeclarations", declarations);
            body.put("tools", Collections.<Object>singletonList(tool));
        }
        body.putAll(request.getExtra());
        return body;
    }

    private Map<String, Object> translateMessage(ChatMessage message, Map<String, String> callIdToName) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("role", translateRole(message.getRole()));
        List<Object> parts = new ArrayList<>();
        List<ToolCall> toolCalls = message.getToolCalls();
        boolean hasToolCalls = toolCalls != null && !toolCalls.isEmpty();
        if (hasToolCalls) {
            String text = textOf(message);
            if (text != null) {
                parts.add(textPart(text));
            }
            for (ToolCall toolCall : toolCalls) {
                callIdToName.put(toolCall.getId(), toolCall.getName());
                parts.add(Collections.singletonMap(PART_FUNCTION_CALL, functionCallNode(toolCall)));
            }
        } else {
            parts.addAll(translateContent(message));
        }
        node.put("parts", parts);
        return node;
    }

    /**
     * 工具结果消息：{@code user} 角色内的 functionResponse，按工具名与调用配对
     */
    private Map<String, Object> functionResponseContent(ChatMessage message, Map<String, String> callIdToName) {
        String toolName = message.getName() != null ? message.getName() : callIdToName.get(message.getToolCallId());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("name", toolName == null ? message.getToolCallId() : toolName);
        response.put("response", parseResponseObject(message));

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("role", ChatRole.USER.getValue());
        node.put("parts", Collections.<Object>singletonList(
                Collections.singletonMap(PART_FUNCTION_RESPONSE, response)));
        return node;
    }

    private static Map<String, Object> functionCallNode(ToolCall toolCall) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("name", toolCall.getName());
        node.put("args", parseArgumentsObject(toolCall));
        return node;
    }

    private static String translateRole(ChatRole role) {
        return role == ChatRole.ASSISTANT ? ROLE_MODEL : (role == null ? ChatRole.USER.getValue() : role.getValue());
    }

    /**
     * 字符串内容给出单个text分片，分片内容逐片翻译
     */
    private static List<Object> translateContent(ChatMessage message) {
        if (message.isTextContent()) {
            String text = message.getContentText();
            return text == null ? Collections.<Object>emptyList()
                    : Collections.<Object>singletonList(textPart(text));
        }
        List<ContentPart> contentParts = message.getContentParts();
        if (contentParts == null) {
            return Collections.<Object>emptyList();
        }
        List<Object> parts = new ArrayList<>(contentParts.size());
        for (ContentPart contentPart : contentParts) {
            parts.add(translateContentPart(contentPart));
        }
        return parts;
    }

    private static Map<String, Object> translateContentPart(ContentPart part) {
        if (ContentPart.TYPE_IMAGE_URL.equals(part.getType())) {
            return imagePart(part);
        }
        return textPart(part.getText());
    }

    private static Map<String, Object> textPart(String text) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("text", text);
        return part;
    }

    /**
     * 图片分片：内联数据走 inlineData，公网URL走 fileData
     */
    private static Map<String, Object> imagePart(ContentPart part) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (part.isInlineImage()) {
            data.put("mimeType", part.getInlineMimeType());
            data.put("data", part.getInlineBase64Data());
            return Collections.singletonMap("inlineData", data);
        }
        data.put("fileUri", part.getImageUrl() == null ? null : part.getImageUrl().getUrl());
        return Collections.singletonMap("fileData", data);
    }

    private static Map<String, Object> translateGenerationConfig(ChatRequest request) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (request.getTemperature() != null) {
            config.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            config.put("maxOutputTokens", request.getMaxTokens());
        }
        if (request.getStop() != null) {
            config.put("stopSequences", toStopSequences(request.getStop()));
        }
        return config;
    }

    private List<Map<String, Object>> translateToolSpecs(List<ToolSpec> toolSpecs) {
        if (toolSpecs == null || toolSpecs.isEmpty()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> declarations = new ArrayList<>(toolSpecs.size());
        for (ToolSpec spec : toolSpecs) {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("name", spec.getName());
            node.put("description", spec.getDescription());
            node.put("parameters", toUppercaseTypes(spec.getParameters()));
            declarations.add(node);
        }
        return declarations;
    }

    /**
     * 递归把Map树里 {@code type} 的取值改成大写形式，Gemini的Schema类型是大写枚举。
     * ToolSchemaGenerator的输出保持小写，本变换只发生在Gemini方言内。
     *
     * @param node 任意JSON节点
     * @return 类型取值已改为大写的节点副本
     */
    @SuppressWarnings("unchecked")
    static Object toUppercaseTypes(Object node) {
        if (node instanceof Map) {
            Map<String, Object> source = (Map<String, Object>) node;
            Map<String, Object> converted = new LinkedHashMap<>(source.size());
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                converted.put(key, "type".equals(key) && value instanceof String
                        ? ((String) value).toUpperCase() : toUppercaseTypes(value));
            }
            return converted;
        }
        if (node instanceof List) {
            List<Object> source = (List<Object>) node;
            List<Object> converted = new ArrayList<>(source.size());
            for (Object element : source) {
                converted.add(toUppercaseTypes(element));
            }
            return converted;
        }
        return node;
    }

    /**
     * 参数JSON文本 → 参数对象，报文里要求是对象而不是带转义的字符串
     */
    private static Map<String, Object> parseArgumentsObject(ToolCall toolCall) {
        String arguments = toolCall.getArguments();
        if (arguments == null || arguments.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return AiJsonUtils.fromJson(arguments, new SerializationTypeToken<Map<String, Object>>() {
            });
        } catch (Exception e) {
            throw new VendorDialectException("The arguments JSON of tool call [" + toolCall.getName()
                    + "] cannot be translated into an args object: " + arguments, e);
        }
    }

    /**
     * 工具结果文本 → response对象：本身是JSON对象时原样给出，否则以content分片承载文本
     */
    private static Map<String, Object> parseResponseObject(ChatMessage message) {
        String text = textOf(message);
        if (text == null || text.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> parsed = AiJsonUtils.fromJson(text, new SerializationTypeToken<Map<String, Object>>() {
            });
            if (parsed != null) {
                return parsed;
            }
        } catch (Exception ignored) {
            // 不是JSON对象的结果文本，按原文本承载
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", text);
        return response;
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

    @Override
    public ChatResponse parseResponse(Map<String, Object> raw) {
        List<Object> candidates = AiMaps.getList(raw, "candidates");
        List<Map<String, Object>> candidateNodes = AiMaps.toMapList(candidates);
        Map<String, Object> candidateNode = candidateNodes.isEmpty() ? Collections.<String, Object>emptyMap() : candidateNodes.get(0);
        Map<String, Object> contentNode = AiMaps.getMap(candidateNode, "content");
        List<Object> parts = AiMaps.getList(contentNode, "parts");

        StringBuilder text = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<>();
        for (Map<String, Object> part : AiMaps.toMapList(parts)) {
            String delta = AiMaps.getString(part, "text");
            if (delta != null) {
                text.append(delta);
            }
            if (part.get(PART_FUNCTION_CALL) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> functionCall = (Map<String, Object>) part.get(PART_FUNCTION_CALL);
                String name = AiMaps.getString(functionCall, "name");
                toolCalls.add(ToolCall.of(synthesizeCallId(toolCalls.size(), name), name,
                        AiJsonUtils.toJson(AiMaps.getMap(functionCall, "args"))));
            }
        }

        ChatMessage message = new ChatMessage();
        ChatRole role = ChatRole.of(AiMaps.getString(contentNode, "role"));
        message.setRole(role == null ? ChatRole.ASSISTANT : role);
        message.setContent(text.length() == 0 ? null : text.toString());
        if (!toolCalls.isEmpty()) {
            message.setToolCalls(toolCalls);
        }
        FinishReason finishReason = normalizeFinishReason(
                AiMaps.getString(candidateNode, "finishReason"), !toolCalls.isEmpty());
        List<ChatResponse.Choice> choices = new ArrayList<>(1);
        choices.add(new ChatResponse.Choice(0, message, finishReason));
        return new ChatResponse(choices, parseUsage(AiMaps.getMap(raw, "usageMetadata")));
    }

    /**
     * Gemini报文不提供调用标识，合成同一响应内唯一且稳定的标识（同一片段序列重复解析结果一致）
     */
    private static String synthesizeCallId(int index, String toolName) {
        return SYNTHESIZED_CALL_ID_PREFIX + index + "-" + toolName;
    }

    /**
     * finishReason归一：出现functionCall时一律表达为「要求调用工具」，与该厂商取值无关；
     * STOP → 正常结束，MAX_TOKENS → 长度上限
     */
    private static FinishReason normalizeFinishReason(String finishReason, boolean hasFunctionCall) {
        if (hasFunctionCall) {
            return FinishReason.TOOL_CALLS;
        }
        if ("MAX_TOKENS".equals(finishReason)) {
            return FinishReason.LENGTH;
        }
        return FinishReason.of(finishReason);
    }

    private Usage parseUsage(Map<String, Object> usageNode) {
        Usage usage = new Usage(
                AiMaps.getInteger(usageNode, "promptTokenCount"),
                AiMaps.getInteger(usageNode, "candidatesTokenCount"),
                AiMaps.getInteger(usageNode, "totalTokenCount"));
        usage.fillTotalIfAbsent();
        return usage;
    }

    @Override
    public Capabilities capabilities() {
        return Capabilities.standard();
    }

    @Override
    public StreamAccumulator newAccumulator() {
        return new GeminiStreamAccumulator();
    }
}
