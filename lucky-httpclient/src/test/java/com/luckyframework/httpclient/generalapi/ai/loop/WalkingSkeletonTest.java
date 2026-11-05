package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.WalkingSkeletonApi;
import com.luckyframework.httpclient.generalapi.ai.support.WeatherToolHolder;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer.CHAT_COMPLETIONS_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * 切片一（walking skeleton）的端到端验收：
 * 「会话接口 + {@link EnableToolCall} + 注解登记的一条工具」跑通
 * 发请求 → 模型要求调用工具 → 本地执行 → 结果回填 → 带历史重放 → 最终文本。
 * <pre>
 *     与 {@link FunctionCallingRunnerTest} 的区别是本用例走真实的本地HTTP服务端，
 *     把方言写体、响应转换器、回环重放、注册表全集默认实例这几段串在一起。
 *     响应样本按到达顺序回放，全程不发起任何外部网络请求。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class WalkingSkeletonTest {

    private static final String CONFIG_LOCATION = "classpath:ai/openai/api-config.yml";

    private TestHttpServer server;
    private WalkingSkeletonApi api;

    /**
     * 注解工具的实现载体，其 invocations() 记录模型传给工具的参数
     */
    private WeatherToolHolder toolHolder;

    @Before
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.registerJson(CHAT_COMPLETIONS_PATH);
        // 第1轮要求调用工具，第2轮给出文本回答
        server.queueResponse(CHAT_COMPLETIONS_PATH, AiFixtures.resource("ai/openai/response-tool-call.json"));
        server.queueResponse(CHAT_COMPLETIONS_PATH, AiFixtures.resource("ai/openai/response-text.json"));

        toolHolder = new WeatherToolHolder();
        ToolRegistry.getDefault().register(toolHolder, true);

        AiConfiguration configuration = AiConfiguration.load(CONFIG_LOCATION)
                .put("openai.baseUrl", server.baseUrl() + "/");
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        configuration.install(factory);
        api = factory.getProxyObject(WalkingSkeletonApi.class);
    }

    @After
    public void tearDown() {
        server.close();
    }

    /**
     * 一轮工具调用后拿到最终文本，且第二次请求带上了回填的历史
     */
    @Test
    public void oneToolCallRoundProducesTheFinalAnswer() {
        ChatResponse response = api.chat(AiFixtures.minimalRequest());

        assertEquals("回环应当发出2次请求", 2, server.hit(CHAT_COMPLETIONS_PATH));
        assertEquals("模型给的参数应原样传进工具", "北京", toolHolder.invocations().get(0).get("city"));
        assertEquals("北京今天晴，12 度。", response.getContent());

        List<TestHttpServer.RecordedRequest> requests = server.requests(CHAT_COMPLETIONS_PATH);
        Map<String, Object> firstBody = parseBody(requests.get(0).body());
        Map<String, Object> secondBody = parseBody(requests.get(1).body());

        assertEquals(2, messages(firstBody).size());
        List<Map<String, Object>> secondMessages = messages(secondBody);
        assertEquals(4, secondMessages.size());
        assertEquals("assistant", secondMessages.get(2).get("role"));
        assertEquals(1, ((List<?>) secondMessages.get(2).get("tool_calls")).size());
        assertEquals("tool", secondMessages.get(3).get("role"));
        assertEquals("call_001", secondMessages.get(3).get("tool_call_id"));
        assertEquals("{\"temp\":12,\"weather\":\"晴\"}", secondMessages.get(3).get("content"));
    }

    /**
     * 工具声明由框架注入请求体，业务代码没有手工设置
     */
    @Test
    public void toolDeclarationsComeFromTheDefaultRegistry() {
        api.chat(AiFixtures.minimalRequest());

        Map<String, Object> body = parseBody(server.lastRequest(CHAT_COMPLETIONS_PATH).body());
        List<?> tools = (List<?>) body.get("tools");
        assertNotNull("请求体应带上工具声明列表", tools);
        assertTrue("声明中应有get_weather", tools.toString().contains("get_weather"));
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
