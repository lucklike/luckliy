package com.luckyframework.httpclient.generalapi.oauth2.resolver;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2Saml2BearerGrant;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * SAML2 断言授权模式表单参数解析器（RFC 7522 §2.1）。
 * <pre>
 *     从 {@link OAuth2Saml2BearerGrant} 注解中解析该模式的表单参数：
 *     grant_type=urn:ietf:params:oauth:grant-type:saml2-bearer、assertion。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 */
public class Saml2BearerTokenFormResolver implements StaticParamResolver {

    /** SAML2 断言授权模式的 grant_type（RFC 7522 §2.1） */
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:saml2-bearer";

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        OAuth2Saml2BearerGrant ann = context.getMergedAnnotationCheckParent(OAuth2Saml2BearerGrant.class);
        if (ann == null) {
            ann = context.toAnnotation(OAuth2Saml2BearerGrant.class);
        }

        List<ParamInfo> params = new ArrayList<>();
        params.add(new ParamInfo("grant_type", GRANT_TYPE));

        String assertion = context.parseExpression(ann.assertion(), String.class);
        if (StringUtils.hasText(assertion)) {
            params.add(new ParamInfo("assertion", assertion));
        }

        // public 客户端（有 clientId 无 clientSecret）时把 client_id 放入表单体
        OAuth2ClientAuthStaticParamResolver.appendPublicClientIdParam(
                params, context, ann.clientId(), ann.clientSecret());

        return params;
    }
}
