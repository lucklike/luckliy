package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.AiConfiguration;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.proxy.annotations.JsonBody;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.Path;
import com.luckyframework.httpclient.proxy.annotations.Post;
import com.luckyframework.httpclient.proxy.annotations.ResultConvertMeta;
import com.luckyframework.httpclient.proxy.annotations.ServerAddress;
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;

/**
 * Gemini的会话接口（generateContent）。
 * <pre>
 *     Gemini把模型标识放在目标地址里，所以本接口用 {@link Path} 取模型名：
 *     <code>
 *     ChatRequest request = new ChatRequest("gemini-2.0-flash");
 *     ChatResponse response = api.chat(request.getModel(), request);
 *     </code>
 *     base-url与鉴权信息全部来自配置（见 {@link AiConfiguration}）：ai.gemini.baseUrl / ai.gemini.apiKey。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @see GeminiDialect
 */
@ServerAddress(url = "#{ai.gemini.baseUrl}")
public interface GeminiChatApi {

    /**
     * 发起一次会话请求，响应体归一化为统一模型
     *
     * @param model   模型标识，落在目标地址的 {@code models/{model}:generateContent} 里
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/models/{model}:generateContent")
    @StaticHeader("x-goog-api-key: #{ai.gemini.apiKey}")
    @ResultConvertMeta(convert = @ObjectGenerate(GeminiChatResponseConvert.class))
    ChatResponse chat(@Path("model") String model,
                      @JsonBody(serialization = @ObjectGenerate(GeminiBodySerialization.class)) ChatRequest request);
}
