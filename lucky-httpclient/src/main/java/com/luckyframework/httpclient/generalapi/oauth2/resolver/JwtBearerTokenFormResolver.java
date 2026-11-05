package com.luckyframework.httpclient.generalapi.oauth2.resolver;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2JwtBearerGrant;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * JWT 断言授权模式表单参数解析器（RFC 7523 §2.1）。
 * <pre>
 *     从 {@link OAuth2JwtBearerGrant} 注解中解析该模式的表单参数：
 *     grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer、assertion。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 */
public class JwtBearerTokenFormResolver implements StaticParamResolver {

    /** JWT 断言授权模式的 grant_type（RFC 7523 §2.1） */
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        OAuth2JwtBearerGrant ann = context.getMergedAnnotationCheckParent(OAuth2JwtBearerGrant.class);
        if (ann == null) {
            ann = context.toAnnotation(OAuth2JwtBearerGrant.class);
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
