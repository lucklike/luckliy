package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;

/**
 * Anthropic的会话接口：与 {@link OpenAIChatApi} 的唯一区别是本报文形状由Anthropic方言负责翻译。
 * <pre>
 *     厂商差异靠「选哪个接口」表达，业务代码与回环引擎里不存在厂商分支：
 *     <code>
 *     AiConfiguration.load("classpath:ai/anthropic/api-config.yml")   // ai.anthropic.baseUrl / ai.anthropic.apiKey
 *             .install(factory);
 *     AnthropicChatApi api = factory.getProxyObject(AnthropicChatApi.class);
 *     ChatResponse response = api.chat(request);   // 入参与返回值仍是统一模型
 *     </code>
 *     anthropic-version头是协议必填项，写在默认注解里；鉴权信息仍然只来自配置文件。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @see AnthropicDialect
 * @see AnthropicBodySerialization
 * @see AnthropicChatResponseConvert
 */
@ServerAddress(url = "#{ai.anthropic.baseUrl}")
public interface AnthropicChatApi {

    /**
     * 发起一次会话请求，请求体按Anthropic形状组装，响应归一化为统一模型
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/v1/messages")
    @StaticHeader({"anthropic-version: " + AnthropicDialect.PROTOCOL_VERSION, "x-api-key: #{ai.anthropic.apiKey}"})
    @ResultConvertMeta(convert = @ObjectGenerate(AnthropicChatResponseConvert.class))
    ChatResponse chat(@JsonBody(serialization = @ObjectGenerate(AnthropicBodySerialization.class)) ChatRequest request);
}
