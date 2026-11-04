package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.loop.EnableToolCall;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.mock.Mock;
import com.luckyframework.httpclient.proxy.mock.MockResponse;
import com.luckyframework.httpclient.proxy.mock.SseMock;
import com.luckyframework.httpclient.proxy.sse.EventListener;
import com.luckyframework.httpclient.proxy.sse.Sse;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 流式回环测试用的会话API接口：用 {@link Mock} 回放录制的SSE文本，全程不发起任何外部网络请求。
 * <pre>
 *     约定与 {@link ToolCallTestApi} 一致：静态方法 {@code stream$Mock} 记录每轮发出的请求体，
 *     并按当前消息条数选择回放样本——2条消息（首轮）回放「要求调用工具」的流，
 *     4条消息（回填后的第二轮）回放「文本增量」的流。
 *     需要每一轮都回放工具调用流（验证轮次上限）时，把 {@link #OVERRIDE_SAMPLE} 设为固定样本即可。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@ServerAddress("http://127.0.0.1:1/")
public interface StreamLoopTestApi {

    /**
     * 工具注册表在SpEL中的根变量名
     */
    String REGISTRY_VARIABLE = "toolCallTestRegistry";

    /**
     * 厂商方言在SpEL中的根变量名（流式回环用它创建增量拼接器）
     */
    String DIALECT_VARIABLE = "openAIDialect";

    /**
     * 轮次上限的配置项键名，交给 {@code #{ai.streamTest.maxRounds}} 读取
     */
    String MAX_ROUNDS_CONFIG = "streamTest.maxRounds";

    /**
     * 每一轮实际发出的请求体
     */
    List<String> SENT_BODIES = Collections.synchronizedList(new ArrayList<>());

    /**
     * 非空时所有轮次都回放这个样本，用于「模型每轮都要求调用工具」的超限场景
     */
    AtomicReference<String> OVERRIDE_SAMPLE = new AtomicReference<>();

    /**
     * 开启流式工具调用回环的会话方法，业务监听器通过方法参数传入
     *
     * @param request  会话请求
     * @param listener 业务方的流式监听器，收到的始终是拼接后的文本增量
     */
    @Post("/chat/completions")
    @Sse
    @Mock
    @EnableToolCall(registry = "#{toolCallTestRegistry}",
            dialect = "#{openAIDialect}",
            maxRounds = "#{ai." + MAX_ROUNDS_CONFIG + "}")
    void stream(@JsonBody ChatRequest request, EventListener listener);

    /**
     * 回放录制的SSE文本，同时记录本轮发出的请求体
     *
     * @param request 本轮即将发出的请求
     * @return 录制的流式响应
     */
    static MockResponse stream$Mock(Request request) {
        String body = request.getRequestParameter().getBody().getBodyAsString();
        SENT_BODIES.add(body);
        String sample = OVERRIDE_SAMPLE.get();
        if (sample == null) {
            sample = messageCount(body) > 2
                    ? "ai/openai/stream-text.txt"
                    : "ai/openai/stream-tool-call.txt";
        }
        return MockResponse.create().status(200).sse(SseMock.resource(sample));
    }

    /**
     * 从请求体JSON里取消息条数
     */
    static int messageCount(String body) {
        Map<String, Object> sent = AiJsonUtils.fromJson(body, new SerializationTypeToken<Map<String, Object>>() {
        });
        Object messages = sent.get("messages");
        return messages instanceof Collection ? ((Collection<?>) messages).size() : 0;
    }
}
