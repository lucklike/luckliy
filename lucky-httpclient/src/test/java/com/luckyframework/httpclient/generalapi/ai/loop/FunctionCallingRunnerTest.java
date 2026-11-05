package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.ToolCallTestApi;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.logging.BeautifulLoggerPrintHandler;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * 阻塞式工具调用回环的端到端验证：接口方法标注 {@link EnableToolCall}，
 * 响应样本由 {@code @Mock} 按当前消息条数回放，全程离线。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class FunctionCallingRunnerTest {

    private ToolCallTestApi api;

    @Before
    public void setUp() {
        ToolCallTestApi.SENT_BODIES.clear();

        ToolRegistry registry = new ToolRegistry();
        registry.register(AiFixtures.weatherToolSpec(), args -> "{\"temp\":12,\"weather\":\"晴\"}");

        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.setLoggerHandler(new BeautifulLoggerPrintHandler());
        factory.addSpringElRootVariable(ToolCallTestApi.REGISTRY_VARIABLE, registry);
        api = factory.getProxyObject(ToolCallTestApi.class);
    }

    /**
     * 单轮工具调用：共发2次请求，第2次多出assistant的工具调用消息与tool结果消息，返回第2轮文本
     */
    @Test
    public void singleRoundToolCallLoop() {
        ChatRequest request = AiFixtures.minimalRequest();
        ChatResponse response = api.chat(request);

        List<String> sentBodies = new ArrayList<>(ToolCallTestApi.SENT_BODIES);
        assertEquals("回环应当只发出2次请求", 2, sentBodies.size());
        assertEquals("北京今天晴，12 度。", response.getContent());

        Map<String, Object> first = parseBody(sentBodies.get(0));
        Map<String, Object> second = parseBody(sentBodies.get(1));

        assertEquals(2, messages(first).size());
        List<Map<String, Object>> secondMessages = messages(second);
        assertEquals("第二轮请求体应比第一轮多两条消息", 4, secondMessages.size());

        Map<String, Object> assistantMessage = secondMessages.get(2);
        assertEquals("assistant", assistantMessage.get("role"));
        List<?> toolCalls = (List<?>) assistantMessage.get("tool_calls");
        assertNotNull("assistant消息应携带工具调用", toolCalls);
        assertEquals(1, toolCalls.size());

        Map<String, Object> toolMessage = secondMessages.get(3);
        assertEquals("tool", toolMessage.get("role"));
        assertEquals("call_001", toolMessage.get("tool_call_id"));
        assertEquals("{\"temp\":12,\"weather\":\"晴\"}", toolMessage.get("content"));
    }

    /**
     * 启用注解后工具列表由框架注入，业务代码没有手工设置
     */
    @Test
    public void toolDeclarationsAreInjected() {
        api.chat(AiFixtures.minimalRequest());

        Map<String, Object> first = parseBody(ToolCallTestApi.SENT_BODIES.get(0));
        List<?> tools = (List<?>) first.get("tools");
        assertNotNull("请求体应带上工具声明列表", tools);
        assertEquals(1, tools.size());
    }

    /**
     * 子集生效：注册表里有3个工具而注解只放开1个，报文的工具列表长度为1，
     * 未被选中的工具不会出现在请求里（模型若调用它们则按「未知工具」处理）
     */
    @Test
    @SuppressWarnings("unchecked")
    public void toolSubsetLimitsTheDeclaredTools() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(AiFixtures.weatherToolSpec(), args -> "{\"temp\":12,\"weather\":\"晴\"}");
        registry.register(new ToolSpec("get_stock", "查询股价", AiFixtures.weatherSchema()), args -> "600000");
        registry.register(new ToolSpec("get_news", "查询新闻", AiFixtures.weatherSchema()), args -> "no news");

        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.addSpringElRootVariable(ToolCallTestApi.REGISTRY_VARIABLE, registry);
        ChatResponse response = factory.getProxyObject(ToolCallTestApi.class)
                .chatWithToolSubset(AiFixtures.minimalRequest());

        List<Map<String, Object>> tools = (List<Map<String, Object>>) parseBody(
                ToolCallTestApi.SENT_BODIES.get(0)).get("tools");
        assertEquals("注解只放开了一个工具", 1, tools.size());
        Map<String, Object> function = (Map<String, Object>) tools.get(0).get("function");
        assertEquals("get_weather", function.get("name"));
        assertEquals("北京今天晴，12 度。", response.getContent());
    }

    private static Map<String, Object> parseBody(String body) {
        return AiJsonUtils.fromJson(body, new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> messages(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("messages");
    }
}
