package com.luckyframework.httpclient.generalapi.ai.chat;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * 会话数据模型的离线断言：四类基础模型与请求/响应模型的「构造 → 序列化 → 回读」逐字段一致性。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class ChatSessionModelTest {

    private static Map<String, Object> toMap(String json) {
        return AiJsonUtils.fromJson(json, new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> asMapList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    // ------------------------------------------------------------------------------------
    // 1.2 ChatRole / FinishReason / ContentPart / Usage
    // ------------------------------------------------------------------------------------

    @Test
    public void chatRoleRoundTrip() {
        assertEquals("system", AiJsonUtils.toJson(ChatRole.SYSTEM).replace("\"", ""));
        for (ChatRole role : ChatRole.values()) {
            assertSame(role, ChatRole.of(role.getValue()));
            assertSame(role, AiJsonUtils.fromJson(AiJsonUtils.toJson(role), ChatRole.class));
        }
        assertNull(ChatRole.of("developer"));
        assertNull(ChatRole.of(null));
    }

    @Test
    public void finishReasonNormalization() {
        assertSame(FinishReason.TOOL_CALLS, FinishReason.of("tool_calls"));
        assertSame(FinishReason.STOP, FinishReason.of("stop"));
        assertSame(FinishReason.LENGTH, FinishReason.of("length"));
        assertSame(FinishReason.UNKNOWN, FinishReason.of("prohibited_content"));
        assertSame(FinishReason.UNKNOWN, FinishReason.of(null));
        assertSame(FinishReason.LENGTH, AiJsonUtils.fromJson(AiJsonUtils.toJson(FinishReason.LENGTH), FinishReason.class));
    }

    @Test
    public void contentPartRoundTrip() {
        ContentPart text = ContentPart.text("这张图里有什么？");
        Map<String, Object> textMap = toMap(AiJsonUtils.toJson(text));
        assertEquals("text", textMap.get("type"));
        assertEquals("这张图里有什么？", textMap.get("text"));
        assertFalse(textMap.containsKey("image_url"));

        ContentPart image = ContentPart.image("https://example.com/a.png");
        Map<String, Object> imageMap = toMap(AiJsonUtils.toJson(image));
        assertEquals("image_url", imageMap.get("type"));
        assertEquals("https://example.com/a.png", asMap(imageMap.get("image_url")).get("url"));
        assertFalse(image.isInlineImage());
        assertNull(image.getInlineMimeType());

        ContentPart inline = ContentPart.imageBase64("image/png", "iVBORw0KGgo=");
        Map<String, Object> inlineMap = toMap(AiJsonUtils.toJson(inline));
        assertEquals("data:image/png;base64,iVBORw0KGgo=", asMap(inlineMap.get("image_url")).get("url"));
        assertTrue(inline.isInlineImage());
        assertEquals("image/png", inline.getInlineMimeType());
        assertEquals("iVBORw0KGgo=", inline.getInlineBase64Data());

        ContentPart readBack = AiJsonUtils.fromJson(AiJsonUtils.toJson(inline), ContentPart.class);
        assertEquals("image_url", readBack.getType());
        assertEquals("data:image/png;base64,iVBORw0KGgo=", readBack.getImageUrl().getUrl());
    }

    @Test
    public void usageRoundTrip() {
        Usage usage = new Usage(18, 21, 39);
        Map<String, Object> map = toMap(AiJsonUtils.toJson(usage));
        assertEquals(18, map.get("prompt_tokens"));
        assertEquals(21, map.get("completion_tokens"));
        assertEquals(39, map.get("total_tokens"));

        Usage readBack = AiJsonUtils.fromJson(AiJsonUtils.toJson(usage), Usage.class);
        assertEquals(Integer.valueOf(18), readBack.getInputTokens());
        assertEquals(Integer.valueOf(21), readBack.getOutputTokens());
        assertEquals(Integer.valueOf(39), readBack.getTotalTokens());

        Usage partial = new Usage(25, 30, null);
        assertNull(toMap(AiJsonUtils.toJson(partial)).get("total_tokens"));
        partial.fillTotalIfAbsent();
        assertEquals(Integer.valueOf(55), partial.getTotalTokens());

        Usage absent = new Usage();
        assertTrue(toMap(AiJsonUtils.toJson(absent)).isEmpty());
    }

    // ------------------------------------------------------------------------------------
    // 1.3 ChatMessage / ChatRequest / ChatResponse
    // ------------------------------------------------------------------------------------

    @Test
    public void minimalRequestOmitsUnsetParameters() {
        ChatRequest request = new ChatRequest("deepseek-chat");
        request.addMessage(ChatMessage.system("你是一个助手"));
        request.addMessage(ChatMessage.user("北京今天天气怎么样？"));

        Map<String, Object> body = toMap(AiJsonUtils.toJson(request));
        assertEquals("deepseek-chat", body.get("model"));
        List<Map<String, Object>> messages = asMapList(body.get("messages"));
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("你是一个助手", messages.get(0).get("content"));
        assertEquals("user", messages.get(1).get("role"));

        assertFalse(body.containsKey("temperature"));
        assertFalse(body.containsKey("max_tokens"));
        assertFalse(body.containsKey("stream"));
        assertFalse(body.containsKey("tools"));
        assertFalse(body.containsKey("tool_choice"));
        assertFalse(body.containsKey("stop"));
    }

    @Test
    public void unknownFieldIsIgnored() {
        String responseJson = "{\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"你好\"},"
                + "\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":3,\"completion_tokens\":4,\"total_tokens\":7},"
                + "\"service_tier\":\"standard\",\"system_fingerprint\":\"fp_abc\"}";
        ChatResponse response = AiJsonUtils.fromJson(responseJson, ChatResponse.class);
        assertEquals("你好", response.getContent());
        assertSame(FinishReason.STOP, response.getFinishReason());
        assertEquals(Integer.valueOf(7), response.getUsage().getTotalTokens());

        String requestJson = "{\"model\":\"gpt\",\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],\"audio\":{\"voice\":\"alloy\"}}";
        ChatRequest request = AiJsonUtils.fromJson(requestJson, ChatRequest.class);
        assertEquals("gpt", request.getModel());
        assertEquals(1, request.getMessages().size());
    }

    @Test
    public void extraEntriesAreEmittedAsIs() {
        ChatRequest request = new ChatRequest("deepseek-chat");
        ChatMessage message = ChatMessage.user("北京今天天气怎么样？");
        message.putExtra("reasoning_effort", "low");
        request.addMessage(message);
        request.putExtra("response_format", Collections.singletonMap("type", "json_object"));

        Map<String, Object> body = toMap(AiJsonUtils.toJson(request));
        assertEquals("low", asMap(asMapList(body.get("messages")).get(0)).get("reasoning_effort"));
        assertEquals("json_object", asMap(body.get("response_format")).get("type"));
    }

    @Test
    public void loopRoundNeverLeaksIntoBody() {
        ChatRequest request = new ChatRequest("deepseek-chat");
        request.addMessage(ChatMessage.user("hi"));
        request.setLoopRound(3);

        String json = AiJsonUtils.toJson(request);
        assertFalse(json.contains("loopRound"));
        assertFalse(json.contains("loop_round"));
        assertEquals(0, AiJsonUtils.fromJson(json, ChatRequest.class).getLoopRound());
    }

    @Test
    public void toolCallHistoryIsRoundTrippable() {
        ChatRequest request = new ChatRequest("deepseek-chat");
        request.addMessage(ChatMessage.system("你是一个助手"));
        request.addMessage(ChatMessage.user("北京今天天气怎么样？"));
        request.addAssistantToolCalls(Collections.singletonList(
                ToolCall.of("call_001", "get_weather", "{\"city\":\"北京\"}")));
        request.addToolResult("call_001", "{\"temp\":12,\"weather\":\"晴\"}");
        request.setToolSpecs(Collections.singletonList(new ToolSpec("get_weather", "查询城市天气", weatherSchema())));
        request.setToolChoice("auto");
        request.setTemperature(0.7);
        request.setMaxTokens(1024);
        request.setStream(false);

        Map<String, Object> body = toMap(AiJsonUtils.toJson(request));
        assertEquals(Boolean.FALSE, body.get("stream"));
        assertEquals("auto", body.get("tool_choice"));
        List<Map<String, Object>> messages = asMapList(body.get("messages"));
        assertEquals(4, messages.size());
        List<Map<String, Object>> toolCalls = asMapList(messages.get(2).get("tool_calls"));
        assertEquals("call_001", toolCalls.get(0).get("id"));
        assertEquals("function", toolCalls.get(0).get("type"));
        assertEquals("get_weather", asMap(toolCalls.get(0).get("function")).get("name"));
        assertEquals("{\"city\":\"北京\"}", asMap(toolCalls.get(0).get("function")).get("arguments"));
        assertEquals("tool", messages.get(3).get("role"));
        assertEquals("call_001", messages.get(3).get("tool_call_id"));
        Map<String, Object> declaration = asMapList(body.get("tools")).get(0);
        assertEquals("function", declaration.get("type"));
        Map<String, Object> function = asMap(declaration.get("function"));
        assertEquals("查询城市天气", function.get("description"));
        assertEquals("city", asMap(asMap(function.get("parameters")).get("properties")).keySet().iterator().next());

        String json = AiJsonUtils.toJson(request);
        ChatRequest readBack = AiJsonUtils.fromJson(json, ChatRequest.class);
        assertEquals(4, readBack.getMessages().size());
        assertSame(ChatRole.ASSISTANT, readBack.getMessages().get(2).getRole());
        assertEquals("call_001", readBack.getMessages().get(2).getToolCalls().get(0).getId());
        assertEquals("get_weather", readBack.getMessages().get(2).getToolCalls().get(0).getName());
        assertEquals("{\"city\":\"北京\"}", readBack.getMessages().get(2).getToolCalls().get(0).getArguments());
        assertEquals("call_001", readBack.getMessages().get(3).getToolCallId());
        assertEquals("{\"temp\":12,\"weather\":\"晴\"}", readBack.getMessages().get(3).getContentText());
        assertEquals(1, readBack.getToolSpecs().size());
        assertEquals("get_weather", readBack.getToolSpecs().get(0).getName());
        assertEquals(Double.valueOf(0.7), readBack.getTemperature());
        assertEquals(Integer.valueOf(1024), readBack.getMaxTokens());

        assertEquals(json, AiJsonUtils.toJson(readBack));

        readBack.addMessage(ChatMessage.assistant("北京今天晴，12度。"));
        assertEquals(5, asMapList(toMap(AiJsonUtils.toJson(readBack)).get("messages")).size());
        assertEquals(json, AiJsonUtils.toJson(request));
    }

    @Test
    public void stringContentAndMixedPartContent() {
        ChatMessage plain = ChatMessage.user("你好");
        Map<String, Object> plainMap = toMap(AiJsonUtils.toJson(plain));
        assertTrue(plainMap.get("content") instanceof String);

        ChatMessage mixed = ChatMessage.user(Arrays.asList(
                ContentPart.text("这张图里有什么？"),
                ContentPart.imageBase64("image/png", "iVBORw0KGgo="),
                ContentPart.text("请简短回答")));
        Map<String, Object> mixedMap = toMap(AiJsonUtils.toJson(mixed));
        List<Map<String, Object>> parts = asMapList(mixedMap.get("content"));
        assertEquals(3, parts.size());
        assertEquals("text", parts.get(0).get("type"));
        assertEquals("image_url", parts.get(1).get("type"));
        assertEquals("text", parts.get(2).get("type"));
        assertEquals("请简短回答", parts.get(2).get("text"));

        ChatMessage readBack = AiJsonUtils.fromJson(AiJsonUtils.toJson(mixed), ChatMessage.class);
        List<ContentPart> readParts = readBack.getContentParts();
        assertEquals(3, readParts.size());
        assertEquals("这张图里有什么？", readParts.get(0).getText());
        assertEquals("data:image/png;base64,iVBORw0KGgo=", readParts.get(1).getImageUrl().getUrl());
        assertFalse(readBack.isTextContent());
    }

    // ------------------------------------------------------------------------------------
    // 1.4 副本隔离
    // ------------------------------------------------------------------------------------

    @Test
    public void copyIsolation() {
        ChatRequest origin = new ChatRequest("deepseek-chat");
        origin.addMessage(ChatMessage.system("你是一个助手"));
        origin.addMessage(ChatMessage.user("北京今天天气怎么样？"));
        origin.setLoopRound(2);
        origin.putExtra("vendor_only", "v1");

        ChatRequest copy = origin.copy();
        copy.addMessage(ChatMessage.assistantToolCalls(
                Collections.singletonList(ToolCall.of("call_001", "get_weather", "{\"city\":\"北京\"}"))));
        copy.addToolResult("call_001", "{\"temp\":12}");
        copy.setLoopRound(3);
        copy.putExtra("vendor_only", "changed");

        assertEquals(2, origin.getMessages().size());
        assertEquals(4, copy.getMessages().size());
        assertTrue(origin.getMessages() != copy.getMessages());
        assertEquals("你是一个助手", origin.getMessages().get(0).getContentText());
        assertEquals("北京今天天气怎么样？", origin.getMessages().get(1).getContentText());
        assertEquals(2, origin.getLoopRound());
        assertEquals("v1", origin.getExtra().get("vendor_only"));
        assertEquals("changed", copy.getExtra().get("vendor_only"));

        ToolSpec spec = new ToolSpec("get_weather", "查询城市天气", weatherSchema());
        origin.setToolSpecs(Collections.singletonList(spec));
        ChatRequest withTools = origin.copy();
        assertEquals(1, withTools.getToolSpecs().size());
        assertSame(spec, withTools.getToolSpecs().get(0));
    }

    // ------------------------------------------------------------------------------------
    // 夹具
    // ------------------------------------------------------------------------------------

    private static ConfigurationMap weatherSchema() {
        Map<String, Object> city = new LinkedHashMap<>();
        city.put("type", "string");
        city.put("description", "城市名");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("city", city);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", Collections.singletonList("city"));
        return new ConfigurationMap(schema);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        assertNotNull(value);
        return (Map<String, Object>) value;
    }
}
