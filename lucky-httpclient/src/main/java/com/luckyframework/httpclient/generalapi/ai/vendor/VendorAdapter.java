package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;

import java.util.Map;

/**
 * 厂商方言：统一模型与各家报文之间的唯一翻译层。
 * 业务代码与回环引擎里不出现厂商分支，厂商差异只落在本接口的实现内。
 * 方言的选择方式是「选API接口」——每套内置会话接口绑定一个方言实现。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public interface VendorAdapter {

    /**
     * 统一模型 → 各家请求体。
     * OpenAI兼容簇直接返回请求对象本身（其形状即chat/completions请求体），
     * 其余家返回按各家结构组装的Map树。
     */
    Object writeBody(ChatRequest request);

    /**
     * 各家响应 → 统一模型。入参是已解析为Map的原始报文，实现里显式取字段，不依赖POJO的隐式绑定
     */
    ChatResponse parseResponse(Map<String, Object> raw);

    /**
     * 能力标志，取值必须与实际实现一致
     */
    Capabilities capabilities();

    /**
     * 新建一个流式增量拼接器（每轮流式调用各建一个实例）
     */
    StreamAccumulator newAccumulator();
}
