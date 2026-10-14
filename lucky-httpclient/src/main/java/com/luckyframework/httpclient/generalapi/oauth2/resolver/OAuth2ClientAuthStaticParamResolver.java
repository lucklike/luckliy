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
 *     按客户端类型区分认证方式（二者互斥，由 {@link #useBasicAuth} / {@link #sendClientIdInBody} 统一判定）：
 *     1.confidential 客户端（clientId 与 clientSecret 均有值）：
 *       生成 {@code Authorization: Basic base64(clientId:clientSecret)} 请求头（RFC 6749 §2.3.1）；
 *     2.public 客户端（仅有 clientId、无 clientSecret，如 PKCE 场景）：
 *       不下发 Basic 头，改由各 grant 表单解析器通过 {@link #appendPublicClientIdParam}
 *       把 client_id 追加到表单体；
 *     3.clientId 未配置时两种参数均不产生。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public class OAuth2ClientAuthStaticParamResolver implements StaticParamResolver {

    /** confidential 客户端判定：clientId 与 clientSecret 均有值时才使用 Authorization: Basic 认证 */
    static boolean useBasicAuth(String clientId, String clientSecret) {
        return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
    }

    /** public 客户端判定：有 clientId 且无 clientSecret 时，client_id 需随表单体提交 */
    static boolean sendClientIdInBody(String clientId, String clientSecret) {
        return StringUtils.hasText(clientId) && !StringUtils.hasText(clientSecret);
    }

    /**
     * 供各 grant 表单解析器复用：public 客户端（有 clientId 无 clientSecret）时，
     * 把 client_id 追加到表单体（RFC 6749 §2.3.1 / RFC 7636 public 客户端）。
     *
     * @param params                当前 grant 已解析出的表单参数列表
     * @param context               表单参数的注解上下文
     * @param clientIdExpression    grant 注解上的 clientId（可能含 SpEL 表达式）
     * @param clientSecretExpression grant 注解上的 clientSecret（可能含 SpEL 表达式）
     */
    public static void appendPublicClientIdParam(List<ParamInfo> params,
                                                 StaticParamAnnContext context,
                                                 String clientIdExpression,
                                                 String clientSecretExpression) {
        String clientId = context.parseExpression(clientIdExpression, String.class);
        String clientSecret = context.parseExpression(clientSecretExpression, String.class);
        if (sendClientIdInBody(clientId, clientSecret)) {
            params.add(new ParamInfo("client_id", clientId));
        }
    }

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

        // 仅 confidential 客户端才下发 Basic 头；public 客户端（只有 clientId）在此跳过，
        // 其 client_id 由各 grant 表单解析器通过 appendPublicClientIdParam 放入表单体
        if (!useBasicAuth(clientId, clientSecret)) {
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
