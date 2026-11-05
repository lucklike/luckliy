package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.generalapi.ai.vendor.AnthropicChatResponseConvert;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;

/**
 * Anthropic必填项提前失败的测试夹具：与 {@code AnthropicChatApi} 结构相同，
 * 只是请求体序列化用的是「没有默认 max_tokens」的方言。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@ServerAddress(url = "#{ai.anthropic.baseUrl}")
public interface AnthropicFailFastApi {

    /**
     * 发起一次会话请求，业务未设置 max_tokens 时应当在请求发出前抛异常
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/v1/messages")
    @StaticHeader("anthropic-version: 2023-06-01")
    @ResultConvertMeta(convert = @ObjectGenerate(AnthropicChatResponseConvert.class))
    ChatResponse chat(@JsonBody(serialization = @ObjectGenerate(NoDefaultMaxTokensAnthropicSerialization.class)) ChatRequest request);
}
