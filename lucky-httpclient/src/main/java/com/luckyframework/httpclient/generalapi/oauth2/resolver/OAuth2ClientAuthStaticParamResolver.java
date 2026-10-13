package com.luckyframework.httpclient.generalapi.oauth2.resolver;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.OAuth2ConfigException;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2ClientAuth;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.luckyframework.httpclient.proxy.function.CommonFunctions.basicAuth;

/**
 * OAuth2 客户端认证参数解析器（RFC 6749 §2.3.1）。
 * <pre>
 *     按照规范生成 {@code Authorization: Basic base64(clientId:clientSecret)} 请求头：
 *     Authorization = "Basic " + base64(clientId + ":" + clientSecret)
 *
 *     clientId与clientSecret均未配置时返回空参数列表（public客户端场景）。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public class OAuth2ClientAuthStaticParamResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {

        // 优先从上下文链中获取合并注解：使用位置的grant注解实例上的clientId/clientSecret
        // 会通过@AliasFor属性透传到@OAuth2ClientAuth的合并注解实例上；
        // 获取不到时退回框架标准的组合注解转换方式
        OAuth2ClientAuth clientAuth = context.getMergedAnnotationCheckParent(OAuth2ClientAuth.class);
        if (clientAuth == null) {
            clientAuth = context.toAnnotation(OAuth2ClientAuth.class);
        }

        // 解析注解属性中可能存在的SpEL表达式
        String clientId = context.parseExpression(clientAuth.clientId(), String.class);
        String clientSecret = context.parseExpression(clientAuth.clientSecret(), String.class);

        // 客户端凭证均未配置时不设置认证头（适用于public客户端）
        if (!StringUtils.hasText(clientId) && !StringUtils.hasText(clientSecret)) {
            return Collections.emptyList();
        }

        String header = context.parseExpression(clientAuth.header(), String.class);
        String charset = context.parseExpression(clientAuth.charset(), String.class);

        try {
            return Collections.singletonList(new ParamInfo(header, basicAuth(clientId, clientSecret, charset)));
        } catch (IOException e) {
            throw new OAuth2ConfigException("Failed to encode client credentials with charset '" + charset + "'", e);
        }
    }
}
