package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRole;
import com.luckyframework.httpclient.generalapi.ai.chat.FinishReason;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * OpenAI兼容簇方言的 golden-file 断言：请求侧逐字段比对录制样本，响应侧断言三种归一结果。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class OpenAIDialectTest {

    private final OpenAIDialect dialect = new OpenAIDialect();

    private static Map<String, Object> toMap(String json) {
        return AiJsonUtils.fromJson(json, new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object node) {
        return (List<Map<String, Object>>) node;
    }

    // ------------------------------------------------------------------------------------
    // 1.7 writeBody
    // ------------------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    public void writeBodyMatchesGoldenFile() {
        Object body = dialect.writeBody(AiFixtures.weatherRequest());
        Map<String, Object> actual = toMap(AiJsonUtils.toJson(body));
        Map<String, Object> golden = AiFixtures.jsonResource("ai/openai/request-with-tools.json");
        assertEquals(golden, actual);

        // 本轮不写入 parallel_tool_calls
        assertFalse(actual.containsKey("parallel_tool_calls"));

        List<Map<String, Object>> tools = mapList(actual.get("tools"));
        assertEquals(1, tools.size());
        assertEquals("function", tools.get(0).get("type"));
        Map<String, Object> function = (Map<String, Object>) tools.get(0).get("function");
        assertEquals("get_weather", function.get("name"));
        assertEquals("city", mapKeys((Map<String, Object>) ((Map<String, Object>) function.get("parameters")).get("properties")));
    }

    @Test
    public void extraIsMergedIntoBody() {
        ChatRequest request = AiFixtures.minimalRequest();
        request.putExtra("response_format", "json_object");
        Map<String, Object> actual = toMap(AiJsonUtils.toJson(dialect.writeBody(request)));
        assertEquals("json_object", actual.get("response_format"));
        assertEquals(2, mapList(actual.get("messages")).size());
        assertFalse(actual.containsKey("tools"));
    }

    // ------------------------------------------------------------------------------------
    // 1.8 parseResponse
    // ------------------------------------------------------------------------------------

    @Test
    public void parsePlainTextResponse() {
        ChatResponse response = dialect.parseResponse(AiFixtures.jsonResource("ai/openai/response-text.json"));
        assertEquals("北京今天晴，12 度。", response.getContent());
        assertSame(ChatRole.ASSISTANT, response.getFirstChoice().getMessage().getRole());
        assertSame(FinishReason.STOP, response.getFinishReason());
        assertFalse(response.hasToolCalls());
        assertEquals(Integer.valueOf(39), response.getUsage().getTotalTokens());
    }

    @Test
    public void parseTwoToolCallsResponse() {
        ChatResponse response = dialect.parseResponse(AiFixtures.jsonResource("ai/openai/response-two-tool-calls.json"));
        assertSame(FinishReason.TOOL_CALLS, response.getFinishReason());
        assertNull(response.getContent());
        List<ToolCall> toolCalls = response.getToolCalls();
        assertEquals(2, toolCalls.size());
        assertEquals("call_001", toolCalls.get(0).getId());
        assertEquals("get_weather", toolCalls.get(0).getName());
        assertEquals("{\"city\":\"北京\"}", toolCalls.get(0).getArguments());
        assertEquals("call_002", toolCalls.get(1).getId());
        assertEquals("{\"city\":\"上海\"}", toolCalls.get(1).getArguments());
    }

    @Test
    public void parseResponseWithoutUsageAndWithUnknownFields() {
        ChatResponse response = dialect.parseResponse(AiFixtures.jsonResource("ai/openai/response-no-usage.json"));
        assertEquals("我不会查天气。", response.getContent());
        assertSame(FinishReason.STOP, response.getFinishReason());
        assertNull(response.getUsage().getInputTokens());
        assertNull(response.getUsage().getOutputTokens());
        assertNull(response.getUsage().getTotalTokens());
    }

    @Test
    public void capabilitiesAndAccumulatorAreExposedThroughTheDialect() {
        Capabilities capabilities = dialect.capabilities();
        assertTrue(capabilities.isToolCallSupported());
        assertTrue(capabilities.isStreamToolCallSupported());
        assertFalse(capabilities.isParallelToolCallsSupported());
        assertTrue(dialect.newAccumulator() instanceof OpenAIStreamAccumulator);
    }

    private static String mapKeys(Map<String, Object> map) {
        return String.join(",", map.keySet());
    }
}
