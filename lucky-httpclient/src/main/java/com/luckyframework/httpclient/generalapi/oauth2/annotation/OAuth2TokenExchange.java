package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.generalapi.oauth2.resolver.TokenExchangeFormResolver;
import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.setter.FormParameterSetter;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 令牌交换模式注解（RFC 8693 §2.1）。
 * <pre>
 *     框架级协议语义封装，仅组合OAuth2协议要素，不绑定任何端点：
 *     1.{@link StaticParam @StaticParam}(FormParameterSetter)：解析
 *       grant_type=urn:ietf:params:oauth:grant-type:token-exchange 及交换相关参数
 *       （subject_token/subject_token_type 必填，actor_token/actor_token_type、
 *       requested_token_type、scope、resource、audience 可选）
 *     2.{@link OAuth2ClientAuth @OAuth2ClientAuth}：客户端认证（Authorization: Basic ...）
 *
 *     令牌端点由使用位置声明（如 {@code @Post("/oauth2/token")}）。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StaticParam(
        setter = @ObjectGenerate(FormParameterSetter.class),
        resolver = @ObjectGenerate(TokenExchangeFormResolver.class)
)
@OAuth2ClientAuth
public @interface OAuth2TokenExchange {

    /**
     * 客户端ID，支持SpEL表达式。
     *
     * @see OAuth2ClientAuth#clientId()
     */
    @AliasFor(annotation = OAuth2ClientAuth.class, attribute = "clientId")
    String clientId() default "";

    /**
     * 客户端密钥，支持SpEL表达式。
     *
     * @see OAuth2ClientAuth#clientSecret()
     */
    @AliasFor(annotation = OAuth2ClientAuth.class, attribute = "clientSecret")
    String clientSecret() default "";

    /**
     * 客户端认证请求头，支持SpEL表达式。
     *
     * @see OAuth2ClientAuth#header()
     */
    @AliasFor(annotation = OAuth2ClientAuth.class, attribute = "header")
    String header() default HttpHeaders.AUTHORIZATION;

    /**
     * 客户端凭证的编码方式，支持SpEL表达式。
     *
     * @see OAuth2ClientAuth#charset()
     */
    @AliasFor(annotation = OAuth2ClientAuth.class, attribute = "charset")
    String charset() default "ISO_8859_1";

    /**
     * 被交换的主体令牌（必填），支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String subjectToken() default "";

    /**
     * 主体令牌类型（必填，如 urn:ietf:params:oauth:token-type:access_token），支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String subjectTokenType() default "";

    /**
     * 委托行使的参与者令牌（可选），支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String actorToken() default "";

    /**
     * 参与者令牌类型（可选），支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String actorTokenType() default "";

    /**
     * 期望换取的目标令牌类型（可选），支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String requestedTokenType() default "";

    /**
     * 期望授予的权限范围（空格分隔，可选），支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String scope() default "";

    /**
     * 目标资源服务标识（URI，可重复，可选），支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String[] resource() default {};

    /**
     * 目标受众标识（可重复，可选），支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String[] audience() default {};
}
