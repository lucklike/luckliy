package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.url.DomainNameGetter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 服务器地址配置相关的注解
 *
 * @see ServerAddress
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ServerAddressMeta {

    /**
     * 空域名
     */
    String EMPTY = "";

    /**
     * 用于创建{@link DomainNameGetter}对象的生成器注解
     */
    ObjectGenerate getter();

}
