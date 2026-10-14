package com.luckyframework.httpclient.generalapi.oauth2.resolver;

import com.luckyframework.common.StringUtils;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2DeviceCodeGrant;
import com.luckyframework.httpclient.proxy.paraminfo.ParamInfo;
import com.luckyframework.httpclient.proxy.statics.StaticParamAnnContext;
import com.luckyframework.httpclient.proxy.statics.StaticParamResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备码模式表单参数解析器（RFC 8628 §3.4）。
 * <pre>
 *     从 {@link OAuth2DeviceCodeGrant} 注解中解析该模式的表单参数：
 *     grant_type=urn:ietf:params:oauth:grant-type:device_code、device_code，
 *     public 客户端（无 clientSecret）时随表单体提交 client_id。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 */
public class DeviceCodeTokenFormResolver implements StaticParamResolver {

    /** 设备码模式的 grant_type（RFC 8628 §3.4） */
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";

    @Override
    public List<ParamInfo> parser(StaticParamAnnContext context) {
        OAuth2DeviceCodeGrant ann = context.getMergedAnnotationCheckParent(OAuth2DeviceCodeGrant.class);
        if (ann == null) {
            ann = context.toAnnotation(OAuth2DeviceCodeGrant.class);
        }

        List<ParamInfo> params = new ArrayList<>();
        params.add(new ParamInfo("grant_type", GRANT_TYPE));

        String deviceCode = context.parseExpression(ann.deviceCode(), String.class);
        if (StringUtils.hasText(deviceCode)) {
            params.add(new ParamInfo("device_code", deviceCode));
        }

        // public 客户端（有 clientId 无 clientSecret）时，client_id 需随表单体提交（RFC 8628 §3.4）
        OAuth2ClientAuthStaticParamResolver.appendPublicClientIdParam(
                params, context, ann.clientId(), ann.clientSecret());

        return params;
    }
}
