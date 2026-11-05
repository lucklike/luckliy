package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.StreamLoopTestApi;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolSpec;
import com.luckyframework.httpclient.generalapi.ai.vendor.OpenAIDialect;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.sse.EventListener;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 5.6 流式与阻塞共用同一份runner语义：轮次上限、失败回填、请求副本都不因传输方式而改变，
 * 只是「发一轮」的实现换成「一次流式代理方法调用 + 等待本轮聚合结果」。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class StreamLoopRoundTest {

    private StreamLoopTestApi api;
    private HttpClientProxyObjectFactory factory;
    private ToolRegistry registry;

    @Before
    public void setUp() {
        StreamLoopTestApi.SENT_BODIES.clear();
        StreamLoopTestApi.OVERRIDE_SAMPLE.set(null);

        registry = new ToolRegistry();
        factory = new HttpClientProxyObjectFactory();
        factory.addSpringElRootVariable(StreamLoopTestApi.REGISTRY_VARIABLE, registry);
        factory.addSpringElRootVariable(StreamLoopTestApi.DIALECT_VARIABLE, new OpenAIDialect());
        AiConfiguration.create().put(StreamLoopTestApi.MAX_ROUNDS_CONFIG, 5).install(factory);
        api = factory.getProxyObject(StreamLoopTestApi.class);
    }

    /**
     * 换一个轮次上限重新装配工厂（配置项覆盖默认值）
     */
    private void withMaxRounds(int maxRounds) {
        factory = new HttpClientProxyObjectFactory();
        factory.addSpringElRootVariable(StreamLoopTestApi.REGISTRY_VARIABLE, registry);
        factory.addSpringElRootVariable(StreamLoopTestApi.DIALECT_VARIABLE, new OpenAIDialect());
        AiConfiguration.create().put(StreamLoopTestApi.MAX_ROUNDS_CONFIG, maxRounds).install(factory);
        api = factory.getProxyObject(StreamLoopTestApi.class);
    }

    /**
     * 每一轮都要求调用工具时，流式回环同样在到达上限处终止并抛出可诊断异常
     */
    @Test
    public void streamLoopHitsRoundLimit() {
        registry.register(AiFixtures.weatherToolSpec(), args -> "{\"temp\":12,\"weather\":\"晴\"}");
        StreamLoopTestApi.OVERRIDE_SAMPLE.set("ai/openai/stream-tool-call.txt");
        withMaxRounds(3);

        try {
            api.stream(streamingRequest(), new SilentListener());
            fail("达到轮次上限时应当抛出异常");
        } catch (Throwable e) {
            assertTrue(String.valueOf(e), e instanceof ToolCallException);
            assertTrue(e.getMessage(), e.getMessage().contains("轮次上限 3"));
            assertTrue(e.getMessage(), e.getMessage().contains("get_weather"));
            assertTrue(e.getMessage(), e.getMessage().contains("{\"city\":\"北京\"}"));
        }
        assertEquals("第4次请求不得发出", 3, StreamLoopTestApi.SENT_BODIES.size());
    }

    /**
     * 提高上限后同一批样本正常收敛
     */
    @Test
    public void streamLoopConvergesAfterRaisingLimit() {
        registry.register(AiFixtures.weatherToolSpec(), args -> "{\"temp\":12,\"weather\":\"晴\"}");
        withMaxRounds(8);

        StringBuilder received = new StringBuilder();
        api.stream(streamingRequest(), new SilentListener() {
            @Override
            public void onText(String message) {
                received.append(message);
            }
        });

        assertEquals("北京今天晴，12 度。", received.toString());
        assertEquals(2, StreamLoopTestApi.SENT_BODIES.size());
    }

    /**
     * 工具执行异常在流式下同样被回填给模型自纠，业务侧既不收到异常也能拿到后续文本
     */
    @Test
    public void toolFailureIsBackfilledInStreamMode() {
        registry.register(AiFixtures.weatherToolSpec(), args -> {
            throw new IllegalStateException("天气服务不可用");
        });

        StringBuilder received = new StringBuilder();
        api.stream(streamingRequest(), new SilentListener() {
            @Override
            public void onText(String message) {
                received.append(message);
            }
        });

        assertEquals("北京今天晴，12 度。", received.toString());
        List<String> sentBodies = new ArrayList<>(StreamLoopTestApi.SENT_BODIES);
        assertEquals(2, sentBodies.size());

        List<Map<String, Object>> secondMessages = messages(parseBody(sentBodies.get(1)));
        Map<String, Object> toolMessage = secondMessages.get(3);
        assertEquals("tool", toolMessage.get("role"));
        String content = String.valueOf(toolMessage.get("content"));
        assertTrue(content, content.contains("get_weather"));
        assertTrue(content, content.contains("天气服务不可用"));
    }

    /**
     * 未知工具在流式下同样回填「未知工具」说明，不影响回环收敛
     */
    @Test
    public void unknownToolIsBackfilledInStreamMode() {
        registry.register(new ToolSpec(
                "another_tool", "与模型调用的工具同名不同", AiFixtures.weatherSchema()), args -> "{}");

        api.stream(streamingRequest(), new SilentListener());

        List<String> sentBodies = new ArrayList<>(StreamLoopTestApi.SENT_BODIES);
        assertEquals(2, sentBodies.size());
        String content = String.valueOf(messages(parseBody(sentBodies.get(1))).get(3).get("content"));
        assertTrue(content, content.contains("未知工具"));
        assertTrue(content, content.contains("get_weather"));
    }

    /**
     * 回环只在副本上追加消息，调用方的请求对象不被就地修改
     */
    @Test
    public void callerRequestIsNotModifiedByStreamLoop() {
        registry.register(AiFixtures.weatherToolSpec(), args -> "{\"temp\":12,\"weather\":\"晴\"}");
        ChatRequest request = streamingRequest();

        api.stream(request, new SilentListener());

        assertEquals(2, request.getMessages().size());
        assertEquals(2, StreamLoopTestApi.SENT_BODIES.size());
    }

    private static ChatRequest streamingRequest() {
        ChatRequest request = AiFixtures.minimalRequest();
        request.setStream(true);
        return request;
    }

    private static Map<String, Object> parseBody(String body) {
        return AiJsonUtils.fromJson(body, new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> messages(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("messages");
    }

    /**
     * 只接收不校验的监听器，个别用例通过覆写onText来收集文本
     */
    private static class SilentListener implements EventListener {

        @Override
        public void onOpen(Response response) {
        }

        @Override
        public void onText(String message) {
        }

        @Override
        public void onError(Throwable event) {
        }

        @Override
        public void onCompleted() {
        }
    }
}
