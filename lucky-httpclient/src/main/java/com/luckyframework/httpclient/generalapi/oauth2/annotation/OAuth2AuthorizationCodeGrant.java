package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.generalapi.oauth2.resolver.AuthorizationCodeFormResolver;
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
 * 授权码模式令牌请求注解（RFC 6749 §4.1.3 + RFC 7636 §4.5.3）。
 * <pre>
 *     框架级协议语义封装，仅组合OAuth2协议要素，不绑定任何端点：
 *     1.{@link StaticParam @StaticParam}(FormParameterSetter)：从注解属性解析
 *       grant_type=authorization_code、code、redirect_uri 表单参数，
 *       以及可选的 PKCE 参数 code_verifier
 *     2.{@link OAuth2ClientAuth @OAuth2ClientAuth}：客户端认证。confidential 客户端
 *       （clientId+clientSecret）下发 Authorization: Basic；public 客户端（仅 clientId、
 *       常伴 PKCE）不下发 Basic，改将 client_id 随表单体提交
 *
 *     令牌端点由使用位置声明（如 {@code @Post("/oauth2/token")}），
 *     不同认证服务器的端点路径不同，需随对接的服务调整。
 *
 *     普通模式与 PKCE 模式共用本注解：codeVerifier 为空时不追加 PKCE 参数
 *     （向后兼容不含 PKCE 的客户端），是否 PKCE 与客户端认证方式相互独立。
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
@StaticParam(
        setter = @ObjectGenerate(FormParameterSetter.class),
        resolver = @ObjectGenerate(AuthorizationCodeFormResolver.class)
)
@OAuth2ClientAuth
public @interface OAuth2AuthorizationCodeGrant {

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
     * 授权码，支持SpEL表达式。
     * @see SpELVariableNote
     */
    String code() default "";

    /**
     * 重定向地址（需与授权请求时保持一致），支持SpEL表达式。
     * @see SpELVariableNote
     */
    String redirectUri() default "";

    /**
     * PKCE code_verifier（RFC 7636 §4.5.3），支持SpEL表达式。
     * 为空时不追加 PKCE 参数，适用于不含 PKCE 扩展的传统客户端。
     * @see SpELVariableNote
     */
    String codeVerifier() default "";
}
