package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ssl.DefaultHostnameVerifierBuilder;
import com.luckyframework.httpclient.proxy.ssl.DefaultSSLSocketFactoryBuilder;
import com.luckyframework.reflect.Combination;
import org.springframework.core.annotation.AliasFor;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SSL认证注解
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
        hostnameVerifierBuilder = @ObjectGenerate(DefaultHostnameVerifierBuilder.class),
        sslSocketFactoryBuilder = @ObjectGenerate(DefaultSSLSocketFactoryBuilder.class)
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
     * 已经配置在{@code HttpClientProxyObjectFactory#lazySSLContextMap}中的SSLContext对应的ID
     */
    String sslContext() default "";

    /**
     * 返回主机名验证器{@link HostnameVerifier}的SpEL表达式
     */
    String hostnameVerifier() default "";

    /**
     * 返回{@link SSLSocketFactory}的pEL表达式
     */
    String sslSocketFactory() default "";

}
