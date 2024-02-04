package com.luckyframework.httpclient.proxy.annotations;

import com.luckyframework.httpclient.proxy.url.DomainNameGetter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 域名配置注解
 *
 * @see DomainName
 *
 * @author fukang
 * @version 1.0.0
 * @date 2023/7/25 12:25
 */
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DomainNameMeta {

    String ATTRIBUTE_GETTER = "getter";
    String ATTRIBUTE_GETTER_MSG = "getterMsg";

    /**
     * 域名获取器实例Class
     */
    Class<? extends DomainNameGetter> getter();

    /**
     * 用于创建域名获取器实例的额外信息
     */
    String getterMsg() default "";

}
