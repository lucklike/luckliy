package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.loop.EnableToolCall;
import com.luckyframework.httpclient.generalapi.ai.vendor.OpenAIChatResponseConvert;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.mock.Mock;
import com.luckyframework.httpclient.proxy.mock.MockResponse;
import com.luckyframework.serializable.SerializationTypeToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 回环测试用的会话API接口：用 {@link Mock} 回放录制样本，全程不发起任何外部网络请求。
 * <pre>
 *     约定：静态方法 {@code chat$Mock} 由 @Mock 机制自动发现，入参可注入 {@link Request}，
 *     本夹具把每轮发出的请求体记录下来，并按当前消息条数选择回放样本——
 *     2条消息（首轮）回放「要求调用工具」，4条消息（回填后的第二轮）回放「文本回答」。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@ServerAddress("http://127.0.0.1:1/")
public interface ToolCallTestApi {

    /**
     * 工具注册表在SpEL中的根变量名，测试通过它把自建注册表交给注解
     */
    String REGISTRY_VARIABLE = "toolCallTestRegistry";

    /**
     * 每一轮实际发出的请求体，用于断言轮次与消息追加
     */
    List<String> SENT_BODIES = Collections.synchronizedList(new ArrayList<>());

    /**
     * 开启工具调用回环的会话方法
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/chat/completions")
    @Mock
    @EnableToolCall(registry = "#{toolCallTestRegistry}")
    @ResultConvertMeta(convert = @ObjectGenerate(OpenAIChatResponseConvert.class))
    ChatResponse chat(@JsonBody ChatRequest request);

    /**
     * 只放开{@code get_weather}一个工具的会话方法，用于断言「子集生效」：
     * 注册表里即便有更多工具，请求体的工具列表也只包含被选中的那一个
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/chat/completions")
    @Mock
    @EnableToolCall(registry = "#{toolCallTestRegistry}", tools = "get_weather")
    @ResultConvertMeta(convert = @ObjectGenerate(OpenAIChatResponseConvert.class))
    ChatResponse chatWithToolSubset(@JsonBody ChatRequest request);

    /**
     * 回放录制样本，同时记录本轮发出的请求体
     *
     * @param request 本轮即将发出的请求
     * @return 录制的响应样本
     */
    static MockResponse chat$Mock(Request request) {
        String body = request.getRequestParameter().getBody().getBodyAsString();
        SENT_BODIES.add(body);
        String sample = messageCount(body) > 2
                ? "ai/openai/response-text.json"
                : "ai/openai/response-tool-call.json";
        return MockResponse.create().status(200).json(AiFixtures.resource(sample));
    }

    /**
     * 子集方法与主方法回放同一批样本
     */
    static MockResponse chatWithToolSubset$Mock(Request request) {
        return chat$Mock(request);
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
