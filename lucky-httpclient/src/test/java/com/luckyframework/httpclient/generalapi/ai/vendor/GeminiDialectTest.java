package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.common.ConfigurationMap;
import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatMessage;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRole;
import com.luckyframework.httpclient.generalapi.ai.chat.ContentPart;
import com.luckyframework.httpclient.generalapi.ai.chat.FinishReason;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.chat.Usage;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
 * Gemini方言的 golden-file 断言：请求侧比对 systemInstruction/contents/functionDeclarations 的形状、
 * 类型名大写与角色映射，响应侧断言 functionCall 归一化、调用标识合成与 usageMetadata 归一，
 * 并验证目标地址按模型标识生成。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class GeminiDialectTest {

    private static final String MODEL = "gemini-2.0-flash";
    private static final String GENERATE_CONTENT_PATH = "/models/" + MODEL + ":generateContent";
    private static final String CONFIG_LOCATION = "classpath:ai/gemini/api-config.yml";
    private static final String CONFIGURED_API_KEY = "gemini-config-file-key";

    private final GeminiDialect dialect = new GeminiDialect();

    private TestHttpServer server;

    @Before
    public void setUp() throws IOException {
        server = new TestHttpServer();
    }

    @After
    public void tearDown() {
        server.close();
    }

    private static Map<String, Object> toMap(String json) {
        return AiJsonUtils.fromJson(json, new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    private Map<String, Object> bodyOf(ChatRequest request) {
        return toMap(AiJsonUtils.toJson(dialect.writeBody(request)));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object node) {
        return (List<Map<String, Object>>) node;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object node) {
        return (Map<String, Object>) node;
    }

    private HttpClientProxyObjectFactory factory() {
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        AiConfiguration.load(CONFIG_LOCATION)
                .put("gemini.baseUrl", server.baseUrl() + "/")
                .install(factory);
        return factory;
    }

    // ------------------------------------------------------------------------------------
    // 3.3 writeBody
    // ------------------------------------------------------------------------------------

    /**
     * 一轮完整往返的报文形状：systemInstruction顶层、functionDeclarations位置、user/model角色映射
     */
    @Test
    public void requestBodyMatchesGoldenFile() {
        Map<String, Object> actual = bodyOf(AiFixtures.weatherRequest());
        assertEquals(AiFixtures.jsonResource("ai/gemini/request-with-tools.json"), actual);

        List<Map<String, Object>> contents = mapList(actual.get("contents"));
        assertEquals(3, contents.size());
        assertEquals("model", contents.get(1).get("role"));
        assertFalse("system不允许出现在contents里", containsRole(contents, ChatRole.SYSTEM.getValue()));
        assertFalse("工具结果固定用user角色", containsRole(contents, ChatRole.TOOL.getValue()));

        Map<String, Object> declaration = mapList(mapList(actual.get("tools")).get(0).get("functionDeclarations")).get(0);
        assertEquals("get_weather", declaration.get("name"));
        assertEquals("OBJECT", map(declaration.get("parameters")).get("type"));
    }

    /**
     * 多条system合并进systemInstruction；分片翻译为inlineData/fileData；失败结果以文本承载
     */
    @Test
    public void systemMergeAndContentParts() {
        ChatRequest request = new ChatRequest(MODEL);
        request.addMessage(ChatMessage.system("你是助手A"));
        request.addMessage(ChatMessage.system("你是助手B"));
        request.addMessage(ChatMessage.user(Arrays.asList(
                ContentPart.text("这张图里有什么？"),
                ContentPart.image("https://example.com/a.png"),
                ContentPart.imageBase64("image/png", "iVBORw0KGgo="))));
        request.addAssistantToolCalls(Collections.singletonList(
                ToolCall.of("c1", "get_weather", "{\"city\":\"北京\"}")));
        request.addMessage(ChatMessage.toolResult("c1", "工具 [get_weather] 执行失败：boom", true));

        Map<String, Object> actual = bodyOf(request);
        assertEquals(AiFixtures.jsonResource("ai/gemini/request-system-merge-and-parts.json"), actual);
        assertEquals("多条system按先后顺序以换行连接",
                "你是助手A\n你是助手B", map(mapList(map(actual.get("systemInstruction")).get("parts")).get(0)).get("text"));
        List<Map<String, Object>> contents = mapList(actual.get("contents"));
        assertEquals("assistant消息角色映射为model", "model", contents.get(1).get("role"));
        assertEquals("工具结果固定用user角色", ChatRole.USER.getValue(), contents.get(2).get("role"));
    }

    /**
     * 小写类型不泄漏：嵌套层（array.items、多层object）的类型取值全部大写，enum取值不受影响
     */
    @Test
    public void nestedSchemaTypesAreUppercased() {
        Map<String, Object> city = new LinkedHashMap<>();
        city.put("type", "string");
        Map<String, Object> unit = new LinkedHashMap<>();
        unit.put("type", "string");
        unit.put("enum", Arrays.asList("C", "F"));
        Map<String, Object> objectProperty = new LinkedHashMap<>();
        objectProperty.put("type", "object");
        objectProperty.put("properties", mapOf("city", city, "unit", unit));
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("type", "integer");
        Map<String, Object> arrayProperty = new LinkedHashMap<>();
        arrayProperty.put("type", "array");
        arrayProperty.put("items", items);
        Map<String, Object> numberProperty = new LinkedHashMap<>();
        numberProperty.put("type", "number");
        Map<String, Object> booleanProperty = new LinkedHashMap<>();
        booleanProperty.put("type", "boolean");
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", mapOf("query", objectProperty, "ids", arrayProperty,
                "ratio", numberProperty, "flag", booleanProperty));
        ChatRequest request = new ChatRequest(MODEL);
        request.addMessage(ChatMessage.user("查一下"));
        request.setToolSpecs(Collections.singletonList(new ToolSpec("get_weather", "查询城市天气", new ConfigurationMap(schema))));

        Map<String, Object> parameters = map(mapList(mapList(bodyOf(request).get("tools")).get(0)
                .get("functionDeclarations")).get(0).get("parameters"));
        Map<String, Object> properties = map(parameters.get("properties"));
        Map<String, Object> queryProperties = map(map(properties.get("query")).get("properties"));
        assertEquals("OBJECT", parameters.get("type"));
        assertEquals("OBJECT", map(properties.get("query")).get("type"));
        assertEquals("STRING", map(queryProperties.get("city")).get("type"));
        assertEquals("ARRAY", map(properties.get("ids")).get("type"));
        assertEquals("INTEGER", map(map(properties.get("ids")).get("items")).get("type"));
        assertEquals("NUMBER", map(properties.get("ratio")).get("type"));
        assertEquals("BOOLEAN", map(properties.get("flag")).get("type"));
        assertEquals("enum取值不受影响", Arrays.asList("C", "F"), map(queryProperties.get("unit")).get("enum"));
    }

    /**
     * 工具结果文本不是JSON时以content承载，错误说明不被改写或丢弃
     */
    @Test
    public void plainTextToolResultIsPreserved() {
        ChatRequest request = new ChatRequest(MODEL);
        request.addAssistantToolCalls(Collections.singletonList(
                ToolCall.of("c1", "get_weather", "{}")));
        request.addToolResult("c1", "北京今天晴，12度");

        Map<String, Object> functionResponse = map(mapList(mapList(bodyOf(request).get("contents")).get(1)
                .get("parts")).get(0).get("functionResponse"));
        assertEquals("get_weather", functionResponse.get("name"));
        assertEquals("北京今天晴，12度", map(functionResponse.get("response")).get("content"));
    }

    /**
     * 目标地址按模型标识与调用方式生成，鉴权头取自配置，请求体里没有模型字段
     */
    @Test
    public void urlCarriesTheModelNameAndCredentialComesFromConfig() {
        server.registerJson(GENERATE_CONTENT_PATH);
        server.queueResponse(GENERATE_CONTENT_PATH, AiFixtures.resource("ai/gemini/response-text.json"));
        GeminiChatApi api = factory().getProxyObject(GeminiChatApi.class);

        ChatRequest request = AiFixtures.minimalRequest();
        request.setModel(MODEL);
        ChatResponse response = api.chat(MODEL, request);

        assertEquals(1, server.hit(GENERATE_CONTENT_PATH));
        TestHttpServer.RecordedRequest sent = server.lastRequest(GENERATE_CONTENT_PATH);
        assertEquals("POST", sent.method());
        assertEquals(GENERATE_CONTENT_PATH, sent.path());
        assertEquals(CONFIGURED_API_KEY, sent.header("x-goog-api-key"));
        Map<String, Object> body = toMap(sent.body());
        assertFalse("模型标识在地址里，不进请求体", body.containsKey("model"));
        assertFalse("回环内部控制字段不得泄漏进请求体", body.containsKey("loopRound"));
        assertEquals("北京今天晴，12 度。", response.getContent());
        assertSame(FinishReason.STOP, response.getFinishReason());
    }

    // ------------------------------------------------------------------------------------
    // 3.4 parseResponse
    // ------------------------------------------------------------------------------------

    /**
     * functionCall分片归一为工具调用，文本分片同时保留，finishReason为STOP时仍表达为「要求调用工具」
     */
    @Test
    public void parseFunctionCallResponse() {
        ChatResponse response = dialect.parseResponse(AiFixtures.jsonResource("ai/gemini/response-tool-call.json"));

        assertEquals("我来查一下。", response.getContent());
        assertSame(ChatRole.ASSISTANT, response.getFirstChoice().getMessage().getRole());
        List<ToolCall> toolCalls = response.getToolCalls();
        assertEquals(1, toolCalls.size());
        assertEquals("get_weather", toolCalls.get(0).getName());
        assertEquals("{\"city\":\"北京\"}", toolCalls.get(0).getArguments());
        assertSame("出现functionCall时结束原因与厂商取值无关", FinishReason.TOOL_CALLS, response.getFinishReason());
        assertEquals(Integer.valueOf(18), response.getUsage().getInputTokens());
        assertEquals(Integer.valueOf(12), response.getUsage().getOutputTokens());
        assertEquals(Integer.valueOf(30), response.getUsage().getTotalTokens());
    }

    /**
     * 报文无调用标识时合成的标识非空、同一响应内互不相同，且重复解析结果稳定
     */
    @Test
    public void synthesizeCallId() {
        Map<String, Object> raw = AiFixtures.jsonResource("ai/gemini/response-two-function-calls.json");
        List<ToolCall> toolCalls = dialect.parseResponse(raw).getToolCalls();
        assertEquals(2, toolCalls.size());

        List<String> ids = new ArrayList<>();
        for (ToolCall toolCall : toolCalls) {
            assertNotNull(toolCall.getId());
            assertFalse(toolCall.getId().isEmpty());
            ids.add(toolCall.getId());
        }
        assertFalse("同一响应内的标识必须互不相同", ids.get(0).equals(ids.get(1)));
        assertEquals("同一片段序列重复解析结果一致", ids,
                extractIds(dialect.parseResponse(raw).getToolCalls()));
        assertEquals("同时保留文本与两个调用", "同时查一下上海。",
                dialect.parseResponse(raw).getContent());
    }

    /**
     * 归一化后的标识可参与回填报文，按工具名与调用配对
     */
    @Test
    public void backfillPairsByToolName() {
        ChatResponse response = dialect.parseResponse(AiFixtures.jsonResource("ai/gemini/response-tool-call.json"));
        String callId = response.getToolCalls().get(0).getId();

        ChatRequest secondRound = new ChatRequest(MODEL);
        secondRound.addMessage(ChatMessage.user("北京今天天气怎么样？"));
        secondRound.addAssistantToolCalls(new ArrayList<>(response.getToolCalls()));
        secondRound.addToolResult(callId, "{\"temp\":12,\"weather\":\"晴\"}");

        List<Map<String, Object>> contents = mapList(bodyOf(secondRound).get("contents"));
        Map<String, Object> functionCall = map(mapList(contents.get(1).get("parts")).get(0).get("functionCall"));
        Map<String, Object> functionResponse = map(mapList(contents.get(2).get("parts")).get(0).get("functionResponse"));
        assertEquals("get_weather", functionCall.get("name"));
        assertEquals(functionCall.get("name"), functionResponse.get("name"));
        assertEquals(Collections.singletonMap("city", "北京"), map(functionCall.get("args")));
        assertEquals(mapOf("temp", 12, "weather", "晴"), map(functionResponse.get("response")));
    }

    /**
     * usageMetadata的三个计数映射到统一字段，缺失总数时由输入加输出补齐
     */
    @Test
    public void usageNormalized() {
        Map<String, Object> raw = AiFixtures.jsonResource("ai/gemini/response-tool-call.json");
        assertEquals(Integer.valueOf(30), dialect.parseResponse(raw).getUsage().getTotalTokens());

        Map<String, Object> withoutTotal = new HashMap<>();
        Map<String, Object> usage = new HashMap<>();
        usage.put("promptTokenCount", 7);
        usage.put("candidatesTokenCount", 3);
        withoutTotal.put("usageMetadata", usage);
        Usage derived = dialect.parseResponse(withoutTotal).getUsage();
        assertEquals(Integer.valueOf(7), derived.getInputTokens());
        assertEquals(Integer.valueOf(3), derived.getOutputTokens());
        assertEquals(Integer.valueOf(10), derived.getTotalTokens());
    }

    /**
     * 缺失用量与未知字段都不破坏解析；MAX_TOKENS归一为长度上限
     */
    @Test
    public void parseTextResponseWithoutUsageAndWithUnknownFields() {
        ChatResponse noUsage = dialect.parseResponse(AiFixtures.jsonResource("ai/gemini/response-no-usage.json"));
        assertEquals("我不会查天气。", noUsage.getContent());
        assertSame(FinishReason.STOP, noUsage.getFinishReason());
        assertFalse(noUsage.hasToolCalls());
        assertNull(noUsage.getUsage().getInputTokens());
        assertNull(noUsage.getUsage().getOutputTokens());
        assertNull(noUsage.getUsage().getTotalTokens());

        Map<String, Object> raw = AiFixtures.jsonResource("ai/gemini/response-text.json");
        mapList(raw.get("candidates")).get(0).put("finishReason", "MAX_TOKENS");
        assertSame(FinishReason.LENGTH, dialect.parseResponse(raw).getFinishReason());
    }

    /**
     * 能力标志与拼接器通过方言暴露
     */
    @Test
    public void capabilitiesAndAccumulatorAreExposedThroughTheDialect() {
        Capabilities capabilities = dialect.capabilities();
        assertTrue(capabilities.isToolCallSupported());
        assertTrue(capabilities.isStreamToolCallSupported());
        assertFalse(capabilities.isParallelToolCallsSupported());
        assertFalse(capabilities.isStrictSchemaSupported());
        assertTrue(dialect.newAccumulator() instanceof GeminiStreamAccumulator);
    }

    private static List<String> extractIds(List<ToolCall> toolCalls) {
        List<String> ids = new ArrayList<>(toolCalls.size());
        for (ToolCall toolCall : toolCalls) {
            ids.add(toolCall.getId());
        }
        return ids;
    }

    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private static boolean containsRole(List<Map<String, Object>> contents, String role) {
        for (Map<String, Object> content : contents) {
            if (role.equals(content.get("role"))) {
                return true;
            }
        }
        return false;
    }
}
