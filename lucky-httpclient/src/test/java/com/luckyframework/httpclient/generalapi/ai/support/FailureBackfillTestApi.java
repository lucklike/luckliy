package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.loop.EnableToolCall;
import com.luckyframework.httpclient.generalapi.ai.loop.ToolCallFailurePolicy;
import com.luckyframework.httpclient.generalapi.ai.vendor.OpenAIChatResponseConvert;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;

/**
 * 工具失败回填测试用的会话API接口：响应由本地假服务端按队列回放，
 * 两个方法分别对应失败策略的两态（默认回填 / 直接终止）。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@ServerAddress(url = "#{ai.failureTest.baseUrl}")
public interface FailureBackfillTestApi {

    /**
     * 默认策略：工具失败作为结果回填给模型自纠，业务侧无异常
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/chat/completions")
    @EnableToolCall(registry = "#{toolCallTestRegistry}")
    @ResultConvertMeta(convert = @ObjectGenerate(OpenAIChatResponseConvert.class))
    ChatResponse chat(@JsonBody ChatRequest request);

    /**
     * 终止策略：首个工具异常即上抛，不再发出后续请求
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/chat/completions")
    @EnableToolCall(registry = "#{toolCallTestRegistry}", failurePolicy = ToolCallFailurePolicy.ABORT)
    @ResultConvertMeta(convert = @ObjectGenerate(OpenAIChatResponseConvert.class))
    ChatResponse chatWithAbortPolicy(@JsonBody ChatRequest request);
}
