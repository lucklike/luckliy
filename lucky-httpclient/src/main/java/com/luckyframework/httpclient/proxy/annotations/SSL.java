package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ssl.SSLContextBuilder;
import com.luckyframework.httpclient.proxy.ssl.SSLContextSSLSocketFactoryBuilder;
import com.luckyframework.httpclient.proxy.ssl.TrustAllHostnameVerifierBuilder;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跳过SSL认证
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SSLMeta(
        hostnameVerifier = @ObjectGenerate(TrustAllHostnameVerifierBuilder.class),
        sslSocketFactory = @ObjectGenerate(SSLContextSSLSocketFactoryBuilder.class)
)
@Combination(SSLMeta.class)
public @interface SSL {

    /**
     * SSL认证协议，支持SpEL表达式
     */
    @AliasFor("protocol")
    String value() default "TLS";

    /**
     * SSL认证协议，支持SpEL表达式
     */
    @AliasFor("value")
    String protocol() default "TLS";

    /**
     * KeyStore构建器
     */
    ObjectGenerate sslContextBuilder() default @ObjectGenerate(SSLContextBuilder.class);

    /**
     * 通过一个SpEL表达式来获取SSLContext
     */
    String sslContextExpression() default "";

    /**
     * 已经配置在{@code HttpClientProxyObjectFactory#lazySSLContextMap}中的SSLContext对应的ID
     */
    String sslContext() default "";


}
