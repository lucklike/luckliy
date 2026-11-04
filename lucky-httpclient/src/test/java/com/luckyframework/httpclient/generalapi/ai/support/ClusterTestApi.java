package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.core.meta.Request;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.vendor.OpenAIChatResponseConvert;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;
import com.luckyframework.httpclient.proxy.mock.Mock;
import com.luckyframework.httpclient.proxy.mock.MockResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OpenAI兼容簇测试用的会话API接口：地址与鉴权全部来自配置项，
 * 用 {@link Mock} 拦截真实网络请求并把每一轮的 {@link Request} 记录下来供断言。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@ServerAddress(url = "#{ai.cluster.baseUrl}")
public interface ClusterTestApi {

    /**
     * 每次调用实际生成的请求对象
     */
    List<Request> CAPTURED = Collections.synchronizedList(new ArrayList<>());

    /**
     * 发起一次会话请求
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/chat/completions")
    @Mock
    @StaticHeader("Authorization: Bearer #{ai.cluster.apiKey}")
    @ResultConvertMeta(convert = @ObjectGenerate(OpenAIChatResponseConvert.class))
    ChatResponse chat(@JsonBody ChatRequest request);

    /**
     * 记录本轮请求并回放同一份OpenAI兼容形状样本
     *
     * @param request 本轮即将发出的请求
     * @return 录制的响应样本
     */
    static MockResponse chat$Mock(Request request) {
        CAPTURED.add(request);
        return MockResponse.create().status(200).json(AiFixtures.resource("ai/openai/response-tool-call.json"));
    }
}
