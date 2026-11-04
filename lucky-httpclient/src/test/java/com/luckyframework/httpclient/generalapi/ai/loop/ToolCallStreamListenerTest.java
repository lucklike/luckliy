package com.luckyframework.httpclient.generalapi.ai.loop;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.support.AiFixtures;
import com.luckyframework.httpclient.generalapi.ai.support.StreamLoopTestApi;
import com.luckyframework.httpclient.generalapi.ai.tool.ToolRegistry;
import com.luckyframework.httpclient.generalapi.ai.vendor.OpenAIDialect;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
import com.luckyframework.httpclient.proxy.sse.EventListener;
import com.luckyframework.serializable.SerializationTypeToken;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 5.5 流式回环的对外可见性：业务方只看到一个连续的流——文本增量跨轮连续投递、
 * 完整工具调用被截留执行、中间轮次的开关流与结束事件全部被吞掉。
 * 两轮流式响应都由 {@code @Mock} 回放录制的SSE文本，全程离线。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class ToolCallStreamListenerTest {

    private StreamLoopTestApi api;
    private RecordingListener listener;
    private AtomicInteger toolInvocations;

    @Before
    public void setUp() {
        StreamLoopTestApi.SENT_BODIES.clear();
        StreamLoopTestApi.OVERRIDE_SAMPLE.set(null);

        toolInvocations = new AtomicInteger();
        ToolRegistry registry = new ToolRegistry();
        registry.register(AiFixtures.weatherToolSpec(), args -> {
            toolInvocations.incrementAndGet();
            return "{\"temp\":12,\"weather\":\"晴\"}";
        });

        HttpClientProxyObjectFactory factory = new HttpClientProxyObjectFactory();
        factory.addSpringElRootVariable(StreamLoopTestApi.REGISTRY_VARIABLE, registry);
        factory.addSpringElRootVariable(StreamLoopTestApi.DIALECT_VARIABLE, new OpenAIDialect());
        AiConfiguration.create().put(StreamLoopTestApi.MAX_ROUNDS_CONFIG, 5).install(factory);

        api = factory.getProxyObject(StreamLoopTestApi.class);
        listener = new RecordingListener();
    }

    private ChatRequest streamingRequest() {
        ChatRequest request = AiFixtures.minimalRequest();
        request.setStream(true);
        return request;
    }

    @Test
    public void twoRoundsLookLikeOneContinuousStream() {
        api.stream(streamingRequest(), listener);

        List<String> sentBodies = new ArrayList<>(StreamLoopTestApi.SENT_BODIES);
        assertEquals("工具调用流之后应当再发一轮", 2, sentBodies.size());
        assertEquals("文本增量跨轮连续且不掺工具帧", 2, listener.texts.size());
        assertEquals("北京今天晴", listener.texts.get(0));
        assertEquals("，12 度。", listener.texts.get(1));
        assertEquals(1, toolInvocations.get());
        assertEquals("onCompleted只能触发一次", 1, listener.completedCount.get());
        assertEquals("onOpen只能透传一次", 1, listener.openCount.get());
        assertEquals("onClose只能透传一次", 1, listener.closeCount.get());
        assertTrue(listener.errors.isEmpty());

        List<Map<String, Object>> secondMessages = messages(parseBody(sentBodies.get(1)));
        assertEquals(4, secondMessages.size());
        assertEquals("assistant", secondMessages.get(2).get("role"));
        assertEquals(1, collection(secondMessages.get(2).get("tool_calls")).size());
        assertEquals("tool", secondMessages.get(3).get("role"));
        assertEquals("call_001", secondMessages.get(3).get("tool_call_id"));
        assertEquals("{\"temp\":12,\"weather\":\"晴\"}", secondMessages.get(3).get("content"));
    }

    @Test
    public void streamWithoutToolCallSendsOnlyOneRequest() {
        StreamLoopTestApi.OVERRIDE_SAMPLE.set("ai/openai/stream-text.txt");

        api.stream(streamingRequest(), listener);

        assertEquals(1, StreamLoopTestApi.SENT_BODIES.size());
        assertEquals(0, toolInvocations.get());
        assertEquals(1, listener.completedCount.get());
        assertEquals("北京今天晴，12 度。", String.join("", listener.texts));
    }

    @Test
    public void toolCallFramesAreNeverDeliveredAsText() {
        // 只有工具调用流的一轮：业务方收不到任何文本，也收不到结束事件（回环还要继续）
        StreamLoopTestApi.OVERRIDE_SAMPLE.set("ai/openai/stream-tool-call.txt");
        try {
            api.stream(streamingRequest(), listener);
        } catch (Throwable expected) {
            // 每轮都要求调用工具，到达上限后按契约抛异常
            assertTrue(String.valueOf(expected.getMessage()), expected instanceof ToolCallException);
        }
        assertTrue("工具调用帧不能变成文本事件", listener.texts.isEmpty());
        assertEquals(0, listener.completedCount.get());
        assertEquals(1, listener.openCount.get());
        assertEquals("异常终止时也要把关闭事件交还给业务方", 1, listener.closeCount.get());
    }

    private static Map<String, Object> parseBody(String body) {
        return AiJsonUtils.fromJson(body, new SerializationTypeToken<Map<String, Object>>() {
        });
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> messages(Map<String, Object> body) {
        return (List<Map<String, Object>>) body.get("messages");
    }

    private static Collection<?> collection(Object node) {
        return node instanceof Collection ? (Collection<?>) node : Collections.emptyList();
    }

    /**
     * 业务侧监听器：只统计回调，不参与任何拼接逻辑
     */
    private static class RecordingListener implements EventListener {

        private final List<String> texts = new ArrayList<>();
        private final List<Throwable> errors = new ArrayList<>();
        private final AtomicInteger openCount = new AtomicInteger();
        private final AtomicInteger completedCount = new AtomicInteger();
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public void onOpen(Response response) {
            openCount.incrementAndGet();
        }

        @Override
        public void onText(String message) {
            texts.add(message);
        }

        @Override
        public void onError(Throwable event) {
            errors.add(event);
        }

        @Override
        public void onCompleted() {
            completedCount.incrementAndGet();
        }

        @Override
        public void onClose() {
            closeCount.incrementAndGet();
        }
    }
}
