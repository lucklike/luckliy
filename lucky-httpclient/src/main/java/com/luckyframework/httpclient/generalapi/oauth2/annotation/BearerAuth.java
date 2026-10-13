package com.luckyframework.httpclient.generalapi.oauth2.annotation;

import com.luckyframework.httpclient.core.meta.HttpHeaders;
import com.luckyframework.httpclient.generalapi.oauth2.resolver.BearerAuthStaticParamResolver;
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
 * Bearer Token 认证注解（RFC 6750 §2.1）。
 * <pre>
 *     通过组合框架提供的{@link StaticParam @StaticParam}元注解来实现：
 *     1.解析访问令牌配置（支持SpEL表达式）
 *     2.生成 {@code Authorization: Bearer <token>} 请求头
 *
 *     用于携带OAuth2访问令牌访问受保护资源端点。
 *     token未配置时不设置认证头。
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
        resolver = @ObjectGenerate(BearerAuthStaticParamResolver.class)
)
public @interface BearerAuth {

    /**
     * 访问令牌,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String token() default "";

    /**
     * 令牌前缀,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String prefix() default "Bearer ";

    /**
     * 请求头,支持SpEL表达式，SpEL表达式部分需要写在#{}中
     *
     * @see SpELVariableNote
     */
    String header() default HttpHeaders.AUTHORIZATION;
}
