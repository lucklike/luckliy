package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.generalapi.oauth2.AuthorizationUrlBuilder;
import com.luckyframework.httpclient.generalapi.oauth2.OAuth2PkceUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 授权请求 URL 构建配置注解（RFC 6749 §4.1.1）。
 * <pre>
 *     声明构建 /authorize 请求 URL 所需的协议参数默认值，
 *     供 {@link AuthorizationUrlBuilder} 读取使用。
 *
 *     本注解不触发 HTTP 请求，仅作为配置元数据标注在方法上，
 *     调用方通过 AuthorizationUrlBuilder 的链式 API 构建完整的授权重定向 URL。
 *
 *     若希望由框架直接返回构建好的URL（不产生真实网络调用），可在使用位置额外标注
 *     {@code @MockMeta(mock = @ObjectGenerate(AuthorizationUrlMockFactory.class))}，
 *     由该 Mock 工厂基于已解析好的请求地址补齐 query 参数后作为响应体返回。
 *
 *     state 参数的生成由 {@link OAuth2PkceUtils#generateState()} 提供，
 *     校验由调用方自行负责（推荐与用户 session 绑定比对）。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 * @see AuthorizationUrlBuilder
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizationRequest {

    /**
     * 授权端点地址（如 https://auth-server.example.com/oauth2/authorize），
     * 不同认证服务器路径不同，需随对接的服务调整。
     */
    String authorizeEndpoint() default "";

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
