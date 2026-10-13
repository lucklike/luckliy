package com.luckyframework.httpclient.generalapi.oauth2;

import com.luckyframework.httpclient.generalapi.oauth2.annotation.AuthorizationCodeToken;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.AuthorizationRequest;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.BearerAuth;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.ClientCredentialsToken;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.PasswordToken;
import com.luckyframework.httpclient.generalapi.oauth2.annotation.RefreshTokenGrant;
import com.luckyframework.httpclient.generalapi.oauth2.resp.OAuth2Token;
import com.luckyframework.httpclient.generalapi.oauth2.resp.OAuth2UserInfo;
import com.luckyframework.httpclient.proxy.annotations.Get;
import com.luckyframework.httpclient.proxy.annotations.Post;

/**
 * OAuth2 认证客户端（RFC 6749）。
 * <pre>
 *     演示如何使用框架级OAuth2注解套件（oauth2.annotation包）声明式地对接OAuth2认证服务器：
 *     1.令牌请求方法：授权模式注解（@ClientCredentialsToken等）+ 使用位置声明的令牌端点
 *     2.资源访问方法：@BearerAuth + 使用位置声明的资源端点
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
 *
 *     本客户端提供令牌获取与刷新请求能力（含 @RefreshTokenGrant），
 *     但不内置自动缓存与过期刷新机制；令牌的生命周期管理由调用方自行负责。
 *
 *     授权码模式中 §4.1.1 构建授权URL 的步骤由 {@link AuthorizationUrlBuilder} 工具类提供，
 *     本接口中 {@link #buildAuthorizationUrl(String, String, String, String)} 为声明性示例方法。
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
    @ClientCredentialsToken(clientId = "#{clientId}", clientSecret = "#{clientSecret}")
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
    @PasswordToken(
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
    @RefreshTokenGrant(
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
    @AuthorizationCodeToken(
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
     * 构建授权请求URL（RFC 6749 §4.1.1，声明性示例方法）。
     * <pre>
     *     实际调用推荐使用 {@link AuthorizationUrlBuilder} 工具类：
     *     AuthorizationUrlBuilder.of(endpoint).clientId(...).redirectUri(...).build()
     *
     *     本方法展示 @AuthorizationRequest 注解的声明方式；若希望通过代理方法直接拿到
     *     构建好的URL（不产生真实网络调用），需在使用位置额外标注
     *     {@code @MockMeta(mock = @ObjectGenerate(AuthorizationUrlMockFactory.class))}
     *     并配合 @Get 声明授权端点路径。
     * </pre>
     *
     * @param authorizeEndpoint 授权服务器端点
     * @param clientId          客户端ID
     * @param redirectUri       回调地址
     * @param scope             授权范围
     * @return 完整的授权请求URL
     */
    @AuthorizationRequest(
            authorizeEndpoint = "#{authorizeEndpoint}",
            clientId = "#{clientId}",
            redirectUri = "#{redirectUri}",
            scope = "#{scope}"
    )
    String buildAuthorizationUrl(String authorizeEndpoint,
                                 String clientId,
                                 String redirectUri,
                                 String scope);

    /**
     * 使用访问令牌获取用户信息（OIDC UserInfo端点）
     *
     * @param accessToken 访问令牌
     * @return 用户信息
     */
    @Get("/userinfo")
    @BearerAuth(token = "#{accessToken}")
    OAuth2UserInfo userInfo(String accessToken);
}
