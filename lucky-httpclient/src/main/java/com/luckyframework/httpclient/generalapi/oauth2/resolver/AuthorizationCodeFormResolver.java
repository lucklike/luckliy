package com.luckyframework.httpclient.generalapi.oauth2.resolver;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2AuthorizationCodeGrant;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * 授权码模式表单参数解析器（RFC 6749 §4.1.3 + RFC 7636 §4.5.3）。
 * <pre>
 *     从 {@link OAuth2AuthorizationCodeGrant} 注解中解析该模式的必要表单参数：
 *     grant_type=authorization_code、code、redirect_uri
 *     以及可选的 PKCE 参数：code_verifier、code_challenge_method
 *
 *     codeVerifier 为空时不追加 PKCE 参数（未启用 PKCE）。
 *     public 客户端（有 clientId 无 clientSecret）时把 client_id 追加到表单体，
 *     confidential 客户端则由 {@link OAuth2ClientAuthStaticParamResolver} 下发 Basic 头。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public class AuthorizationCodeFormResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        OAuth2AuthorizationCodeGrant ann = context.getMergedAnnotationCheckParent(OAuth2AuthorizationCodeGrant.class);
        if (ann == null) {
            ann = context.toAnnotation(OAuth2AuthorizationCodeGrant.class);
        }

        List<ParamInfo> params = new ArrayList<>();
        params.add(new ParamInfo("grant_type", "authorization_code"));

        String code = context.parseExpression(ann.code(), String.class);
        if (StringUtils.hasText(code)) {
            params.add(new ParamInfo("code", code));
        }

        String redirectUri = context.parseExpression(ann.redirectUri(), String.class);
        if (StringUtils.hasText(redirectUri)) {
            params.add(new ParamInfo("redirect_uri", redirectUri));
        }

        // PKCE parameters (RFC 7636 §4.5.3)
        String codeVerifier = context.parseExpression(ann.codeVerifier(), String.class);
        if (StringUtils.hasText(codeVerifier)) {
            params.add(new ParamInfo("code_verifier", codeVerifier));
        }

        // public 客户端（有 clientId 无 clientSecret）时把 client_id 放入表单体
        OAuth2ClientAuthStaticParamResolver.appendPublicClientIdParam(
                params, context, ann.clientId(), ann.clientSecret());

        return params;
    }
}
