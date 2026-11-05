package com.luckyframework.httpclient.generalapi.ai.mcp;

import com.luckyframework.httpclient.core.meta.Response;
import com.luckyframework.httpclient.proxy.convert.ConvertContext;
import com.luckyframework.httpclient.proxy.convert.ResponseConvert;

/**
 * 把HTTP响应原样摊平成 {@link McpHttpResponse} 的转换器：
 * 传输层要看得到状态码、Content-Type、会话标识头与响应体文本才能分支，所以不走「对象结果」路径。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 * @apiNote 内部API：由 {@link McpHttpApi} 的注解挂载，业务方不需要直接使用
 */
public class McpHttpResponseConvert implements ResponseConvert {

    @Override
    public <T> T convert(Response response, ConvertContext context) throws Throwable {
        @SuppressWarnings("unchecked")
        T result = (T) McpHttpResponse.from(response);
        return result;
    }
}
