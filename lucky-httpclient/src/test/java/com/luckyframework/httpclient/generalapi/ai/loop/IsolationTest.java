package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRole;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.ToolCallTestApi;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.generalapi.ai.vendor.OpenAIChatApi;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 回环的隔离性：只在请求对象的副本上追加消息（入参不被就地修改），
 * 未标注 {@link EnableToolCall} 的方法行为完全不变（不注入工具列表、不执行工具、不重放）。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class IsolationTest {

    private static final String CHAT_PATH = TestHttpServer.CHAT_COMPLETIONS_PATH;

    private TestHttpServer server;

    @Before
    public void setUp() throws IOException {
        server = new TestHttpServer();
        server.registerJson(CHAT_PATH);
        ToolCallTestApi.SENT_BODIES.clear();
    }

    @After
    public void tearDown() {
        server.close();
    }

    /**
     * 回环在副本上追加消息：方法返回后入参对象的消息条数与内容都未变化，
     * 而实际发出的第二轮请求含更多消息（说明它用的是另一个实例）
     */
    @Test
    public void callerRequestIsNeverModifiedInPlace() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(AiFixtures.weatherToolSpec(), args -> "{\"temp\":12,\"weather\":\"晴\"}");

        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.addSpringElRootVariable(ToolCallTestApi.REGISTRY_VARIABLE, registry);
        ChatRequest request = AiFixtures.minimalRequest();
        ChatResponse response = factory.getProxyObject(ToolCallTestApi.class).chat(request);

        assertEquals("北京今天晴，12 度。", response.getContent());
        assertEquals("入参对象的消息条数不得变化", 2, request.getMessages().size());
        assertEquals(ChatRole.SYSTEM, request.getMessages().get(0).getRole());
        assertEquals("你是一个助手", request.getMessages().get(0).getContentText());
        assertEquals("北京今天天气怎么样？", request.getMessages().get(1).getContentText());

        List<String> sent = ToolCallTestApi.SENT_BODIES;
        assertEquals(2, sent.size());
        assertEquals("入参未被就地修改，第二轮发出的必然是另一个实例",
                2, messagesOf(sent.get(0)).size());
        assertEquals(4, messagesOf(sent.get(1)).size());
    }

    /**
     * 未标注启用注解的方法行为不变：请求体不含tools字段，响应里出现工具调用时原样返回、不重放
     */
    @Test
    public void methodWithoutEnableToolCallIsUnchanged() {
        server.queueResponse(CHAT_PATH, AiFixtures.resource("ai/openai/response-tool-call.json"));

        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        AiConfiguration.create()
                .put("openai.baseUrl", server.baseUrl() + "/")
                .put("openai.apiKey", "sk-isolation-test-key")
                .install(factory);

        ChatRequest request = AiFixtures.minimalRequest();
        request.setToolSpecs(null);
        ChatResponse response = factory.getProxyObject(OpenAIChatApi.class).chat(request);

        assertEquals("不启用回环时只发一次请求", 1, server.hit(CHAT_PATH));
        Map<String, Object> body = AiJsonUtils.fromJson(server.lastRequest(CHAT_PATH).body(),
                new SerializationTypeToken<Map<String, Object>>() {
                });
        assertFalse("请求体不该被注入工具列表", body.containsKey("tools"));
        assertTrue("响应里的工具调用原样返回给业务方", response.hasToolCalls());
        assertEquals("call_001", response.getToolCalls().get(0).getId());
    }

    private static List<Map<String, Object>> messagesOf(String body) {
        Map<String, Object> sent = AiJsonUtils.fromJson(body, new SerializationTypeToken<Map<String, Object>>() {
        });
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) sent.get("messages");
        return messages;
    }
}
