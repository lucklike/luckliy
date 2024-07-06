package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ssl.TrustAll;
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
        hostnameVerifier = @ObjectGenerate(TrustAll.TrustAllHostnameVerifierBuilder.class),
        sslSocketFactory = @ObjectGenerate(TrustAll.IgnoreVerifySSLSocketFactoryBuilder.class)
)
@Combination(SSLMeta.class)
public @interface IgnoreVerifySSL {

    /**
     * SSL认证协议
     */
    @AliasFor("sslProtocol")
    String value() default "";

    /**
     * SSL认证协议
     */
    @AliasFor("value")
    String sslProtocol() default "";

}
