package com.luckyframework.httpclient.generalapi.ai.vendor;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.generalapi.ai.chat.ChatResponse;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.convert.ResponseConvert;

/**
 * Anthropic的响应转换器：先把响应体读成Map，再交给方言显式取字段归一化为 {@link ChatResponse}。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API：由 {@link AnthropicChatApi} 的注解挂载，业务方一般不需要直接使用
 */
public class AnthropicChatResponseConvert implements ResponseConvert {

    private final AnthropicDialect dialect = new AnthropicDialect();

    @Override
    public <T> T convert(Response response, ConvertContext context) throws Throwable {
        ChatResponse chatResponse = dialect.parseResponse(response.getMapResult());
        @SuppressWarnings("unchecked")
        T result = (T) chatResponse;
        return result;
    }
}
