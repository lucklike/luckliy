package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatMessage;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRole;
import com.luckyframework.httpclient.generalapi.ai.chat.ContentPart;
import com.luckyframework.httpclient.generalapi.ai.chat.FinishReason;
import com.luckyframework.httpclient.generalapi.ai.chat.ToolCall;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.AnthropicFailFastApi;
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
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Anthropic方言的 golden-file 断言：请求侧逐字段比对录制样本（system合并、tool_use/tool_result结构、
 * input_schema位置、必填 max_tokens），响应侧断言文本与工具调用的归一化、用量归一与标识配对。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class AnthropicDialectTest {

    private static final String MESSAGES_PATH = "/v1/messages";
    private static final String CONFIG_LOCATION = "classpath:ai/anthropic/api-config.yml";
    private static final String CONFIGURED_API_KEY = "sk-ant-config-file-key";

    private final AnthropicDialect dialect = new AnthropicDialect();

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

    private static Map<String, Object> bodyOf(AnthropicDialect dialect, ChatRequest request) {
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
                .put("anthropic.baseUrl", server.baseUrl() + "/")
                .install(factory);
        return factory;
    }

    // ------------------------------------------------------------------------------------
    // 3.1 writeBody
    // ------------------------------------------------------------------------------------

    /**
     * 一轮完整往返的报文形状：system提为顶层、tool_use/tool_result结构、input_schema位置
     */
    @Test
    public void requestBodyMatchesGoldenFile() {
        Map<String, Object> actual = bodyOf(dialect, AiFixtures.weatherRequest());
        assertEquals(AiFixtures.jsonResource("ai/anthropic/request-with-tools.json"), actual);

        List<Map<String, Object>> messages = mapList(actual.get("messages"));
        assertEquals(3, messages.size());
        assertFalse("system不允许出现在messages里", containsRole(messages, ChatRole.SYSTEM.getValue()));
        assertFalse("tool角色要翻译为user角色", containsRole(messages, ChatRole.TOOL.getValue()));

        List<Map<String, Object>> tools = mapList(actual.get("tools"));
        assertEquals("get_weather", tools.get(0).get("name"));
        assertFalse("Anthropic的工具声明不包type层", tools.get(0).containsKey("type"));
        assertNotNull("schema落在input_schema", tools.get(0).get("input_schema"));
    }

    /**
     * 多条system按先后顺序换行连接；分片内容翻译为text/image块；失败的工具结果带 is_error
     */
    @Test
    public void systemMessagesMergeAndFailureIsMarked() {
        ChatRequest request = new ChatRequest("deepseek-chat");
        request.addMessage(ChatMessage.system("你是助手A"));
        request.addMessage(ChatMessage.system("你是助手B"));
        request.addMessage(ChatMessage.user(Arrays.asList(
                ContentPart.text("这张图里有什么？"),
                ContentPart.image("https://example.com/a.png"),
                ContentPart.imageBase64("image/png", "iVBORw0KGgo="))));
        request.addMessage(ChatMessage.toolResult("c1", "工具 [get_weather] 执行失败：boom", true));

        Map<String, Object> actual = bodyOf(dialect, request);
        assertEquals(AiFixtures.jsonResource("ai/anthropic/request-system-merge-and-parts.json"), actual);
        assertEquals("你是助手A\n你是助手B", actual.get("system"));
        assertEquals("业务未设置时取可配置默认值", Integer.valueOf(1024), actual.get("max_tokens"));
    }

    /**
     * 业务显式设置的 max_tokens 优先于默认值
     */
    @Test
    public void explicitMaxTokensWinsOverTheDefault() {
        ChatRequest request = AiFixtures.minimalRequest();
        request.setMaxTokens(2048);
        assertEquals(Integer.valueOf(2048), bodyOf(dialect, request).get("max_tokens"));

        request.setMaxTokens(null);
        Map<String, Object> withCustomDefault = bodyOf(new AnthropicDialect(512), request);
        assertEquals(Integer.valueOf(512), withCustomDefault.get("max_tokens"));
    }

    /**
     * 内置接口打本地假服务端：方法、路径、anthropic-version头与取自配置的凭据
     */
    @Test
    public void apiCarriesTheProtocolHeaderAndConfiguredCredential() {
        server.registerJson(MESSAGES_PATH);
        server.queueResponse(MESSAGES_PATH, AiFixtures.resource("ai/anthropic/response-text.json"));
        AnthropicChatApi api = factory().getProxyObject(AnthropicChatApi.class);

        ChatResponse response = api.chat(AiFixtures.minimalRequest());

        assertEquals(1, server.hit(MESSAGES_PATH));
        TestHttpServer.RecordedRequest sent = server.lastRequest(MESSAGES_PATH);
        assertEquals("POST", sent.method());
        assertEquals(MESSAGES_PATH, sent.path());
        assertEquals(AnthropicDialect.PROTOCOL_VERSION, sent.header("anthropic-version"));
        assertEquals(CONFIGURED_API_KEY, sent.header("x-api-key"));
        assertFalse("回环内部控制字段不得泄漏进请求体", toMap(sent.body()).containsKey("loopRound"));
        assertEquals("北京今天晴，12 度。", response.getContent());
        assertSame(FinishReason.STOP, response.getFinishReason());
    }

    /**
     * 必填 max_tokens 无默认值时在发出请求前失败，一个字节也不发出去
     */
    @Test
    public void maxTokensFailFast() {
        server.registerJson(MESSAGES_PATH);
        AnthropicFailFastApi api = factory().getProxyObject(AnthropicFailFastApi.class);

        try {
            api.chat(AiFixtures.minimalRequest());
            fail("缺少必填的max_tokens时必须抛异常");
        } catch (RuntimeException e) {
            assertTrue("异常消息要指明缺失的字段：" + chain(e), messageChainContains(e, "max_tokens"));
        }
        assertEquals("请求必然被拒绝，不该发出", 0, server.hit(MESSAGES_PATH));
    }

    // ------------------------------------------------------------------------------------
    // 3.2 parseResponse
    // ------------------------------------------------------------------------------------

    /**
     * 文本与工具调用并存的响应两者都保留，标识原样沿用
     */
    @Test
    public void parseToolCallResponse() {
        ChatResponse response = dialect.parseResponse(AiFixtures.jsonResource("ai/anthropic/response-tool-call.json"));

        assertEquals("我来查一下。", response.getContent());
        assertSame(ChatRole.ASSISTANT, response.getFirstChoice().getMessage().getRole());
        List<ToolCall> toolCalls = response.getToolCalls();
        assertEquals(1, toolCalls.size());
        assertEquals("toolu_001", toolCalls.get(0).getId());
        assertEquals("get_weather", toolCalls.get(0).getName());
        assertEquals("{\"city\":\"北京\"}", toolCalls.get(0).getArguments());
        assertSame("stop_reason为tool_use时归一为要求调用工具", FinishReason.TOOL_CALLS, response.getFinishReason());
        assertEquals(Integer.valueOf(25), response.getUsage().getInputTokens());
        assertEquals(Integer.valueOf(30), response.getUsage().getOutputTokens());
        assertEquals(Integer.valueOf(55), response.getUsage().getTotalTokens());
    }

    /**
     * 两个tool_use分片都归一化，顺序与报文一致
     */
    @Test
    public void parseTwoToolCallsResponse() {
        ChatResponse response = dialect.parseResponse(AiFixtures.jsonResource("ai/anthropic/response-two-tool-calls.json"));

        List<ToolCall> toolCalls = response.getToolCalls();
        assertEquals(2, toolCalls.size());
        assertEquals("toolu_001", toolCalls.get(0).getId());
        assertEquals("{\"city\":\"北京\"}", toolCalls.get(0).getArguments());
        assertEquals("toolu_002", toolCalls.get(1).getId());
        assertEquals("{\"city\":\"上海\"}", toolCalls.get(1).getArguments());
        assertSame(FinishReason.TOOL_CALLS, response.getFinishReason());
    }

    /**
     * end_turn与stop_sequence都归一为正常结束；缺失用量与未知字段都不破坏解析
     */
    @Test
    public void parseTextResponseWithoutUsageAndWithUnknownFields() {
        ChatResponse stopped = dialect.parseResponse(AiFixtures.jsonResource("ai/anthropic/response-text.json"));
        assertEquals("北京今天晴，12 度。", stopped.getContent());
        assertSame(FinishReason.STOP, stopped.getFinishReason());
        assertFalse(stopped.hasToolCalls());

        ChatResponse noUsage = dialect.parseResponse(AiFixtures.jsonResource("ai/anthropic/response-no-usage.json"));
        assertEquals("我不会查天气。", noUsage.getContent());
        assertSame(FinishReason.STOP, noUsage.getFinishReason());
        assertNull(noUsage.getUsage().getInputTokens());
        assertNull(noUsage.getUsage().getOutputTokens());
        assertNull(noUsage.getUsage().getTotalTokens());
    }

    /**
     * 归一化后的工具调用回填到请求里，标识能与 tool_use_id 一一对应
     */
    @Test
    public void backfillKeepsTheCallIdPairing() {
        ChatResponse response = dialect.parseResponse(AiFixtures.jsonResource("ai/anthropic/response-tool-call.json"));

        ChatRequest secondRound = AiFixtures.minimalRequest();
        secondRound.addAssistantToolCalls(new ArrayList<>(response.getToolCalls()));
        secondRound.addMessage(ChatMessage.toolResult("toolu_001", "{\"temp\":12,\"weather\":\"晴\"}"));

        List<Map<String, Object>> messages = mapList(bodyOf(dialect, secondRound).get("messages"));
        assertEquals("system被提为顶层，messages里只剩user与assistant", 3, messages.size());
        Map<String, Object> toolUse = map(mapList(messages.get(1).get("content")).get(0));
        Map<String, Object> toolResult = map(mapList(messages.get(2).get("content")).get(0));
        assertEquals("tool_use", toolUse.get("type"));
        assertEquals("toolu_001", toolUse.get("id"));
        assertEquals(Collections.singletonMap("city", "北京"), map(toolUse.get("input")));
        assertEquals("tool_result", toolResult.get("type"));
        assertEquals(toolUse.get("id"), toolResult.get("tool_use_id"));
        assertFalse("成功的结果不该带is_error", toolResult.containsKey("is_error"));
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
        assertTrue(dialect.newAccumulator() instanceof AnthropicStreamAccumulator);
    }

    private static boolean containsRole(List<Map<String, Object>> messages, String role) {
        for (Map<String, Object> message : messages) {
            if (role.equals(message.get("role"))) {
                return true;
            }
        }
        return false;
    }

    private static boolean messageChainContains(Throwable throwable, String text) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static String chain(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            builder.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage()).append(" | ");
        }
        return builder.toString();
    }
}
