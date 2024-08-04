package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.core.ssl.KeyStoreInfo;
import com.luckyframework.httpclient.proxy.HttpClientProxyObjectFactory;
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
    String value() default "";

    /**
     * SSL认证协议，支持SpEL表达式
     */
    @AliasFor("value")
    String protocol() default "";

    /**
     * SSL证书的密钥库，提供给服务器进行证书认证，支持SpEL表达式
     * <pre>
     *     1.可以返回一个{@link KeyStoreInfo}实例
     *     2.可以返回一个{@link String}，将使用此ID到{@link HttpClientProxyObjectFactory#getKeyStoreInfo(String)}
     * </pre>
     */
    String keyStore() default "";

    /**
     * SSL受信任的证书的密钥库，用于验证服务器提供的证书，支持SpEL表达式
     * <pre>
     *     1.可以返回一个{@link KeyStoreInfo}实例
     *     2.可以返回一个{@link String}，将使用此ID到{@link HttpClientProxyObjectFactory#getKeyStoreInfo(String)}
     * </pre>
     */
    String trustStore() default "";


    /**
     * 返回主机名验证器{@link HostnameVerifier}的SpEL表达式
     */
    String hostnameVerifier() default "";

    /**
     * 返回{@link SSLSocketFactory}的pEL表达式
     */
    String sslSocketFactory() default "";

}
