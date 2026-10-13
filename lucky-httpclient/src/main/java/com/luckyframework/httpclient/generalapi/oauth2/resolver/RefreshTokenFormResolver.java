package com.luckyframework.httpclient.generalapi.oauth2.resolver;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.RefreshTokenGrant;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * 刷新令牌模式表单参数解析器（RFC 6749 §4.5.1）。
 * <pre>
 *     从 {@link RefreshTokenGrant} 注解中解析该模式的必要表单参数：
 *     grant_type=refresh_token、refresh_token
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public class RefreshTokenFormResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        RefreshTokenGrant ann = context.getMergedAnnotationCheckParent(RefreshTokenGrant.class);
        if (ann == null) {
            ann = context.toAnnotation(RefreshTokenGrant.class);
        }

        List<ParamInfo> params = new ArrayList<>();
        params.add(new ParamInfo("grant_type", "refresh_token"));

        String refreshToken = context.parseExpression(ann.refreshToken(), String.class);
        if (StringUtils.hasText(refreshToken)) {
            params.add(new ParamInfo("refresh_token", refreshToken));
        }

        return params;
    }
}
