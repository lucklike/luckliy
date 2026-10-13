package com.luckyframework.httpclient.generalapi.oauth2.annotation;

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
 *     2.{@link OAuth2ClientAuth @OAuth2ClientAuth}：客户端认证（Authorization: Basic ...）
 *
 *     令牌端点由使用位置声明（如 {@code @Post("/oauth2/token")}），
 *     不同认证服务器的端点路径不同，需随对接的服务调整。
 *
 *     codeVerifier 为空时不追加 PKCE 参数（向后兼容不含 PKCE 的客户端）。
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
public @interface AuthorizationCodeToken {

    @AliasFor(annotation = OAuth2ClientAuth.class, attribute = "clientId")
    String clientId() default "";

    @AliasFor(annotation = OAuth2ClientAuth.class, attribute = "clientSecret")
    String clientSecret() default "";

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
