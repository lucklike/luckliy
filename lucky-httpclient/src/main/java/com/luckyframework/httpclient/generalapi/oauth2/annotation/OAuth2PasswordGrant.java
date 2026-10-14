package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.generalapi.oauth2.resolver.PasswordTokenFormResolver;
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
 * 密码模式令牌请求注解（RFC 6749 §4.3）。
 * <pre>
 *     框架级协议语义封装，仅组合OAuth2协议要素，不绑定任何端点：
 *     1.{@link StaticParam @StaticParam}(FormParameterSetter)：从注解属性解析
 *       grant_type=password、username、password 表单参数
 *     2.{@link OAuth2ClientAuth @OAuth2ClientAuth}：客户端认证（Authorization: Basic ...）
 *
 *     令牌端点由使用位置声明（如 {@code @Post("/oauth2/token")}），
 *     不同认证服务器的端点路径不同，需随对接的服务调整。
 *
 *     <b>Warning: OAuth 2.1 草案已移除 Resource Owner Password Credentials Grant。</b>
 *     该模式要求客户端直接持有用户密码，存在严重安全风险：
 *     - 无法支持 MFA、SSO、Social Login 等现代认证流程
 *     - 密码在客户端明文存储或传输的泄露风险
 *     仅推荐用于"高度信任的第一方客户端"且无法使用授权码模式的历史场景，
 *     新系统应优先采用 Authorization Code + PKCE 流程。
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
        resolver = @ObjectGenerate(PasswordTokenFormResolver.class)
)
@OAuth2ClientAuth
public @interface OAuth2PasswordGrant {

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
     * 用户名，支持SpEL表达式。
     * @see SpELVariableNote
     */
    String username() default "";

    /**
     * 密码，支持SpEL表达式。
     * @see SpELVariableNote
     */
    String password() default "";
}
