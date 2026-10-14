package com.luckyframework.httpclient.generalapi.oauth2.resolver;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2DeviceAuthorizationRequest;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备授权请求表单参数解析器（RFC 8628 §3.1）。
 * <pre>
 *     从 {@link OAuth2DeviceAuthorizationRequest} 注解中解析设备授权端点所需的表单参数：
 *     可选的 scope；public 客户端（无 clientSecret）时随表单体提交 client_id。
 *     confidential 客户端的认证由 {@code @OAuth2ClientAuth} 生成的 Basic 请求头负责。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 */
public class DeviceAuthorizationFormResolver implements StaticParamResolver {

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        OAuth2DeviceAuthorizationRequest ann = context.getMergedAnnotationCheckParent(OAuth2DeviceAuthorizationRequest.class);
        if (ann == null) {
            ann = context.toAnnotation(OAuth2DeviceAuthorizationRequest.class);
        }

        List<ParamInfo> params = new ArrayList<>();

        String clientId = context.parseExpression(ann.clientId(), String.class);
        String clientSecret = context.parseExpression(ann.clientSecret(), String.class);
        // public 客户端（无密钥）时，client_id 需随表单体提交（RFC 8628 §3.1）
        if (StringUtils.hasText(clientId) && !StringUtils.hasText(clientSecret)) {
            params.add(new ParamInfo("client_id", clientId));
        }

        String scope = context.parseExpression(ann.scope(), String.class);
        if (StringUtils.hasText(scope)) {
            params.add(new ParamInfo("scope", scope));
        }

        return params;
    }
}
