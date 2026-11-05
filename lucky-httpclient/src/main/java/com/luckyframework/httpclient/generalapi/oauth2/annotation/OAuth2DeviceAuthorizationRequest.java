package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.generalapi.oauth2.resolver.DeviceAuthorizationFormResolver;
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
 * 设备授权请求注解（RFC 8628 §3.1）。
 * <pre>
 *     框架级协议语义封装，仅组合OAuth2协议要素，不绑定任何端点：
 *     1.{@link StaticParam @StaticParam}(FormParameterSetter)：解析可选的 scope 表单参数，
 *       以及 public 客户端场景下随表单体提交的 client_id
 *     2.{@link OAuth2ClientAuth @OAuth2ClientAuth}：客户端认证（Authorization: Basic ...，confidential 客户端）
 *
 *     设备授权端点由使用位置声明（如 {@code @Post("/oauth2/device_authorization")}），
 *     不同认证服务器的端点路径不同，需随对接的服务调整。
 *     方法返回值建议使用 {@link com.luckyframework.httpclient.generalapi.oauth2.resp.DeviceAuthorizationResult}。
 * </pre>
 *
 * @author fukang
 * @version 1.0.0
 * @date 2026-10-14
 * @see com.luckyframework.httpclient.generalapi.oauth2.resp.DeviceAuthorizationResult
 * @see OAuth2DeviceCodeGrant
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@StaticParam(
        setter = @ObjectGenerate(FormParameterSetter.class),
        resolver = @ObjectGenerate(DeviceAuthorizationFormResolver.class)
)
@OAuth2ClientAuth
public @interface OAuth2DeviceAuthorizationRequest {

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
     * 授权范围（空格分隔），可选，支持SpEL表达式。
     *
     * @see SpELVariableNote
     */
    String scope() default "";
}
