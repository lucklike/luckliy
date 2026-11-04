package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.chat.ChatMessage;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRole;
import com.luckyframework.httpclient.generalapi.ai.chat.FinishReason;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.chat.Usage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI兼容簇方言（DeepSeek、通义、Kimi、智谱、豆包、Ollama等）。
 * 统一模型的形状即本簇的请求体形状，因此请求侧原样序列化，不做Map重组；
 * 响应侧仍然显式取字段构建统一模型，保证「缺usage」「未知字段」等契约有确定落点。
 * 兼容簇厂商只需要更换 base-url、模型标识与鉴权配置，报文结构完全一致，不存在独立分支。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class OpenAIDialect implements VendorAdapter {

    @Override
    public Object writeBody(ChatRequest request) {
        return request;
    }

    @Override
    public ChatResponse parseResponse(Map<String, Object> raw) {
        List<ChatResponse.Choice> choices = new ArrayList<>();
        for (Map<String, Object> choiceNode : AiMaps.toMapList(AiMaps.getList(raw, "choices"))) {
            choices.add(parseChoice(choiceNode));
        }
        return new ChatResponse(choices, parseUsage(AiMaps.getMap(raw, "usage")));
    }

    private ChatResponse.Choice parseChoice(Map<String, Object> choiceNode) {
        Map<String, Object> messageNode = AiMaps.getMap(choiceNode, "message");
        ChatMessage message = parseMessage(messageNode);
        FinishReason finishReason = FinishReason.of(AiMaps.getString(choiceNode, "finish_reason"));
        return new ChatResponse.Choice(AiMaps.getInteger(choiceNode, "index"), message, finishReason);
    }

    private ChatMessage parseMessage(Map<String, Object> messageNode) {
        ChatMessage message = new ChatMessage();
        ChatRole role = ChatRole.of(AiMaps.getString(messageNode, "role"));
        message.setRole(role == null ? ChatRole.ASSISTANT : role);
        message.setContent(messageNode.get("content"));
        List<ToolCall> toolCalls = parseToolCalls(messageNode);
        if (!toolCalls.isEmpty()) {
            message.setToolCalls(toolCalls);
        }
        message.setToolCallId(AiMaps.getString(messageNode, "tool_call_id"));
        return message;
    }

    private List<ToolCall> parseToolCalls(Map<String, Object> messageNode) {
        List<ToolCall> toolCalls = new ArrayList<>();
        for (Map<String, Object> callNode : AiMaps.toMapList(AiMaps.getList(messageNode, "tool_calls"))) {
            Map<String, Object> function = AiMaps.getMap(callNode, "function");
            toolCalls.add(ToolCall.of(
                    AiMaps.getString(callNode, "id"),
                    AiMaps.getString(function, "name"),
                    AiMaps.getString(function, "arguments")));
        }
        return toolCalls;
    }

    private Usage parseUsage(Map<String, Object> usageNode) {
        Usage usage = new Usage(
                AiMaps.getInteger(usageNode, "prompt_tokens"),
                AiMaps.getInteger(usageNode, "completion_tokens"),
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
        return new OpenAIStreamAccumulator();
    }
}
