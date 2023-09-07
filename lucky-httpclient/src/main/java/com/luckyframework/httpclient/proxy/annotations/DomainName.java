package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.DomainNameGetter;
import com.luckyframework.httpclient.proxy.impl.SpELDomainNameGetter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 域名配置注解
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DomainName {

    /**
     * 请求的域名配置
     */
    String value() default "";

    /**
     * 域名获取器
     */
    Class<? extends DomainNameGetter> getter() default SpELDomainNameGetter.class;

    /**
     * 用于创建域名获取器的额外信息
     */
    String getterMsg() default "";

}
