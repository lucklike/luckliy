package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.proxy.annotations.StaticForm;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 客户端凭证模式令牌请求注解（RFC 6749 §4.4）。
 * <pre>
 *     框架级协议语义封装，仅组合OAuth2协议要素，不绑定任何端点：
 *     1.{@link StaticForm @StaticForm}：固定表单参数 grant_type=client_credentials
 *     2.{@link OAuth2ClientAuth @OAuth2ClientAuth}：客户端认证（Authorization: Basic ...）
 *
 *     令牌端点由使用位置声明（如 {@code @Post("/oauth2/token")}），
 *     不同认证服务器的端点路径不同，需随对接的服务调整。
 *     适用于没有用户参与的服务端对接场景。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-13
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StaticForm({"grant_type=client_credentials"})
@OAuth2ClientAuth
public @interface OAuth2ClientCredentialsGrant {

    /**
     * 客户端ID,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see OAuth2ClientAuth#clientId()
     */
    @AliasFor(annotation = OAuth2ClientAuth.class, attribute = "clientId")
    String clientId() default "";

    /**
     * 客户端密钥,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see OAuth2ClientAuth#clientSecret()
     */
    @AliasFor(annotation = OAuth2ClientAuth.class, attribute = "clientSecret")
    String clientSecret() default "";

    /**
     * 客户端认证请求头，支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see OAuth2ClientAuth#header()
     */
    @AliasFor(annotation = OAuth2ClientAuth.class, attribute = "header")
    String header() default HttpHeaders.AUTHORIZATION;

    /**
     * 客户端凭证的编码方式，支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see OAuth2ClientAuth#charset()
     */
    @AliasFor(annotation = OAuth2ClientAuth.class, attribute = "charset")
    String charset() default "ISO_8859_1";
}
