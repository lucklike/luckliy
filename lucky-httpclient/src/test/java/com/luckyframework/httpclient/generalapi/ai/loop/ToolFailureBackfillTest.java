package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.FailureBackfillTestApi;
import com.luckyframework.httpclient.generalapi.ai.support.ToolCallTestApi;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolExecutor;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.typeparser.support.TestHttpServer;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 工具调用失败的五类回填（执行异常、未知工具、参数JSON非法、必填缺失、有声明无执行实现）、
 * 同轮多调用的完整性，以及失败策略的两态。响应全部由本地假服务端回放，全程离线。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class ToolFailureBackfillTest {

    private static final String CHAT_PATH = TestHttpServer.CHAT_COMPLETIONS_PATH;
    private static final String TEXT_SAMPLE = "ai/openai/response-text.json";
    private static final String TOOL_CALL_SAMPLE = "ai/openai/response-tool-call.json";

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
     * 执行异常：失败说明含工具名与错误摘要，回填后回环继续，业务侧无异常
     */
    @Test
    public void executionExceptionIsBackfilled() {
        ToolRegistry registry = registry(args -> {
            throw new IllegalStateException("weather service down");
        });
        replay(TOOL_CALL_SAMPLE, TEXT_SAMPLE);

        ChatResponse response = api(registry).chat(AiFixtures.minimalRequest());

        assertEquals("北京今天晴，12 度。", response.getContent());
        String content = toolMessageContent(1, 0);
        assertTrue(content, content.contains("get_weather"));
        assertTrue(content, content.contains("weather service down"));
        assertEquals(2, server.hit(CHAT_PATH));
    }

    /**
     * 模型幻觉出未登记的工具名：回填「未知工具」说明，回环继续
     */
    @Test
    public void unknownToolIsBackfilled() {
        ToolRegistry registry = registry(args -> "北京今天晴，12 度");
        replay("ai/openai/response-unknown-tool.json", TEXT_SAMPLE);

        ChatResponse response = api(registry).chat(AiFixtures.minimalRequest());

        assertEquals("北京今天晴，12 度。", response.getContent());
        String content = toolMessageContent(1, 0);
        assertTrue("结果要指明未知工具：" + content, content.contains("未知工具"));
        assertTrue(content, content.contains("get_stock"));
    }

    /**
     * 参数文本不是合法JSON：回填解析失败说明，不以空参数执行工具
     */
    @Test
    public void illegalJsonArgumentsIsBackfilled() {
        final int[] invoked = new int[1];
        ToolRegistry registry = registry(args -> {
            invoked[0]++;
            return "ok";
        });
        replay("ai/openai/response-bad-arguments.json", TEXT_SAMPLE);

        api(registry).chat(AiFixtures.minimalRequest());

        String content = toolMessageContent(1, 0);
        assertTrue("结果要指明参数解析失败：" + content, content.contains("参数JSON解析失败"));
        assertEquals("参数非法时不得执行工具实现", 0, invoked[0]);
    }

    /**
     * 参数值无法按声明类型转换：回填含参数名的转换失败说明
     */
    @Test
    public void unconvertibleArgumentIsBackfilled() {
        ToolRegistry registry = registry(args -> "ok");
        replay("ai/openai/response-wrong-type.json", TEXT_SAMPLE);

        api(registry).chat(AiFixtures.minimalRequest());

        String content = toolMessageContent(1, 0);
        assertTrue("结果要指明无法按声明类型转换：" + content, content.contains("无法按声明的类型"));
        assertTrue(content, content.contains("city"));
    }

    /**
     * 必填参数缺失：回填缺失说明，工具实现方法未被调用
     */
    @Test
    public void missingRequiredArgumentIsBackfilled() {
        final int[] invoked = new int[1];
        ToolRegistry registry = registry(args -> {
            invoked[0]++;
            return "ok";
        });
        replay("ai/openai/response-missing-required.json", TEXT_SAMPLE);

        api(registry).chat(AiFixtures.minimalRequest());

        String content = toolMessageContent(1, 0);
        assertTrue("结果要指明缺失必填参数：" + content, content.contains("缺少必填参数"));
        assertTrue(content, content.contains("city"));
        assertEquals("必填缺失时不得执行工具实现", 0, invoked[0]);
    }

    /**
     * 有声明无执行实现：回填「无执行实现」的失败结果，不抛未捕获异常
     */
    @Test
    public void declarationOnlyToolIsBackfilled() {
        ToolRegistry registry = new ToolRegistry().register(AiFixtures.weatherToolSpec());
        replay(TOOL_CALL_SAMPLE, TEXT_SAMPLE);

        ChatResponse response = api(registry).chat(AiFixtures.minimalRequest());

        assertEquals("北京今天晴，12 度。", response.getContent());
        String content = toolMessageContent(1, 0);
        assertTrue("结果要指明无执行实现：" + content, content.contains("无执行实现"));
        assertTrue(content, content.contains("get_weather"));
    }

    /**
     * 同轮两个调用部分失败：两条结果都与调用标识一一对应，顺序按响应中的调用顺序
     */
    @Test
    public void partialFailure() {
        ToolRegistry registry = registry(args -> {
            if ("上海".equals(args.get("city"))) {
                throw new IllegalStateException("shanghai unavailable");
            }
            return "北京今天晴，12 度";
        });
        replay("ai/openai/response-two-tool-calls.json", TEXT_SAMPLE);

        api(registry).chat(AiFixtures.minimalRequest());

        List<Map<String, Object>> secondMessages = messages(1);
        List<String> callIds = new ArrayList<>();
        for (Map<String, Object> call : mapList(secondMessages.get(2).get("tool_calls"))) {
            callIds.add(String.valueOf(call.get("id")));
        }
        assertEquals("回填的assistant消息必带两个调用", 2, callIds.size());

        List<Map<String, Object>> toolMessages = toolMessages(1);
        assertEquals("两个调用必须有两条结果", 2, toolMessages.size());
        assertEquals("结果按响应中的调用顺序逐条回填", secondMessages.get(3), toolMessages.get(0));
        assertEquals(secondMessages.get(4), toolMessages.get(1));
        assertEquals(callIds.get(0), toolMessages.get(0).get("tool_call_id"));
        assertEquals(callIds.get(1), toolMessages.get(1).get("tool_call_id"));
        assertEquals("北京今天晴，12 度", toolMessages.get(0).get("content"));
        assertTrue(String.valueOf(toolMessages.get(1).get("content")),
                String.valueOf(toolMessages.get(1).get("content")).contains("shanghai unavailable"));
    }

    /**
     * 终止策略：首个工具异常即上抛给业务方，不再发出后续请求
     */
    @Test
    public void aborts() {
        ToolRegistry registry = registry(args -> {
            throw new IllegalStateException("weather service down");
        });
        replay(TOOL_CALL_SAMPLE, TEXT_SAMPLE);

        try {
            api(registry).chatWithAbortPolicy(AiFixtures.minimalRequest());
            fail("终止策略下工具异常必须上抛给业务方");
        } catch (Exception e) {
            assertTrue("业务方要收到工具异常本身：" + chain(e), messageChainContains(e, "weather service down"));
        }
        assertEquals("终止后不再发出后续请求", 1, server.hit(CHAT_PATH));
    }

    // ------------------------------------------------------------------------------------
    // 夹具与断言辅助
    // ------------------------------------------------------------------------------------

    private static ToolRegistry registry(ToolExecutor executor) {
        return new ToolRegistry().register(AiFixtures.weatherToolSpec(), executor);
    }

    private void replay(String... samples) {
        for (String sample : samples) {
            server.queueResponse(CHAT_PATH, AiFixtures.resource(sample));
        }
    }

    private FailureBackfillTestApi api(ToolRegistry registry) {
        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.addSpringElRootVariable(ToolCallTestApi.REGISTRY_VARIABLE, registry);
        AiConfiguration.create().put("failureTest.baseUrl", server.baseUrl() + "/").install(factory);
        return factory.getProxyObject(FailureBackfillTestApi.class);
    }

    private Map<String, Object> body(int requestIndex) {
        String body = server.requests(CHAT_PATH).get(requestIndex).body();
        return AiJsonUtils.fromJson(body, new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    private List<Map<String, Object>> messages(int requestIndex) {
        return mapList(body(requestIndex).get("messages"));
    }

    private List<Map<String, Object>> toolMessages(int requestIndex) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> message : messages(requestIndex)) {
            if ("tool".equals(message.get("role"))) {
                results.add(message);
            }
        }
        return results;
    }

    private String toolMessageContent(int requestIndex, int index) {
        return String.valueOf(toolMessages(requestIndex).get(index).get("content"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object node) {
        return (List<Map<String, Object>>) node;
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
