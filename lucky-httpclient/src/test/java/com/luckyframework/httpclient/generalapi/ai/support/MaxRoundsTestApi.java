package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.loop.EnableToolCall;
import com.luckyframework.httpclient.generalapi.ai.vendor.OpenAIChatResponseConvert;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;

/**
 * 轮次上限测试用的会话API接口：地址与轮次上限都来自配置项，
 * 响应由本地假服务端（{@code TestHttpServer}）按队列回放，用来断言「到底发了几次请求」。
 * <pre>
 *     两个方法分别验证「上限可配置覆盖」与「默认上限5」：
 *     {@code chat} 的上限取 {@code #{ai.loopTest.maxRounds}}，
 *     {@code chatWithDefaultLimit} 不写该属性，走注解默认值。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@ServerAddress(url = "#{ai.loopTest.baseUrl}")
public interface MaxRoundsTestApi {

    /**
     * 工具注册表在SpEL中的根变量名，与 {@link ToolCallTestApi#REGISTRY_VARIABLE} 共用
     */
    String MAX_ROUNDS_CONFIG = "loopTest.maxRounds";

    /**
     * 轮次上限来自配置的会话方法
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/chat/completions")
    @EnableToolCall(registry = "#{toolCallTestRegistry}", maxRounds = "#{ai." + MAX_ROUNDS_CONFIG + "}")
    @ResultConvertMeta(convert = @ObjectGenerate(OpenAIChatResponseConvert.class))
    ChatResponse chat(@JsonBody ChatRequest request);

    /**
     * 不写上限、走注解默认值（5轮）的会话方法
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/chat/completions")
    @EnableToolCall(registry = "#{toolCallTestRegistry}")
    @ResultConvertMeta(convert = @ObjectGenerate(OpenAIChatResponseConvert.class))
    ChatResponse chatWithDefaultLimit(@JsonBody ChatRequest request);
}
