package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.MaxRoundsTestApi;
import com.luckyframework.httpclient.generalapi.ai.support.ToolCallTestApi;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 最大轮次保护：上限可配置覆盖，达上限而模型仍在要求调用工具时停止重放并抛出可诊断异常，
 * 异常文本含已达成轮次数与最后一轮待执行的工具名及参数；响应回放由本地假服务端完成，全程离线。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class MaxRoundsTest {

    private static final String CHAT_PATH = TestHttpServer.CHAT_COMPLETIONS_PATH;
    private static final String TOOL_CALL_SAMPLE = "ai/openai/response-tool-call.json";
    private static final String TEXT_SAMPLE = "ai/openai/response-text.json";

    private TestHttpServer server;

    @Before
    public void setUp() throws IOException {
        server = new TestHttpServer();
        server.registerJson(CHAT_PATH);
    }

    @After
    public void tearDown() {
        server.close();
    }

    /**
     * 上限配置为3且每一轮模型都要求调用工具：共发3次请求后抛异常，第4次请求不得发出
     */
    @Test
    public void hitsLimit() {
        // 队列里只有一个响应体时会被重复使用，于是模型「永远」在要求调用工具
        server.queueResponse(CHAT_PATH, AiFixtures.resource(TOOL_CALL_SAMPLE));

        try {
            api(3).chat(AiFixtures.minimalRequest());
            fail("达到轮次上限时必须抛出可诊断异常");
        } catch (Exception e) {
            assertTrue("异常文本要含已达成的轮次数：" + chain(e), messageChainContains(e, "轮次上限 3"));
            assertTrue("异常文本要含最后一轮的工具名：" + chain(e), messageChainContains(e, "get_weather"));
            assertTrue("异常文本要含最后一轮的参数：" + chain(e), messageChainContains(e, "{\"city\":\"北京\"}"));
        }
        assertEquals("第4次请求不该发出", 3, server.hit(CHAT_PATH));
    }

    /**
     * 同一批样本（前4轮工具调用、第5轮文本）在上限8下正常收敛，返回第5轮的文本回答
     */
    @Test
    public void raisesLimit() {
        for (int round = 0; round < 4; round++) {
            server.queueResponse(CHAT_PATH, AiFixtures.resource(TOOL_CALL_SAMPLE));
        }
        server.queueResponse(CHAT_PATH, AiFixtures.resource(TEXT_SAMPLE));

        ChatResponse response = api(8).chat(AiFixtures.minimalRequest());

        assertEquals("北京今天晴，12 度。", response.getContent());
        assertEquals("前4轮工具调用加最后1轮文本共5次请求", 5, server.hit(CHAT_PATH));
    }

    /**
     * 不写上限属性时走注解默认值5
     */
    @Test
    public void defaultLimitIsFive() {
        server.queueResponse(CHAT_PATH, AiFixtures.resource(TOOL_CALL_SAMPLE));

        HttpClientProxyObjectFactory factory = factory(null);
        MaxRoundsTestApi api = factory.getProxyObject(MaxRoundsTestApi.class);
        try {
            api.chatWithDefaultLimit(AiFixtures.minimalRequest());
            fail("达到默认轮次上限时必须抛出异常");
        } catch (Exception e) {
            assertTrue("默认上限应是5轮：" + chain(e), messageChainContains(e, "轮次上限 5"));
        }
        assertEquals(5, server.hit(CHAT_PATH));
    }

    private MaxRoundsTestApi api(int maxRounds) {
        return factory(maxRounds).getProxyObject(MaxRoundsTestApi.class);
    }

    private HttpClientProxyObjectFactory factory(Integer maxRounds) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(AiFixtures.weatherToolSpec(), args -> "北京今天晴，12 度");

        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.addSpringElRootVariable(ToolCallTestApi.REGISTRY_VARIABLE, registry);
        AiConfiguration config = AiConfiguration.create().put("loopTest.baseUrl", server.baseUrl() + "/");
        if (maxRounds != null) {
            config.put(MaxRoundsTestApi.MAX_ROUNDS_CONFIG, maxRounds);
        }
        config.install(factory);
        return factory;
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
