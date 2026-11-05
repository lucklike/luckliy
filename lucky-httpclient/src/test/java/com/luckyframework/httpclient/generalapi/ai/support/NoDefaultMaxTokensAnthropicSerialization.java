package com.luckyframework.httpclient.generalapi.ai.support;

import com.luckyframework.httpclient.generalapi.ai.vendor.AnthropicBodySerialization;
import com.luckyframework.httpclient.generalapi.ai.vendor.AnthropicDialect;

/**
 * Anthropic请求体序列化的「无默认 max_tokens」变体，用来验证必填项缺失时能在发出请求前失败。
 *
 * @author fukang
 * @version 3.0.3
 * @date 2026-10-22
 */
public class NoDefaultMaxTokensAnthropicSerialization extends AnthropicBodySerialization {

    public NoDefaultMaxTokensAnthropicSerialization() {
        super(new AnthropicDialect(null));
    }
}
