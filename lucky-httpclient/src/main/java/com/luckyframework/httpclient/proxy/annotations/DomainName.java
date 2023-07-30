package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.DomainNameGetter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
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
public @interface DomainName {

    /**
     * 请求的域名配置
     */
    String value() default "";


    /**
     * 域名获取器
     */
    Class<? extends DomainNameGetter> getter() default DomainNameGetter.class;

    String getterMsg() default "";

}
