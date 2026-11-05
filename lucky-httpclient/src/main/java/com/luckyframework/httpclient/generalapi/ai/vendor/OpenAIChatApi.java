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
 * OpenAI兼容簇的会话接口（DeepSeek、通义、Kimi、智谱、豆包、Ollama等同形厂商复用本接口）。
 * <pre>
 *     厂商差异只体现在「选哪个接口」上：本接口对应chat/completions报文形状，
 *     Anthropic与Gemini各有对应的内置接口，业务代码里不需要厂商分支。
 *
 *     base-url与鉴权信息全部来自配置（见 {@link AiConfiguration}），注解里只声明配置项的位置：
 *     <code>
 *     AiConfiguration.load("classpath:ai-config.yml")   // ai.openai.baseUrl / ai.openai.apiKey
 *             .install(factory);
 *     OpenAIChatApi api = factory.getProxyObject(OpenAIChatApi.class);
 *     ChatResponse response = api.chat(request);
 *     </code>
 *     兼容簇厂商更换配置里的 base-url 与模型标识即可复用，报文结构与处理行为完全一致。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @see OpenAIChatResponseConvert
 */
@ServerAddress(url = "#{ai.openai.baseUrl}")
public interface OpenAIChatApi {

    /**
     * 发起一次会话请求，响应体归一化为统一模型
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/chat/completions")
    @StaticHeader("Authorization: Bearer #{ai.openai.apiKey}")
    @ResultConvertMeta(convert = @ObjectGenerate(OpenAIChatResponseConvert.class))
    ChatResponse chat(@JsonBody ChatRequest request);
}
