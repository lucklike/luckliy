package com.luckyframework.httpclient.generalapi.oauth2.resolver;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2TokenExchange;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * 令牌交换模式表单参数解析器（RFC 8693 §2.1）。
 * <pre>
 *     从 {@link OAuth2TokenExchange} 注解中解析令牌交换所需表单参数：
 *     grant_type=urn:ietf:params:oauth:grant-type:token-exchange、
 *     subject_token、subject_token_type（必填），
 *     actor_token、actor_token_type、requested_token_type、scope（可选，非空才追加），
 *     resource、audience（可重复，逐个追加）。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 */
public class TokenExchangeFormResolver implements StaticParamResolver {

    /** 令牌交换模式的 grant_type（RFC 8693 §2.1） */
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange";

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        OAuth2TokenExchange ann = context.getMergedAnnotationCheckParent(OAuth2TokenExchange.class);
        if (ann == null) {
            ann = context.toAnnotation(OAuth2TokenExchange.class);
        }

        List<ParamInfo> params = new ArrayList<>();
        params.add(new ParamInfo("grant_type", GRANT_TYPE));

        addIfPresent(params, context, "subject_token", ann.subjectToken());
        addIfPresent(params, context, "subject_token_type", ann.subjectTokenType());
        addIfPresent(params, context, "actor_token", ann.actorToken());
        addIfPresent(params, context, "actor_token_type", ann.actorTokenType());
        addIfPresent(params, context, "requested_token_type", ann.requestedTokenType());
        addIfPresent(params, context, "scope", ann.scope());
        addArrayIfPresent(params, context, "resource", ann.resource());
        addArrayIfPresent(params, context, "audience", ann.audience());

        // public 客户端（有 clientId 无 clientSecret）时把 client_id 放入表单体
        OAuth2ClientAuthStaticParamResolver.appendPublicClientIdParam(
                params, context, ann.clientId(), ann.clientSecret());

        return params;
    }

    private void addIfPresent(List<ParamInfo> params, StaticParamAnnContext context, String key, String expression) {
        String value = context.parseExpression(expression, String.class);
        if (StringUtils.hasText(value)) {
            params.add(new ParamInfo(key, value));
        }
    }

    private void addArrayIfPresent(List<ParamInfo> params, StaticParamAnnContext context, String key, String[] expressions) {
        for (String expression : expressions) {
            String value = context.parseExpression(expression, String.class);
            if (StringUtils.hasText(value)) {
                params.add(new ParamInfo(key, value));
            }
        }
    }
}
