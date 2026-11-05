package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.generalapi.oauth2.resolver.Saml2BearerTokenFormResolver;
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
 * SAML2 断言授权模式注解（RFC 7522 §2.1）。
 * <pre>
 *     框架级协议语义封装，仅组合OAuth2协议要素，不绑定任何端点：
 *     1.{@link StaticParam @StaticParam}(FormParameterSetter)：解析
 *       grant_type=urn:ietf:params:oauth:grant-type:saml2-bearer 与 assertion 参数
 *     2.{@link OAuth2ClientAuth @OAuth2ClientAuth}：客户端认证（Authorization: Basic ...）
 *
 *     令牌端点由使用位置声明（如 {@code @Post("/oauth2/token")}）。
 *     assertion 为 Base64URL 编码的 SAML2.0 断言，用于换取访问令牌。
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
        resolver = @ObjectGenerate(Saml2BearerTokenFormResolver.class)
)
@OAuth2ClientAuth
public @interface OAuth2Saml2BearerGrant {

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
     * Base64URL 编码的 SAML2.0 断言（必填），支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String assertion() default "";
}
