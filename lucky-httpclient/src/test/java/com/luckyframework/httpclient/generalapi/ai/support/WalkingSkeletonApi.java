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
import com.luckyframework.httpclient.proxy.annotations.StaticHeader;

/**
 * 切片一端到端验收用的会话接口：与 {@code OpenAIChatApi} 形状一致，只是方法上多标了
 * {@link EnableToolCall}，工具取全局默认注册表的全集。
 * <pre>
 *     连接信息全部来自配置文件（base-url在测试里被改成本地假服务端），测试全程不访问外部网络。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
@ServerAddress(url = "#{ai.openai.baseUrl}")
public interface WalkingSkeletonApi {

    /**
     * 开启工具调用回环的会话方法
     *
     * @param request 会话请求
     * @return 归一化后的会话响应
     */
    @Post("/chat/completions")
    @StaticHeader("Authorization: Bearer #{ai.openai.apiKey}")
    @EnableToolCall
    @ResultConvertMeta(convert = @ObjectGenerate(OpenAIChatResponseConvert.class))
    ChatResponse chat(@JsonBody ChatRequest request);
}
