package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.core.serialization.StringBodySerialization;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;

/**
 * Gemini的请求体序列化：把统一模型交给 {@link GeminiDialect#writeBody(ChatRequest)} 翻译后再转JSON，
 * 模型标识本身进不了请求体（它在目标地址里），因此业务侧还需要把模型名作为路径参数传给接口方法。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API：由 {@link GeminiChatApi} 的注解挂载，业务方一般不需要直接使用
 */
public class GeminiBodySerialization extends StringBodySerialization {

    private final GeminiDialect dialect = new GeminiDialect();

    @Override
    protected String serializationToString(Object object) throws Exception {
        if (object instanceof ChatRequest) {
            return AiJsonUtils.toJson(dialect.writeBody((ChatRequest) object));
        }
        return AiJsonUtils.toJson(object);
    }
}
