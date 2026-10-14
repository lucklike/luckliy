package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.generalapi.oauth2.AuthorizationUrlBuilder;
import com.luckyframework.httpclient.generalapi.oauth2.OAuth2PkceUtils;
import com.luckyframework.httpclient.generalapi.oauth2.plugin.AuthorizationRequestProhibition;
import com.luckyframework.httpclient.generalapi.oauth2.plugin.AuthorizationUrlProxyPlugin;
import com.luckyframework.httpclient.proxy.plugin.Plugin;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 授权请求 URL 构建配置注解（RFC 6749 §4.1.1）。
 * <pre>
 *     声明构建 /authorize 请求 URL 所需的协议参数，通过
 *     {@link AuthorizationUrlProxyPlugin @AuthorizationUrlProxyPlugin} 插件实现：
 *     被标注的代理方法不会发起真实 HTTP 请求，插件沿用框架内置流程解析出授权端点地址，
 *     按本注解配置补齐 query 参数后，将构建好的完整授权 URL 作为方法返回值直接返回。
 *
 *     授权端点地址复用框架 URL 生成流程，无需在本注解中配置：
 *     1.域名部分来自类上的 @ServerAddress（或 yml 中的服务器地址）；
 *     2.路径部分来自方法上的 @Get/@Post 等 HttpRequest 注解；
 *     因此需在使用位置声明请求路径，例如：
 *     {@code
 *     @Get("/oauth2/authorize")
 *     @OAuth2AuthorizationRequest(clientId = "#{clientId}", redirectUri = "#{redirectUri}", scope = "#{scope}")
 *     String buildAuthorizationUrl(String clientId, String redirectUri, String scope);
 *     }
 *
 *     协议参数默认值亦可通过 {@link AuthorizationUrlBuilder} 的链式 API 手动构建。
 *
 *     state 参数的生成由 {@link OAuth2PkceUtils#generateState()} 提供，
 *     校验由调用方自行负责（推荐与用户 session 绑定比对）。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 * @see AuthorizationUrlBuilder
 * @see AuthorizationUrlProxyPlugin
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Plugin(pluginClass = AuthorizationUrlProxyPlugin.class, prohibition = AuthorizationRequestProhibition.class)
@Combination({Plugin.class})
public @interface OAuth2AuthorizationRequest {

    /**
     * 响应类型，授权码模式固定为 "code"。
     */
    String responseType() default "code";

    /**
     * 客户端ID。
     */
    String clientId() default "";

    /**
     * 重定向地址（需与认证服务器注册的回调地址一致）。
     */
    String redirectUri() default "";

    /**
     * 授权范围（空格分隔），可选。
     */
    String scope() default "";

    /**
     * CSRF 防护参数（RFC 6749 §10.12），可选。
     * 可由 {@link OAuth2PkceUtils#generateState()} 生成后通过方法入参传入。
     */
    String state() default "";

    /**
     * PKCE code_challenge（RFC 7636 §4.2），可选。
     * 为空时不追加 PKCE 参数。
     */
    String codeChallenge() default "";

    /**
     * PKCE code_challenge_method：S256（推荐）或 plain。
     * 为空时不追加 PKCE 参数。
     */
    String codeChallengeMethod() default "S256";
}
