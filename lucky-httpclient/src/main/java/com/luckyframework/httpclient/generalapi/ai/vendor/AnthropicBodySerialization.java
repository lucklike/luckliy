package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.core.serialization.StringBodySerialization;
import com.luckyframework.httpclient.generalapi.ai.AiJsonUtils;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatRequest;

/**
 * Anthropic的请求体序列化：把统一模型交给 {@link AnthropicDialect#writeBody(ChatRequest)} 翻译后再转JSON，
 * 于是业务侧的方法参数仍然只是 {@link ChatRequest}，厂商形状差异全部留在方言层。
 * <pre>
 *     必填项缺失（如未配置默认的 max_tokens）会在请求体组装阶段抛异常，
 *     因此异常发生在HTTP请求发出之前，不会打出一个必被拒绝的请求。
 * </pre>
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API：由 {@link AnthropicChatApi} 的注解挂载，业务方一般不需要直接使用
 */
public class AnthropicBodySerialization extends StringBodySerialization {

    private final AnthropicDialect dialect;

    public AnthropicBodySerialization() {
        this(new AnthropicDialect());
    }

    /**
     * 使用指定方言组装请求体，业务需要自定义缺省 max_tokens 时走这个构造
     */
    public AnthropicBodySerialization(AnthropicDialect dialect) {
        this.dialect = dialect;
    }

    @Override
    protected String serializationToString(Object object) throws Exception {
        if (object instanceof ChatRequest) {
            return AiJsonUtils.toJson(dialect.writeBody((ChatRequest) object));
        }
        return AiJsonUtils.toJson(object);
    }
}
