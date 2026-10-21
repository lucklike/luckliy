package com.luckyframework.httpclient.generalapi.oauth2;

import com.luckyframework.httpclient.generalapi.oauth2.annotation.GenerateOAuth2AuthorizationUrl;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2AuthorizationCodeGrant;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2BearerAuth;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2ClientCredentialsGrant;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2DeviceAuthorizationRequest;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2DeviceCodeGrant;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2JwtBearerGrant;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2PasswordGrant;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2RefreshTokenGrant;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2Saml2BearerGrant;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.OAuth2TokenExchange;
import com.luckyframework.httpclient.generalapi.oauth2.plugin.GenerateAuthorizationUrlProxyPlugin;
import com.luckyframework.httpclient.generalapi.oauth2.resp.DeviceAuthorizationResult;
import com.luckyframework.httpclient.generalapi.oauth2.resp.OAuth2Token;
import com.luckyframework.httpclient.generalapi.oauth2.resp.OAuth2UserInfo;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.Post;

/**
 * OAuth2 认证客户端（RFC 6749）。
 * <pre>
 *     演示如何使用框架级OAuth2注解套件（oauth2.annotation包）声明式地对接OAuth2认证服务器：
 *     1.令牌请求方法：授权模式注解（@OAuth2ClientCredentialsGrant等）+ 使用位置声明的令牌端点
 *     2.资源访问方法：@OAuth2BearerAuth + 使用位置声明的资源端点
 *
 *     OAuth2注解套件本身不绑定任何端点，本接口中的路径（/oauth2/token、/userinfo）
 *     为示例值，对接不同认证服务器时请修改为对应服务商的端点路径；
 *     服务器地址、超时时间以及各方法的Mock配置在httpclient-configuration.yml中配置，
 *     默认配置ID为接口类名"OAuth2Api"。
 *
 *     1.客户端凭证模式：{@link #clientCredentialsToken(String, String)}
 *     2.密码模式：     {@link #passwordToken(String, String, String, String)}
 *     3.刷新令牌模式： {@link #refreshToken(String, String, String)}
 *     4.授权码模式：   {@link #authorizationCodeToken(String, String, String, String, String)}
 *     5.设备码模式：   {@link #deviceAuthorization(String, String, String)} + {@link #deviceCodeToken(String, String)}（RFC 8628）
 *     6.令牌交换：     {@link #tokenExchange(String, String, String, String)}（RFC 8693）
 *     7.JWT 断言授权： {@link #jwtBearerToken(String, String, String)}（RFC 7523）
 *     8.SAML2 断言授权：{@link #saml2BearerToken(String, String, String)}（RFC 7522）
 *
 *     本客户端提供令牌获取与刷新请求能力（含 @OAuth2RefreshTokenGrant），
 *     但不内置自动缓存与过期刷新机制；令牌的生命周期管理由调用方自行负责。
 *
 *     授权码模式中 §4.1.1 生成授权URL 的步骤由
 *     {@link GenerateAuthorizationUrlProxyPlugin} 插件实现，
 *     本接口中的 {@link #buildAuthorizationUrl(String, String, String)} 为其声明式用法示例。
 *
 *     获取代理对象的方式：
 *     {@code
 *     // 独立使用
 *     OAuth2Api api = new HttpClientProxyObjectFactory().getProxyObject(OAuth2Api.class);
 *     // 与SpringBoot整合时，标注 @HttpClientComponent 或使用 @HttpReference 注入
 *     }
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
public interface OAuth2Api {

    /**
     * 客户端凭证模式获取令牌（RFC 6749 §4.4）
     *
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return 令牌信息
     */
    @Post("/oauth2/token")
    @OAuth2ClientCredentialsGrant(clientId = "#{clientId}", clientSecret = "#{clientSecret}")
    OAuth2Token clientCredentialsToken(String clientId, String clientSecret);

    /**
     * 密码模式获取令牌（RFC 6749 §4.3）
     *
     * @param username     用户名
     * @param password     密码
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return 令牌信息
     */
    @Post("/oauth2/token")
    @OAuth2PasswordGrant(
            clientId = "#{clientId}",
            clientSecret = "#{clientSecret}",
            username = "#{username}",
            password = "#{password}"
    )
    OAuth2Token passwordToken(String username,
                              String password,
                              String clientId,
                              String clientSecret);

    /**
     * 刷新令牌模式获取新的令牌（RFC 6749 §4.5）
     *
     * @param refreshToken 刷新令牌
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return 令牌信息
     */
    @Post("/oauth2/token")
    @OAuth2RefreshTokenGrant(
            clientId = "#{clientId}",
            clientSecret = "#{clientSecret}",
            refreshToken = "#{refreshToken}"
    )
    OAuth2Token refreshToken(String refreshToken,
                             String clientId,
                             String clientSecret);

    /**
     * 授权码模式使用授权码换取令牌（RFC 6749 §4.1.3 + PKCE RFC 7636 §4.5.3）
     *
     * @param code         授权码
     * @param redirectUri  重定向地址（需与授权请求时保持一致）
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @param codeVerifier PKCE code_verifier（未启用PKCE时传null）
     * @return 令牌信息
     */
    @Post("/oauth2/token")
    @OAuth2AuthorizationCodeGrant(
            clientId = "#{clientId}",
            clientSecret = "#{clientSecret}",
            code = "#{code}",
            redirectUri = "#{redirectUri}",
            codeVerifier = "#{codeVerifier}"
    )
    OAuth2Token authorizationCodeToken(String code,
                                       String redirectUri,
                                       String clientId,
                                       String clientSecret,
                                       String codeVerifier);

    /**
     * 生成授权请求URL（RFC 6749 §4.1.1）。
     * <pre>
     *     由 {@link GenerateAuthorizationUrlProxyPlugin}
     *     插件拦截执行：授权端点沿用框架内置流程解析（@ServerAddress 域名 + 方法上 @Get 路径），
     *     按 @GenerateOAuth2AuthorizationUrl 配置补齐 query 参数后直接返回完整授权 URL，不会发起真实 HTTP 请求。
     *
     *     也可不使用本代理方法，直接调用 {@link AuthorizationUrlBuilder} 工具类手动构建：
     *     AuthorizationUrlBuilder.of(endpoint).clientId(...).redirectUri(...).build()
     * </pre>
     *
     * @param clientId    客户端ID
     * @param redirectUri 回调地址
     * @param scope       授权范围
     * @return 完整的授权请求URL
     */
    @Get("/oauth2/authorize")
    @GenerateOAuth2AuthorizationUrl(
            clientId = "#{clientId}",
            redirectUri = "#{redirectUri}",
            scope = "#{scope}"
    )
    String buildAuthorizationUrl(String clientId,
                                 String redirectUri,
                                 String scope);

    /**
     * 使用访问令牌获取用户信息（OIDC UserInfo端点）
     *
     * @param accessToken 访问令牌
     * @return 用户信息
     */
    @Get("/userinfo")
    @OAuth2BearerAuth(token = "#{accessToken}")
    OAuth2UserInfo userInfo(String accessToken);

    /**
     * 设备授权请求（RFC 8628 §3.1）。
     * <pre>
     *     设备端先调用本方法拿到 device_code / user_code / verification_uri，
     *     展示给用户并在另一台设备上完成授权后，再轮询 {@link #deviceCodeToken(String, String)} 换取令牌。
     *     设备授权端点路径为示例值，需随对接的服务调整。
     * </pre>
     *
     * @param clientId     客户端ID（public 客户端随表单体提交）
     * @param clientSecret 客户端密钥（confidential 客户端使用 Basic 认证，可传null）
     * @param scope        授权范围（可选）
     * @return 设备授权响应
     */
    @Post("/oauth2/device_authorization")
    @OAuth2DeviceAuthorizationRequest(
            clientId = "#{clientId}",
            clientSecret = "#{clientSecret}",
            scope = "#{scope}"
    )
    DeviceAuthorizationResult deviceAuthorization(String clientId,
                                                  String clientSecret,
                                                  String scope);

    /**
     * 设备码模式换取令牌（RFC 8628 §3.4）。
     * <pre>
     *     按 {@link #deviceAuthorization} 响应中的 interval 间隔轮询本方法，
     *     用户完成授权前可能返回 authorization_pending / slow_down 等错误，需调用方处理。
     * </pre>
     *
     * @param deviceCode 设备码（来自设备授权响应）
     * @param clientId   客户端ID（public 客户端随表单体提交）
     * @return 令牌信息
     */
    @Post("/oauth2/token")
    @OAuth2DeviceCodeGrant(
            clientId = "#{clientId}",
            deviceCode = "#{deviceCode}"
    )
    OAuth2Token deviceCodeToken(String deviceCode, String clientId);

    /**
     * 令牌交换（RFC 8693 §2.1）。
     * <pre>
     *     用一个已有令牌（subject_token）换取面向其他资源/受众或不同类型的令牌。
     *     resource、audience、actorToken、requestedTokenType 等可选参数未在此暴露，
     *     需要时可直接使用 {@link OAuth2TokenExchange} 注解声明。
     * </pre>
     *
     * @param subjectToken    被交换的主体令牌
     * @param subjectTokenType 主体令牌类型（如 urn:ietf:params:oauth:token-type:access_token）
     * @param clientId         客户端ID
     * @param clientSecret     客户端密钥
     * @return 令牌信息
     */
    @Post("/oauth2/token")
    @OAuth2TokenExchange(
            clientId = "#{clientId}",
            clientSecret = "#{clientSecret}",
            subjectToken = "#{subjectToken}",
            subjectTokenType = "#{subjectTokenType}"
    )
    OAuth2Token tokenExchange(String subjectToken,
                             String subjectTokenType,
                             String clientId,
                             String clientSecret);

    /**
     * JWT 断言授权模式换取令牌（RFC 7523 §2.1）。
     *
     * @param assertion    客户端自签的 JWT 断言
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return 令牌信息
     */
    @Post("/oauth2/token")
    @OAuth2JwtBearerGrant(
            clientId = "#{clientId}",
            clientSecret = "#{clientSecret}",
            assertion = "#{assertion}"
    )
    OAuth2Token jwtBearerToken(String assertion,
                              String clientId,
                              String clientSecret);

    /**
     * SAML2 断言授权模式换取令牌（RFC 7522 §2.1）。
     *
     * @param assertion    Base64URL 编码的 SAML2.0 断言
     * @param clientId     客户端ID
     * @param clientSecret 客户端密钥
     * @return 令牌信息
     */
    @Post("/oauth2/token")
    @OAuth2Saml2BearerGrant(
            clientId = "#{clientId}",
            clientSecret = "#{clientSecret}",
            assertion = "#{assertion}"
    )
    OAuth2Token saml2BearerToken(String assertion,
                                String clientId,
                                String clientSecret);
}
