package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.ssl.HostnameVerifierBuilder;
import com.luckyframework.httpclient.proxy.ssl.SSLSocketFactoryBuilder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SSL认证元注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/30 02:46
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SSLMeta {

    /**
     * 用于生成{@link HostnameVerifierBuilder}参数设置器的对象生成器
     */
    ObjectGenerate hostnameVerifierBuilder();

    /**
     * 用于生成{@link SSLSocketFactoryBuilder}参数设置器的对象生成器
     */
    ObjectGenerate sslSocketFactoryBuilder();

}
