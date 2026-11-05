package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.generalapi.oauth2.resolver.DeviceCodeTokenFormResolver;
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
 * 设备码模式令牌请求注解（RFC 8628 §3.4）。
 * <pre>
 *     框架级协议语义封装，仅组合OAuth2协议要素，不绑定任何端点：
 *     1.{@link StaticParam @StaticParam}(FormParameterSetter)：解析
 *       grant_type=urn:ietf:params:oauth:grant-type:device_code、device_code，
 *       以及 public 客户端场景下随表单体提交的 client_id
 *     2.{@link OAuth2ClientAuth @OAuth2ClientAuth}：客户端认证（confidential 客户端）
 *
 *     令牌端点由使用位置声明（如 {@code @Post("/oauth2/token")}）。
 *     设备端需按 {@link OAuth2DeviceAuthorizationRequest} 响应中的 interval 间隔轮询本方法，
 *     直到用户完成授权；期间可能收到 authorization_pending / slow_down 等错误。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 * @see OAuth2DeviceAuthorizationRequest
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StaticParam(
        setter = @ObjectGenerate(FormParameterSetter.class),
        resolver = @ObjectGenerate(DeviceCodeTokenFormResolver.class)
)
@OAuth2ClientAuth
public @interface OAuth2DeviceCodeGrant {

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
     * 设备码（来自设备授权响应），支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String deviceCode() default "";
}
