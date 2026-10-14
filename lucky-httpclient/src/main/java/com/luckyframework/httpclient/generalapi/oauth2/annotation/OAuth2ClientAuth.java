package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.generalapi.oauth2.resolver.OAuth2ClientAuthStaticParamResolver;
import com.luckyframework.httpclient.proxy.SpELVariableNote;
import com.luckyframework.httpclient.proxy.annotations.ObjectGenerate;
import com.luckyframework.httpclient.proxy.annotations.StaticParam;
import com.luckyframework.httpclient.proxy.setter.HeaderParameterSetter;
import com.luckyframework.reflect.Combination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * OAuth2 客户端认证注解（RFC 6749 §2.3.1）。
 * <pre>
 *     通过组合框架提供的{@link StaticParam @StaticParam}元注解来实现：
 *     1.解析clientId/clientSecret配置（支持SpEL表达式）
 *     2.生成 {@code Authorization: Basic base64(clientId:clientSecret)} 请求头
 *
 *     适用于 confidential 客户端的令牌端点认证以及需要客户端认证的其他端点。
 *     仅当 clientId 与 clientSecret 均有值时才下发 Authorization: Basic 头；
 *     public 客户端（只有 clientId、无 clientSecret，如 PKCE 场景）不下发 Basic 头，
 *     其 client_id 由各 grant 表单解析器放入表单体。
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
@Combination({StaticParam.class})
@StaticParam(
        setter = @ObjectGenerate(HeaderParameterSetter.class),
        resolver = @ObjectGenerate(OAuth2ClientAuthStaticParamResolver.class)
)
public @interface OAuth2ClientAuth {

    /**
     * 客户端ID,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String clientId() default "";

    /**
     * 客户端密钥,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String clientSecret() default "";

    /**
     * 请求头,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String header() default HttpHeaders.AUTHORIZATION;

    /**
     * 编码方式,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String charset() default "ISO_8859_1";
}
